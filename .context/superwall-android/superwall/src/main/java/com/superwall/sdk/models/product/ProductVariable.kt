package com.superwall.sdk.models.product

import com.superwall.sdk.models.serialization.AnySerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import java.util.Base64.Decoder
import java.util.Base64.Encoder

@Serializable(with = ProductVariableSerializer::class)
data class ProductVariable(
    val name: String,
    val attributes: Map<String, Any>,
)

@Serializer(forClass = ProductVariable::class)
object ProductVariableSerializer : KSerializer<ProductVariable> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ProductVariable")

    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: ProductVariable,
    ) {
        // Encoder for JSON content
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("This serializer can only be used with JSON")

        // Build the JSON object with attributes
        val attributesJson =
            buildJsonObject {
                value.attributes.forEach { (key, attributeValue) ->
                    val jsonElement = Json.encodeToJsonElement(AnySerializer.serializerFor(attributeValue), attributeValue)
                    put(key, jsonElement)
                }
            }

        // Wrap the attributes JSON object within another object using the product name as the key
        val productJson =
            buildJsonObject {
                put(value.name, attributesJson)
            }

        // Encode the final JSON object
        jsonEncoder.encodeJsonElement(productJson)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): ProductVariable =
        throw UnsupportedOperationException("Deserialization is not supported")
}
