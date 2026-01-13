package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.store.abstractions.product.StoreProduct

/**
 * Provides subscription status information from the billing service.
 *
 * This interface abstracts the subscription status querying logic,
 * making it easier to test and swap implementations.
 */
interface SubscriptionStatusProvider {
    /**
     * Gets the subscription state for a purchase.
     */
    suspend fun getSubscriptionState(
        purchase: Purchase,
        product: StoreProduct?,
    ): LatestSubscriptionState

    /**
     * Gets whether the subscription will auto-renew.
     */
    fun getWillAutoRenew(purchase: Purchase): Boolean

    /**
     * Gets the offer type (trial, promotional, etc.) for a purchase.
     */
    fun getOfferType(
        purchase: Purchase,
        product: StoreProduct?,
    ): LatestPeriodType?

    /**
     * Determines if a purchase is currently active.
     */
    suspend fun isActive(
        purchase: Purchase,
        product: StoreProduct?,
    ): Boolean
}

/**
 * Default implementation using Google Play Billing.
 */
class PlayBillingSubscriptionStatusProvider(
    private val billing: Billing,
) : SubscriptionStatusProvider {
    override suspend fun getSubscriptionState(
        purchase: Purchase,
        product: StoreProduct?,
    ): LatestSubscriptionState {
        val now = System.currentTimeMillis()
        val timeSincePurchase = now - purchase.purchaseTime
        val duration = product?.rawStoreProduct?.subscriptionPeriod?.toMillis ?: 0

        return when {
            // Revoked/refunded
            purchase.purchaseState == PurchaseState.UNSPECIFIED_STATE ->
                LatestSubscriptionState.REVOKED

            // Grace period - pending but still auto-renewing
            purchase.purchaseState == PurchaseState.PENDING &&
                purchase.isAutoRenewing ->
                LatestSubscriptionState.GRACE_PERIOD

            // Expired - purchased but past duration
            purchase.purchaseState == PurchaseState.PURCHASED &&
                duration > 0 &&
                timeSincePurchase > duration ->
                LatestSubscriptionState.EXPIRED

            // Active subscription
            purchase.purchaseState == PurchaseState.PURCHASED &&
                purchase.isAutoRenewing &&
                (duration == 0L || timeSincePurchase < duration) ->
                LatestSubscriptionState.SUBSCRIBED

            // Expired (non-renewing or past duration)
            purchase.purchaseState == PurchaseState.PURCHASED ->
                LatestSubscriptionState.EXPIRED

            else -> LatestSubscriptionState.UNKNOWN
        }
    }

    override fun getWillAutoRenew(purchase: Purchase): Boolean = purchase.isAutoRenewing

    override fun getOfferType(
        purchase: Purchase,
        product: StoreProduct?,
    ): LatestPeriodType? {
        if (product == null) return null

        val phasesWithoutTrial =
            product.rawStoreProduct.selectedOffer
                ?.pricingPhases
                ?.pricingPhaseList
                ?.dropWhile { it.priceAmountMicros == 0L } ?: emptyList()

        return when {
            // If we are in a trial period
            (product.trialPeriodEndDate?.time ?: 0) > System.currentTimeMillis() ->
                LatestPeriodType.TRIAL

            // Promo period with discounted price
            phasesWithoutTrial.size > 1 &&
                (phasesWithoutTrial.firstOrNull()?.priceAmountMicros ?: 0) <
                (phasesWithoutTrial.lastOrNull()?.priceAmountMicros ?: 0) ->
                LatestPeriodType.PROMOTIONAL

            // Unknown state - we assume it is revoked
            purchase.purchaseState == PurchaseState.UNSPECIFIED_STATE ->
                LatestPeriodType.REVOKED

            // Regular subscription
            else -> LatestPeriodType.SUBSCRIPTION
        }
    }

    override suspend fun isActive(
        purchase: Purchase,
        product: StoreProduct?,
    ): Boolean {
        val now = System.currentTimeMillis()

        return when (purchase.purchaseState) {
            PurchaseState.PENDING -> false
            PurchaseState.PURCHASED -> {
                if (product == null) return false

                when (product.rawStoreProduct.underlyingProductDetails.productType) {
                    com.android.billingclient.api.BillingClient.ProductType.INAPP -> {
                        // Non-consumable is always active if purchased
                        true
                    }
                    com.android.billingclient.api.BillingClient.ProductType.SUBS -> {
                        val subscriptionPeriod = product.rawStoreProduct.subscriptionPeriod
                        if (subscriptionPeriod != null) {
                            val periodMillis = subscriptionPeriod.toMillis
                            val expirationTime = purchase.purchaseTime + periodMillis
                            expirationTime > now
                        } else {
                            // If we can't determine expiration, assume active
                            true
                        }
                    }
                    else -> false
                }
            }
            PurchaseState.UNSPECIFIED_STATE -> false
            else -> false
        }
    }
}
