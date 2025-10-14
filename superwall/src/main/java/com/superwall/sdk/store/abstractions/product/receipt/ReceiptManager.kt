package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.dependencies.ExperimentalPropertiesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.coordinator.ProductsFetcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class InAppPurchase(
    var productIdentifier: String,
)

class ReceiptManager(
    private var delegate: ProductsFetcher?,
    private val billing: Billing,
    private val ioScope: IOScope = IOScope(),
//    private val receiptData: () -> ByteArray? = ReceiptLogic::getReceiptData
) : ExperimentalPropertiesFactory {
    var purchasedSubscriptionGroupIds: Set<String>? = null
    private var _purchases: MutableSet<InAppPurchase> = mutableSetOf()
    private var receiptRefreshCompletion: ((Boolean) -> Unit)? = null
    private val latestSubscriptionState: MutableStateFlow<Map<String, Any>> =
        MutableStateFlow(emptyMap())

    val purchases: Set<String>
        get() = _purchases.map { it.productIdentifier }.toSet()

    @Suppress("RedundantSuspendModifier")
    suspend fun loadPurchasedProducts(): Set<StoreProduct>? =
        billing
            .queryAllPurchases()
            .flatMap { it.products }
            .let { products ->
                _purchases.addAll(products.map { InAppPurchase(it) }.toSet())
                delegate?.products(products.toSet())
            }?.toSet()

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

    suspend fun lastSubscriptionProperties(): Map<String, Any> =
        billing
            .queryAllPurchases()
            .maxByOrNull {
                it.purchaseTime
            }?.let {
                val purchase = it
                val timeSincePurchase = System.currentTimeMillis() - purchase.purchaseTime
                val latestSubscriptionWillAutoRenew = it.isAutoRenewing
                val productIds = purchase.products
                val product =
                    billing
                        .awaitGetProducts(productIds.toSet())
                        .firstOrNull()
                if (product == null) {
                    return@let emptyMap()
                }
                val duration = product.rawStoreProduct.subscriptionPeriod?.toMillis ?: 0
                val state =
                    when {
                        purchase.purchaseState == PurchaseState.PENDING &&
                            purchase.isAutoRenewing -> LatestSubscriptionState.GRACE_PERIOD
                        purchase.purchaseState == PurchaseState.PURCHASED &&
                            timeSincePurchase > duration -> LatestSubscriptionState.EXPIRED
                        purchase.purchaseState == PurchaseState.PURCHASED &&
                            purchase.isAutoRenewing &&
                            timeSincePurchase < duration -> LatestSubscriptionState.SUBSCRIBED
                        else -> {
                            SubscriptionStatus.Unknown
                        }
                    }
                mapOf(
                    "latestSubscriptionPeriodType" to determineLatestPeriodType(purchase, product),
                    "latestSubscriptionWillAutoRenew" to latestSubscriptionWillAutoRenew,
                    "latestSubscriptionState" to state,
                )
            } ?: emptyMap()

    fun determineLatestPeriodType(
        purchase: Purchase,
        productDetails: StoreProduct,
    ): LatestPeriodType {
        val phasesWithoutTrial =
            productDetails.rawStoreProduct.selectedOffer
                ?.pricingPhases
                ?.pricingPhaseList
                ?.dropWhile { it.priceAmountMicros == 0L } ?: emptyList()

        // Heuristics
        return when {
            // If we are in a trial period, it is a trial
            productDetails.trialPeriodEndDate?.time ?: 0 > System.currentTimeMillis() -> LatestPeriodType.TRIAL

            // Promo period with discounted price - without trial, is there more than one phase where
            // the price is cheaper?
            phasesWithoutTrial.size > 1 &&
                (phasesWithoutTrial.firstOrNull()?.priceAmountMicros ?: 0) <
                (phasesWithoutTrial.lastOrNull()?.priceAmountMicros ?: 0) -> LatestPeriodType.PROMOTIONAL

            // Unknown state - we assume it is revoked
            purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE -> LatestPeriodType.REVOKED

            // If all else doesnt match, then the user is in a subscription
            else -> LatestPeriodType.SUBSCRIPTION
        }
    }

    fun hasPurchasedProduct(productId: String): Boolean = _purchases.firstOrNull { it.productIdentifier == productId } != null

    override fun experimentalProperties(): Map<String, Any> = latestSubscriptionState.value
}
