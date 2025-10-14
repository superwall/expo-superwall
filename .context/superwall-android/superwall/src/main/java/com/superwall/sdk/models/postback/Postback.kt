package com.superwall.sdk.models.postback

import com.superwall.sdk.models.serialization.AnySerializer
import kotlinx.serialization.Serializable

@Serializable
data class Postback(
    val products: List<PostbackProduct>,
)

@Serializable
data class PostbackProduct(
    val identifier: String,
    // TODO: Figure out productVariables type
    val productVariables: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any,
    >, // Assuming JSON as Map<String, Any>
    val product: SWProduct,
) {
    constructor(product: StoreProduct) : this(
        identifier = product.productIdentifier,
        productVariables = product.swProductTemplateVariablesJson,
        product = product.swProduct,
    )
}

// Assuming StoreProduct is defined something like this:
data class StoreProduct(
    val productIdentifier: String,
    val swProductTemplateVariablesJson: Map<String, Any>,
    val swProduct: SWProduct,
)

// Assuming SWProduct is a simple data class with some properties:
@Serializable
data class SWProduct(
    val id: String,
)
