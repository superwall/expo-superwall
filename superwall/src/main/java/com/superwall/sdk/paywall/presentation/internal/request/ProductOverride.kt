package com.superwall.sdk.paywall.presentation.internal.request

import com.superwall.sdk.store.abstractions.product.StoreProduct

/**
 * Represents different ways to override products in paywalls.
 */
sealed class ProductOverride {
    /**
     * Override using a product identifier string.
     */
    data class ById(
        val productId: String,
    ) : ProductOverride()

    /**
     * Override using a StoreProduct object.
     */
    data class ByProduct(
        val product: StoreProduct,
    ) : ProductOverride()

    companion object {
        /**
         * Creates a ProductOverride.ById instance.
         */
        fun byId(productId: String): ProductOverride = ById(productId)

        /**
         * Creates a ProductOverride.ByProduct instance.
         */
        fun byProduct(product: StoreProduct): ProductOverride = ByProduct(product)
    }
}
