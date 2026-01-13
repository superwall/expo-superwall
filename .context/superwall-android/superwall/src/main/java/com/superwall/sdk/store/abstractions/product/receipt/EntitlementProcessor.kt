package com.superwall.sdk.store.abstractions.product.receipt

import com.superwall.sdk.models.customer.NonSubscriptionTransaction
import com.superwall.sdk.models.customer.SubscriptionTransaction
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.product.Store
import java.util.Date

/**
 * Processes transactions into enriched entitlements.
 *
 * This class transforms raw transaction data into
 * enriched Entitlement objects with computed properties like isActive, isLifetime,
 * willRenew, etc.
 */
class EntitlementProcessor {
    /**
     * Result of processing transactions into subscription and non-subscription objects.
     */
    data class ProcessedTransactions(
        val nonSubscriptions: List<NonSubscriptionTransaction>,
        val subscriptions: List<SubscriptionTransaction>,
    )

    /**
     * Processes a list of transactions into typed subscription and non-subscription objects.
     *
     * @param transactions The list of transactions to process
     * @return A pair of (nonSubscriptions, subscriptions) lists
     */
    fun processTransactions(transactions: List<EntitlementTransaction>): ProcessedTransactions {
        val nonSubscriptions =
            transactions
                .filter {
                    it.productType == EntitlementTransactionType.CONSUMABLE ||
                        it.productType == EntitlementTransactionType.NON_CONSUMABLE
                }.map { transaction ->
                    NonSubscriptionTransaction(
                        transactionId = transaction.transactionId,
                        productId = transaction.productId,
                        purchaseDate = transaction.purchaseDate,
                        isConsumable = transaction.productType == EntitlementTransactionType.CONSUMABLE,
                        isRevoked = transaction.isRevoked,
                    )
                }
        val subscriptions =
            transactions
                .filter {
                    it.productType == EntitlementTransactionType.AUTO_RENEWABLE ||
                        it.productType == EntitlementTransactionType.NON_RENEWABLE
                }.map { transaction ->
                    SubscriptionTransaction(
                        transactionId = transaction.transactionId,
                        productId = transaction.productId,
                        purchaseDate = transaction.purchaseDate,
                        willRenew = transaction.willRenew,
                        isRevoked = transaction.isRevoked,
                        isInGracePeriod = transaction.isInGracePeriod,
                        isInBillingRetryPeriod = transaction.isInBillingRetryPeriod,
                        isActive = transaction.isActive,
                        expirationDate = transaction.expirationDate,
                    )
                }
        return ProcessedTransactions(nonSubscriptions, subscriptions)
    }

    /**
     * Builds enriched entitlements from transactions grouped by entitlement ID.
     *
     * This method processes transactions and enriches the raw entitlements from the server
     * with computed properties based on the user's transaction history.
     *
     * @param transactionsByEntitlement Map of entitlement ID to list of transactions for that entitlement
     * @param rawEntitlementsByProductId Map of product ID to set of entitlements from server config
     * @param productIdsByEntitlementId Map of entitlement ID to all product IDs that can unlock it (from config)
     * @return Map of product ID to set of enriched entitlements
     */
    fun buildEntitlementsFromTransactions(
        transactionsByEntitlement: Map<String, List<EntitlementTransaction>>,
        rawEntitlementsByProductId: Map<String, Set<Entitlement>>,
        productIdsByEntitlementId: Map<String, Set<String>>,
    ): Map<String, List<Entitlement>> {
        // If no raw entitlements, return empty
        if (rawEntitlementsByProductId.isEmpty()) {
            return emptyMap()
        }

        // Track enriched entitlements by their ID
        val enrichedEntitlementsById =
            transactionsByEntitlement
                // Process each entitlement's transactions
                .map { (entitlementId, transactions) ->
                    // Find the raw entitlement config for this entitlement ID
                    val rawEntitlement =
                        rawEntitlementsByProductId.values
                            .flatten()
                            .find { it.id == entitlementId }
                            ?: return@map null
                    // Get all product IDs that can unlock this entitlement from config
                    val allProductIds = productIdsByEntitlementId[entitlementId] ?: emptySet()
                    val enriched =
                        enrichEntitlement(
                            entitlementId = entitlementId,
                            transactions = transactions,
                            rawEntitlement = rawEntitlement,
                            allProductIds = allProductIds,
                        )
                    entitlementId to enriched
                }.filterNotNull()
                .toMap()

        // Build result map: product ID -> list of entitlements
        // Use enriched entitlements where available, fall back to raw entitlements with productIds populated
        // This ensures all entitlements are available even if never purchased
        val result =
            rawEntitlementsByProductId
                .map { (productId, rawEntitlements) ->
                    productId to
                        rawEntitlements.map { rawEntitlement ->
                            enrichedEntitlementsById[rawEntitlement.id]
                                ?: rawEntitlement.copy(
                                    productIds = productIdsByEntitlementId[rawEntitlement.id] ?: emptySet(),
                                    // No transactions means not active - if there were active purchases,
                                    // Google Play would have returned them
                                    isActive = false,
                                )
                        }
                }.toMap()

        return result
    }

