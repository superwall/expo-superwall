package com.superwall.sdk.models.serialization

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object AnySerializer : KSerializer<Any> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Any", StructureKind.OBJECT)

    @OptIn(InternalSerializationApi::class)
    private val listDescriptor: SerialDescriptor = buildSerialDescriptor("List<Any>", StructureKind.LIST)

    @OptIn(InternalSerializationApi::class)
    private val mapDescriptor: SerialDescriptor = buildSerialDescriptor("Map<String, Any>", StructureKind.MAP)

    override fun serialize(
        encoder: Encoder,
        value: Any,
    ) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Boolean -> encoder.encodeBoolean(value)
            is Int -> encoder.encodeInt(value)
            is Long -> encoder.encodeLong(value)
            is Float -> encoder.encodeFloat(value)
            is Double -> encoder.encodeDouble(value)
            is List<*> -> {
                val nonNullList = value.filterNotNull()
                val serializer = ListSerializer(AnySerializer)
                encoder.encodeSerializableValue(serializer, nonNullList)
            }
            is Map<*, *> -> {
                val convertedMap =
                    value.entries
                        .filter { it.value != null }
                        .associate { it.key.toString() to it.value!! }

                val mapSerializer = MapSerializer(String.serializer(), AnySerializer)
                encoder.encodeSerializableValue(mapSerializer, convertedMap)
            }
            null -> encoder.encodeNull()
            else -> {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.all,
                    "Warning: Unsupported type ${value::class}, skipping...",
                )
                encoder.encodeNull()
            }
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val input =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")

        return when (val element = input.decodeJsonElement()) {
            is JsonPrimitive -> deserializePrimitive(element)
            is JsonObject -> deserializeObject(element)
            is JsonArray -> deserializeArray(element)
            else -> throw SerializationException("Unknown type")
        }
    }

    private fun deserializePrimitive(element: JsonPrimitive): Any =
        when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.boolean
            element.intOrNull != null -> element.int
            element.longOrNull != null -> element.long
            element.doubleOrNull != null -> element.double
            else -> throw SerializationException("Unknown primitive type")
        }

    private fun deserializeObject(element: JsonObject): Map<String, Any> =
        element
            .mapValues { (_, value) ->
                when (value) {
                    is JsonPrimitive -> deserializePrimitive(value)
                    is JsonObject -> deserializeObject(value)
                    is JsonArray -> deserializeArray(value)
                    else -> throw SerializationException("Unknown type in JsonObject")
                }
            }.toMap()

    private fun deserializeArray(element: JsonArray): List<Any> =
        element.map { item ->
            when (item) {
                is JsonPrimitive -> deserializePrimitive(item)
                is JsonObject -> deserializeObject(item)
                is JsonArray -> deserializeArray(item)
                else -> throw SerializationException("Unknown type in JsonArray")
            }
        }

    // Helper function to get the appropriate serializer for a value
    fun serializerFor(value: Any): KSerializer<Any> =
        when (value) {
            is String -> String.serializer()
            is Boolean -> Boolean.serializer()
            is Int -> Int.serializer()
            is Long -> Long.serializer()
            is Float -> Float.serializer()
            is Double -> Double.serializer()
            is List<*> -> ListSerializer(AnySerializer)
            is Map<*, *> -> MapSerializer(String.serializer(), AnySerializer)
            else -> AnySerializer
        } as KSerializer<Any>
}
