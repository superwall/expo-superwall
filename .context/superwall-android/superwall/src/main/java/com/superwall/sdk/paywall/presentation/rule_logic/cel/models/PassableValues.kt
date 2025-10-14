package com.superwall.sdk.paywall.presentation.rule_logic.cel.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ExecutionContext(
    @SerialName("variables")
    val variables: PassableMap,
    @SerialName("expression")
    val expression: String,
    @SerialName("computed")
    val computed: Map<String, List<PassableValue>>,
    @SerialName("device")
    val device: Map<String, List<PassableValue>>,
)

@Serializable
@SerialName("PassableMap")
data class PassableMap(
    @SerialName("map")
    val map: Map<String, PassableValue>,
)

@Serializable
@Polymorphic
sealed interface PassableValue {
    @Serializable
    @SerialName("list")
    data class ListValue(
        val value: List<PassableValue>,
    ) : PassableValue

    @Serializable
    @SerialName("map")
    data class MapValue(
        val value: Map<String, PassableValue>,
    ) : PassableValue

    @Serializable
    @SerialName("function")
    data class FunctionValue(
        val value: String,
        val args: PassableValue?,
    ) : PassableValue

    @Serializable
    @SerialName("int")
    data class IntValue(
        val value: Int,
    ) : PassableValue

    @Serializable
    @SerialName("uint")
    data class UIntValue(
        val value: ULong,
    ) : PassableValue

    @Serializable
    @SerialName("float")
    data class FloatValue(
        val value: Double,
    ) : PassableValue

    @Serializable
    @SerialName("string")
    data class StringValue(
        val value: String,
    ) : PassableValue

    @Serializable
    @SerialName("bytes")
    data class BytesValue(
        val value: ByteArray,
    ) : PassableValue

    @Serializable
    @SerialName("bool")
    data class BoolValue(
        val value: Boolean,
    ) : PassableValue

    @Serializable
    @SerialName("timestamp")
    data class TimestampValue(
        val value: Long,
    ) : PassableValue

    @Serializable
    @SerialName("Null")
    object NullValue : PassableValue
}

object CELResultDeserializer : KSerializer<CELResult> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("CELResult") {
            element<JsonElement>("Ok")
            element<JsonElement>("Err")
        }

    override fun deserialize(decoder: Decoder): CELResult {
        require(decoder is JsonDecoder) { "This deserializer can be used only with Json format" }
        val jsonElement = decoder.decodeJsonElement()

        return when {
            jsonElement.jsonObject.containsKey("Ok") -> {
                val okContent = jsonElement.jsonObject["Ok"]!!
                val passableValue = decoder.json.decodeFromJsonElement<PassableValue>(okContent)
                CELResult.Ok(passableValue)
            }

            jsonElement.jsonObject.containsKey("Err") -> {
                val errContent = jsonElement.jsonObject["Err"]!!.jsonPrimitive.content
                CELResult.Err(errContent)
            }

            else -> throw SerializationException("Unknown result type")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: CELResult,
    ) {
        TODO("Serialization not needed")
    }
}

@Serializable(with = CELResultDeserializer::class)
sealed class CELResult {
    @Serializable
    data class Ok(
        val value: PassableValue,
    ) : CELResult()

    @Serializable
    data class Err(
        val message: String,
    ) : CELResult()
}