    /**
     * Enriches a single entitlement with transaction data.
     *
     * @param entitlementId The ID of the entitlement to enrich
     * @param transactions The list of transactions for this entitlement
     * @param rawEntitlement The raw entitlement from server config
     * @param allProductIds All product IDs that can unlock this entitlement (from config)
     */
    private fun enrichEntitlement(
        entitlementId: String,
        transactions: List<EntitlementTransaction>,
        rawEntitlement: Entitlement,
        allProductIds: Set<String>,
    ): Entitlement {
        var isActive = false
        var renewedAt: Date? = null
        var expiresAt: Date? = null
        var mostRecentRenewable: EntitlementTransaction? = null
        var latestProductId: String? = null
        var isLifetime = false

        // Use all product IDs from config that can unlock this entitlement
        // Fall back to transaction product IDs if config is empty
        val productIds = allProductIds.ifEmpty { transactions.map { it.productId }.toSet() }

        // Find the earliest purchase date for startsAt
        val startsAt = transactions.minByOrNull { it.originalPurchaseDate }?.originalPurchaseDate

        // Check for lifetime (non-consumable) purchases first
        val lifetimePurchase =
            transactions.find { transaction ->
                transaction.productType == EntitlementTransactionType.NON_CONSUMABLE &&
                    !transaction.isRevoked
            }

        if (lifetimePurchase != null) {
            isLifetime = true
            latestProductId = lifetimePurchase.productId
            isActive = true
        }

        // Process all transactions
        for (transaction in transactions) {
            // Skip revoked transactions for active/expiration calculations
            if (transaction.isRevoked) continue

            // Check for active status - trust the transaction's isActive status
            when (transaction.productType) {
                EntitlementTransactionType.NON_CONSUMABLE -> {
                    // Non-consumable without revocation is always active
                    isActive = true
                }

                EntitlementTransactionType.AUTO_RENEWABLE,
                EntitlementTransactionType.NON_RENEWABLE,
                -> {
                    // Trust the transaction's isActive status from Google Play
                    if (transaction.isActive) {
                        isActive = true
                    }
                }

                EntitlementTransactionType.CONSUMABLE -> {
                    // Consumables don't grant active status by themselves
                }
            }

            // Track most recent renewable transaction (for non-lifetime)
            if (!isLifetime &&
                transaction.productType in
                listOf(
                    EntitlementTransactionType.AUTO_RENEWABLE,
                    EntitlementTransactionType.NON_RENEWABLE,
                )
            ) {
                if (mostRecentRenewable == null ||
                    mostRecentRenewable.purchaseDate.before(transaction.purchaseDate)
                ) {
                    mostRecentRenewable = transaction
                }
            }

            // Track renewal date - set when this is a renewal (purchase date differs from original)
            // Also use renewedAt from transaction if available
            if (!transaction.isRevoked &&
                transaction.productType in
                listOf(
                    EntitlementTransactionType.AUTO_RENEWABLE,
                    EntitlementTransactionType.NON_RENEWABLE,
                )
            ) {
                val transactionRenewedAt =
                    transaction.renewedAt ?: run {
                        // If purchaseDate != originalPurchaseDate, this is a renewal
                        if (transaction.purchaseDate != transaction.originalPurchaseDate) {
                            transaction.purchaseDate
                        } else {
                            null
                        }
                    }
                if (transactionRenewedAt != null) {
                    if (renewedAt == null || renewedAt.before(transactionRenewedAt)) {
                        renewedAt = transactionRenewedAt
                    }
                }
            }

            // Track latest expiration for non-lifetime subscriptions
            if (!isLifetime &&
                transaction.productType in
                listOf(
                    EntitlementTransactionType.AUTO_RENEWABLE,
                    EntitlementTransactionType.NON_RENEWABLE,
                )
            ) {
                val expiration = transaction.expirationDate
                if (expiration != null) {
                    if (expiresAt == null || expiration.after(expiresAt)) {
                        expiresAt = expiration
                    }
                }
            }
        }

        // Determine latest product ID if not already set by lifetime
        if (latestProductId == null) {
            latestProductId = mostRecentRenewable?.productId
        }

        // Determine willRenew and state from most recent renewable
        val willRenew =
            if (!isLifetime) {
                mostRecentRenewable?.willRenew ?: false
            } else {
                false
            }

        val state =
            if (!isLifetime && mostRecentRenewable != null) {
                when {
                    mostRecentRenewable.isRevoked -> LatestSubscriptionState.REVOKED
                    mostRecentRenewable.isInGracePeriod -> LatestSubscriptionState.GRACE_PERIOD
                    mostRecentRenewable.isInBillingRetryPeriod -> LatestSubscriptionState.BILLING_RETRY
                    !isActive -> LatestSubscriptionState.EXPIRED
                    else -> LatestSubscriptionState.SUBSCRIBED
                }
            } else {
                null
            }

        return Entitlement(
            id = entitlementId,
            type = rawEntitlement.type,
            isActive = isActive,
            productIds = productIds,
            latestProductId = latestProductId,
            startsAt = startsAt,
            renewedAt = renewedAt,
            expiresAt = if (isLifetime) null else expiresAt,
            isLifetime = isLifetime,
            willRenew = willRenew,
            state = state,
            offerType = null, // Can be enriched later with live subscription data
            store = rawEntitlement.store ?: Store.PLAY_STORE, // Default to Play Store for device entitlements
        )
    }
}
