package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.Purchase
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.customer.CustomerInfoManager
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.customer.NonSubscriptionTransaction
import com.superwall.sdk.models.customer.SubscriptionTransaction
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.storage.LatestDeviceCustomerInfo
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StoredTransactionHistory
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.coordinator.ProductsFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class InAppPurchase(
    var productIdentifier: String,
    var fullIdentifier: String,
)

data class TransactionReceipt(
    val originalTransactionId: String,
    val purchaseToken: String,
)

enum class LatestSubscriptionOfferType {
    TRIAL,
    CODE,
    PROMOTIONAL,
    WINBACK,
}

@Suppress("EXPOSED_PARAMETER_TYPE")
class ReceiptManager(
    private var delegate: ProductsFetcher?,
    private val billing: Billing,
    private val ioScope: IOScope = IOScope(),
    private val storage: Storage,
    internal val customerInfoManager: () -> CustomerInfoManager,
    private val entitlementProcessor: EntitlementProcessor = EntitlementProcessor(),
    private val subscriptionStatusProvider: SubscriptionStatusProvider = PlayBillingSubscriptionStatusProvider(billing),
) {
    var latestSubscriptionWillAutoRenew: Boolean? = null
    var latestSubscriptionPeriodType: LatestPeriodType? = null
    var transactionReceipts: MutableList<TransactionReceipt> = mutableListOf()

    data class PurchaseSnapshot(
        val purchases: Set<InAppPurchase>,
        val entitlementsByProductId: Map<String, List<Entitlement>>,
        val nonSubscriptions: List<NonSubscriptionTransaction>,
        val subscriptions: List<SubscriptionTransaction>,
    )

    private var _purchases: MutableSet<InAppPurchase> = mutableSetOf()
    private val latestSubscriptionState: MutableStateFlow<LatestSubscriptionState> =
        MutableStateFlow(LatestSubscriptionState.UNKNOWN)

    val purchases: Set<String>
        get() = _purchases.map { it.productIdentifier }.toSet()

    /**
     * Loads purchased products from device receipts and builds enriched entitlements.
     *
     * This method:
     * 1. Queries all purchases from Google Play
     * 2. Builds enriched entitlements using server entitlement definitions
     * 3. Creates device CustomerInfo from the receipts
     * 4. Stores the device CustomerInfo and triggers merge with web info
     *
     * @param serverEntitlementsByProductId Map of product ID to entitlements from config
     */
    suspend fun loadPurchasedProducts(serverEntitlementsByProductId: Map<String, Set<Entitlement>>) {
        // Load purchases and build enriched entitlements
        loadPurchases(serverEntitlementsByProductId)
    }

    suspend fun loadPurchases(serverEntitlementsByProductId: Map<String, Set<Entitlement>>): PurchaseSnapshot {
        val purchases: MutableSet<InAppPurchase> = mutableSetOf()
        val originalTransactionIds: MutableSet<String> = mutableSetOf()
        transactionReceipts.clear()

        // Note: Need to access Superwall instance through proper dependency injection
        val enableExperimentalDeviceVariables = false // TODO: Get from proper source

        // Build product ID to entitlement ID mapping
        val productIdsByEntitlementId: MutableMap<String, MutableSet<String>> = mutableMapOf()
        serverEntitlementsByProductId.forEach { (productId, entitlements) ->
            entitlements.forEach { entitlement ->
                productIdsByEntitlementId.getOrPut(entitlement.id) { mutableSetOf() }.add(productId)
            }
        }

        // Load existing transaction history to preserve data from expired purchases
        val existingHistory = storage.read(StoredTransactionHistory) ?: UserTransactionHistory()

        // 1️⃣ FIRST PASS: Collect purchases, create transaction adapters, and collect receipts
        val allPurchases = billing.queryAllPurchases()
        val transactionsByEntitlement: MutableMap<String, MutableList<EntitlementTransaction>> = mutableMapOf()
        val currentTransactions: MutableList<EntitlementTransaction> = mutableListOf()

        // Build mapping from raw product ID to full product ID using server entitlements
        // This allows us to fetch products with proper base plan/offer info
        val rawProductIds = allPurchases.flatMap { it.products }.toSet()
        val rawToFullProductId = mutableMapOf<String, String>()
        rawProductIds.forEach { rawId ->
            // Find the full product ID from server config that matches this raw ID
            // Check exact match first, then fall back to prefix match
            val fullId =
                serverEntitlementsByProductId.keys.firstOrNull { it == rawId }
                    ?: serverEntitlementsByProductId.keys.firstOrNull { it.startsWith("$rawId:") }
            rawToFullProductId[rawId] = fullId ?: rawId
        }

        // Fetch products using full product IDs so they have proper base plan/offer info
        val fullProductIds = rawToFullProductId.values.toSet()
        val productsById = billing.awaitGetProducts(fullProductIds).associateBy { it.productIdentifier }

        allPurchases.forEach { purchase ->
            // Create adapters for each product in the purchase
            val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)

            adapters.forEach { adapter ->
                currentTransactions.add(adapter)
                purchases.add(
                    InAppPurchase(
                        productIdentifier = productsById[adapter.productId]?.productIdentifier ?: adapter.productId,
                        fullIdentifier = adapter.productId,
                    ),
                )

                // Map to entitlements - adapter.productId is now the full product ID
                val serverEntitlements = serverEntitlementsByProductId[adapter.productId]
                serverEntitlements?.forEach { entitlement ->
                    transactionsByEntitlement
                        .getOrPut(entitlement.id) { mutableListOf() }
                        .add(adapter)
                }
            }

            // Collect receipt for original transaction
            val txnId = purchase.orderId ?: purchase.purchaseToken
            if (!originalTransactionIds.contains(txnId)) {
                transactionReceipts.add(
                    TransactionReceipt(
                        originalTransactionId = txnId,
                        purchaseToken = purchase.purchaseToken,
                    ),
                )
                originalTransactionIds.add(txnId)
            }
        }

        // 2️⃣ Merge current transactions with history and persist
        // This preserves data from expired purchases that Google Play no longer returns
        val updatedHistory = existingHistory.mergeWith(currentTransactions)
        storage.write(StoredTransactionHistory, updatedHistory)

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.receipts,
            message =
                "Transaction history: ${currentTransactions.size} current, " +
                    "${updatedHistory.transactions.size} total stored, " +
                    "${updatedHistory.activeTransactions().size} active",
        )

        // 3️⃣ Use all historical transactions for processing (includes inactive ones for history)
        val allTransactions: List<EntitlementTransaction> = updatedHistory.allTransactions()

        // 4️⃣ Use EntitlementProcessor to process transactions
        val (nonSubscriptions, subscriptions) = entitlementProcessor.processTransactions(allTransactions)

        // 5️⃣ Build enriched entitlements using historical transactions mapped to entitlements
        // Re-map historical transactions to entitlements
        val historicalTransactionsByEntitlement: MutableMap<String, MutableList<EntitlementTransaction>> = mutableMapOf()
        allTransactions.forEach { transaction ->
            val serverEntitlements = serverEntitlementsByProductId[transaction.productId]
            serverEntitlements?.forEach { entitlement ->
                historicalTransactionsByEntitlement
                    .getOrPut(entitlement.id) { mutableListOf() }
                    .add(transaction)
            }
        }

        val entitlementsByProductId =
            entitlementProcessor.buildEntitlementsFromTransactions(
                transactionsByEntitlement = historicalTransactionsByEntitlement,
                rawEntitlementsByProductId = serverEntitlementsByProductId,
                productIdsByEntitlementId = productIdsByEntitlementId,
            )

        // 6️⃣ Update experimental device variables if enabled
        if (enableExperimentalDeviceVariables) {
            updateExperimentalDeviceVariables(allPurchases, productsById)
        }

        _purchases = purchases

        // Build device CustomerInfo from local receipts (including historical)
        val deviceCustomerInfo =
            CustomerInfo(
                subscriptions = subscriptions.reversed(),
                nonSubscriptions = nonSubscriptions.reversed(),
                userId = "", // Will be filled by merge with web info
                entitlements =
                    entitlementsByProductId.values
                        .flatten()
                        .distinctBy { it.id }
                        .toList(),
                isPlaceholder = subscriptions.isEmpty() && nonSubscriptions.isEmpty(),
            )

        // Store device CustomerInfo
        storage.write(LatestDeviceCustomerInfo, deviceCustomerInfo)

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.receipts,
            message =
                "Built device CustomerInfo: ${deviceCustomerInfo.subscriptions.size} subs, " +
                    "${deviceCustomerInfo.nonSubscriptions.size} non-subs, " +
                    "${deviceCustomerInfo.entitlements.size} entitlements",
        )

        // Trigger merge
        customerInfoManager().updateMergedCustomerInfo()

        return PurchaseSnapshot(
            purchases = purchases,
            entitlementsByProductId = entitlementsByProductId,
            nonSubscriptions = nonSubscriptions.reversed(),
            subscriptions = subscriptions.reversed(),
        )
    }

    /**
     * Updates experimental device variables from the most recent purchase.
     */
    private suspend fun updateExperimentalDeviceVariables(
        allPurchases: List<Purchase>,
        productsById: Map<String, StoreProduct>,
    ) {
        val mostRecentPurchase = allPurchases.maxByOrNull { it.purchaseTime } ?: return
        val product = productsById[mostRecentPurchase.products.firstOrNull()] ?: return

        latestSubscriptionWillAutoRenew = subscriptionStatusProvider.getWillAutoRenew(mostRecentPurchase)
        latestSubscriptionState.value = subscriptionStatusProvider.getSubscriptionState(mostRecentPurchase, product)
        latestSubscriptionPeriodType = subscriptionStatusProvider.getOfferType(mostRecentPurchase, product)
    }

    fun refreshReceipt() {
        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.receipts,
            message = "Refreshing receipts",
        )
        ioScope.launch {
            latestSubscriptionState.value = lastSubscriptionProperties()
        }
    }

    init {
        refreshReceipt()
    }

    suspend fun lastSubscriptionProperties(): LatestSubscriptionState {
        val purchase =
            billing.queryAllPurchases().maxByOrNull { it.purchaseTime }
                ?: return LatestSubscriptionState.UNKNOWN

        val product =
            billing.awaitGetProducts(purchase.products.toSet()).firstOrNull()
                ?: return LatestSubscriptionState.UNKNOWN

        return subscriptionStatusProvider.getSubscriptionState(purchase, product)
    }

    fun hasPurchasedProduct(productId: String): Boolean = _purchases.firstOrNull { it.productIdentifier == productId } != null

    suspend fun isEligibleForIntroOffer(storeProduct: StoreProduct): Boolean {
        // Check if user has never purchased this subscription before
        val hasExistingPurchase =
            _purchases.any { it.productIdentifier == storeProduct.productIdentifier }
        return !hasExistingPurchase
    }

    /**
     * Returns experimental device variables for backwards compatibility.
     * These are used in device variables for paywall templating.
     */
    fun experimentalProperties(): Map<String, Any> =
        buildMap {
            determinePeriodType()?.let { put("latestSubscriptionPeriodType", it) }
            latestSubscriptionWillAutoRenew?.let { put("latestSubscriptionWillAutoRenew", it) }
            put("latestSubscriptionState", latestSubscriptionState.value)
        }

    /**
     * Determines the period type string from the latest subscription period type.
     */
    private fun determinePeriodType(): String? =
        when (latestSubscriptionPeriodType) {
            LatestPeriodType.TRIAL -> "trial"
            LatestPeriodType.SUBSCRIPTION -> "normal"
            LatestPeriodType.PROMOTIONAL -> "intro"
            LatestPeriodType.REVOKED -> "revoked"
            null -> null
        }
}
