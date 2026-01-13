package com.superwall.sdk.paywall.view.webview.templating.models

import com.superwall.sdk.models.product.ProductItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductTemplate(
    @SerialName("event_name")
    val eventName: String,
    @SerialName("products")
    val products: List<ProductItem>,
)
