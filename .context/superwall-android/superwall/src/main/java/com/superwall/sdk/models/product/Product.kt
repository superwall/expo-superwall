package com.superwall.sdk.models.product

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    @SerialName("primary")
    PRIMARY,

    @SerialName("secondary")
    SECONDARY,

    @SerialName("tertiary")
    TERTIARY,

    ;

    override fun toString() = name.lowercase()
}
/*
@Serializer(forClass = Product::class)
object ProductSerializer : KSerializer<Product> {
    override fun serialize(
        encoder: Encoder,
        value: Product,
    ) {
        // Create a JSON object with custom field names for serialization
        val jsonOutput =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can be saved only by Json")
        val jsonObject =
            buildJsonObject {
                put("product", Json.encodeToJsonElement(ProductType.serializer(), value.type))
                put("productId", JsonPrimitive(value.id))
            }
        // Encode the JSON object
        jsonOutput.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Product {
        // Decode the JSON object
        val jsonInput =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonInput.decodeJsonElement().jsonObject

        // Extract fields using the expected names during deserialization
        val type = Json.decodeFromJsonElement(ProductType.serializer(), jsonObject["product"]!!)
        val id = jsonObject["product_id_android"]?.jsonPrimitive?.content ?: ""

        return Product(type, id)
    }
}
*
 */
