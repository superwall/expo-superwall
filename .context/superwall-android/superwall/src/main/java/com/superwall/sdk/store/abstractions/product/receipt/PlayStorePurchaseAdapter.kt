package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.superwall.sdk.store.abstractions.product.StoreProduct
import java.util.Date

/**
 * Adapts a Google Play Purchase to the EntitlementTransaction interface.
 *
 */
class PlayStorePurchaseAdapter(
    private val purchase: Purchase,
    private val product: StoreProduct?,
    override val productId: String,
) : EntitlementTransaction {
    override val transactionId: String
        get() = purchase.orderId ?: purchase.purchaseToken

    override val purchaseDate: Date
        get() = Date(purchase.purchaseTime)

    override val originalPurchaseDate: Date
        get() = Date(purchase.purchaseTime) // Google Play doesn't distinguish original vs renewal

    override val expirationDate: Date?
        get() = calculateExpirationDate()

    override val isRevoked: Boolean
        get() =
            purchase.purchaseState == PurchaseState.PENDING ||
                purchase.purchaseState == PurchaseState.UNSPECIFIED_STATE

    override val productType: EntitlementTransactionType
        get() =
            when (product?.rawStoreProduct?.underlyingProductDetails?.productType) {
                ProductType.INAPP -> EntitlementTransactionType.NON_CONSUMABLE
                ProductType.SUBS ->
                    if (purchase.isAutoRenewing) {
                        EntitlementTransactionType.AUTO_RENEWABLE
                    } else {
                        EntitlementTransactionType.NON_RENEWABLE
                    }
                else -> EntitlementTransactionType.CONSUMABLE
            }

    override val willRenew: Boolean
        get() = purchase.isAutoRenewing

    override val renewedAt: Date?
        get() = null // Google Play doesn't provide this directly

    override val isInGracePeriod: Boolean
        get() = purchase.purchaseState == PurchaseState.PENDING && purchase.isAutoRenewing

    override val isInBillingRetryPeriod: Boolean
        get() = false // Google Play doesn't expose this directly

    override val isActive: Boolean
        get() = calculateIsActive()

    private fun calculateExpirationDate(): Date? {
        if (product == null) return null

        return when (product.rawStoreProduct.underlyingProductDetails.productType) {
            ProductType.SUBS -> {
                val subscriptionPeriod = product.rawStoreProduct.subscriptionPeriod
                if (subscriptionPeriod != null) {
                    val periodMillis = subscriptionPeriod.toMillis
                    Date(purchase.purchaseTime + periodMillis)
                } else {
                    null
                }
            }
            ProductType.INAPP -> null // Non-consumable don't expire
            else -> null
        }
    }

    private fun calculateIsActive(): Boolean {
        val now = System.currentTimeMillis()

        return when (purchase.purchaseState) {
            PurchaseState.PENDING -> false
            PurchaseState.PURCHASED -> {
                val expiration = expirationDate
                expiration == null || expiration.time > now
            }
            PurchaseState.UNSPECIFIED_STATE -> false
            else -> false
        }
    }

    companion object {
        /**
         * Creates adapters for all products in a purchase.
         *
         * @param purchase The Google Play purchase
         * @param productsById Map of products keyed by raw product ID (productIdentifier)
         * @return List of adapters, one for each product in the purchase
         */
        fun fromPurchase(
            purchase: Purchase,
            productsById: Map<String, StoreProduct>,
        ): List<PlayStorePurchaseAdapter> =
            purchase.products.map { productId ->
                val product = productsById[productId]
                PlayStorePurchaseAdapter(
                    purchase = purchase,
                    product = product,
                    // Use fullIdentifier for entitlement matching when product is found,
                    // otherwise fall back to raw productId
                    productId = product?.fullIdentifier ?: productId,
                )
            }
    }
}
