package com.superwall.sdk.paywall.view_controller.web_view.templating.models

import com.superwall.sdk.models.product.ProductItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductTemplate(
    @SerialName("event_name")
    val eventName: String,
    val products: List<ProductItem>,
)
