package com.superwall.sdk.paywall.view.webview.templating.models

import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.models.serialization.AnySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Variables(
    val user: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    >,
    val device: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    >,
    val params: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    >,
    var products: List<ProductVariable> = emptyList(),
    var primary: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    > = emptyMap(),
    var secondary: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    > = emptyMap(),
    var tertiary: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
    > = emptyMap(),
) {
    constructor(
        products: List<ProductVariable>?,
        params: Map<String, Any?>?,
        userAttributes: Map<String, Any?>,
        templateDeviceDictionary: Map<String, Any?>?,
    ) : this(
        user = userAttributes,
        device = templateDeviceDictionary ?: emptyMap(),
        params = params ?: emptyMap(),
    ) {
        products?.forEach { productVariable ->
            when (productVariable.name) {
                "primary" -> primary = productVariable.attributes
                "secondary" -> secondary = productVariable.attributes
                "tertiary" -> tertiary = productVariable.attributes
            }
        }
        products?.let {
            this.products = it
        }
    }

    fun templated(): JsonVariables =
        JsonVariables(
            eventName = "template_variables",
            variables = this,
        )
}

@Serializable
data class JsonVariables(
    @SerialName("event_name")
    val eventName: String,
    val variables: Variables,
)
