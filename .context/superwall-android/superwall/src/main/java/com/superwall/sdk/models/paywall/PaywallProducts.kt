package com.superwall.sdk.models.paywall

import com.superwall.sdk.store.abstractions.product.StoreProduct

// Assuming StoreProduct is defined something like this:
// @Serializable
// data class StoreProduct(val productIdentifier: String)

@Deprecated(
    message =
        "When overriding paywall products, pass a dictionary to productsByName in the " +
            "PaywallOverrides object instead",
)
class PaywallProducts(
    val primary: StoreProduct? = null,
    val secondary: StoreProduct? = null,
    val tertiary: StoreProduct? = null,
) {
    val ids: List<String> =
        listOfNotNull(
            primary?.fullIdentifier,
            secondary?.fullIdentifier,
            tertiary?.fullIdentifier,
        )
}
