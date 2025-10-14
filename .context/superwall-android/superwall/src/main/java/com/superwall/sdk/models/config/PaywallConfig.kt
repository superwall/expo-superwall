package com.superwall.sdk.models.config

import kotlinx.serialization.Serializable

@Serializable
data class PaywallConfig(
    var identifier: String,
    var products: List<ProductConfig>,
) {
    @Serializable
    data class ProductConfig(
        var identifier: String,
    )
}
