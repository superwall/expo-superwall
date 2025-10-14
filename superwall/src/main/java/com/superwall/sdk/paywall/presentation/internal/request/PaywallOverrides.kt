package com.superwall.sdk.paywall.presentation.internal.request

import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.models.paywall.PaywallProducts
import com.superwall.sdk.store.abstractions.product.StoreProduct

// File.kt

data class PaywallOverrides(
    val productsByName: Map<String, StoreProduct> = emptyMap(),
    val ignoreSubscriptionStatus: Boolean = false,
    val presentationStyle: PaywallPresentationStyle = PaywallPresentationStyle.None,
) {
    @Deprecated("This variable has been deprecated.", ReplaceWith("productsByName"))
    val products: PaywallProducts? = mapToPaywallProducts(productsByName)

    /**
     * Converts the productsByName map to a map of ProductOverride objects.
     * This provides a consistent interface for handling both product objects and product IDs.
     */
    val productOverridesByName: Map<String, ProductOverride>
        get() = productsByName.mapValues { ProductOverride.ByProduct(it.value) }

    // Secondary constructors
    @Deprecated(
        "This constructor has been deprecated.",
        ReplaceWith("PaywallOverrides(productsByName)"),
    )
    constructor(products: PaywallProducts?) : this(mapFromPaywallProducts(products))

    @Deprecated(
        "This constructor has been deprecated.",
        ReplaceWith("PaywallOverrides(productsByName, ignoreSubscriptionStatus)"),
    )
    constructor(products: PaywallProducts?, ignoreSubscriptionStatus: Boolean) : this(
        mapFromPaywallProducts(products),
        ignoreSubscriptionStatus,
    )

    companion object {
        private fun mapFromPaywallProducts(products: PaywallProducts?): Map<String, StoreProduct> {
            val mutableProductsMap = mutableMapOf<String, StoreProduct>()

            products?.primary?.let { mutableProductsMap["primary"] = it }
            products?.secondary?.let { mutableProductsMap["secondary"] = it }
            products?.tertiary?.let { mutableProductsMap["tertiary"] = it }

            return mutableProductsMap
        }

        private fun mapToPaywallProducts(products: Map<String, StoreProduct>): PaywallProducts? {
            val primaryProduct: StoreProduct? = products["primary"]
            val secondaryProduct: StoreProduct? = products["secondary"]
            val tertiaryProduct: StoreProduct? = products["tertiary"]

            val paywallProducts: PaywallProducts? =
                if (primaryProduct != null || secondaryProduct != null || tertiaryProduct != null) {
                    PaywallProducts(
                        primary = primaryProduct,
                        secondary = secondaryProduct,
                        tertiary = tertiaryProduct,
                    )
                } else {
                    null
                }

            return paywallProducts
        }
    }
}
