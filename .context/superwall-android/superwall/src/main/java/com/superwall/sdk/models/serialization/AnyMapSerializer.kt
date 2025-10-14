package com.superwall.sdk.models.serialization

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = AnyMapSerializer::class)
data class AnyMap(
    val map: Map<String, Any>,
)

object AnyMapSerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AnyMap") {
            mapSerialDescriptor(String.serializer().descriptor, JsonElement.serializer().descriptor)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: Map<String, Any?>,
    ) {
        val jsonOutput =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can be saved only by Json")
        val jsonObject =
            buildJsonObject {
                value.forEach { (k, v) ->
                    when (v) {
                        is String -> put(k, JsonPrimitive(v))
                        is Int -> put(k, JsonPrimitive(v))
                        is Double -> put(k, JsonPrimitive(v))
                        is Boolean -> put(k, JsonPrimitive(v))
                        null -> put(k, JsonNull)
                        else -> {
                            // TODO: Figure out when this is happening
                            put(k, JsonNull)
                            Logger.debug(
                                LogLevel.debug,
                                LogScope.all,
                                "!! Warning: Unsupported type ${v::class}, skipping...",
                            )
//                        throw SerializationException("$v is not supported")
                        }
                    }
                }
            }
        jsonOutput.encodeJsonElement(jsonObject)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val jsonInput =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonInput.decodeJsonElement().jsonObject
        return jsonObject.map { it.key to it.value.jsonPrimitive.content }.toMap()
    }
}

//
// object AnyMapSerializer : KSerializer<Map<String, Any?>> {
//    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnyMap")
//
//    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
//        val output = encoder.beginStructure(descriptor)
//        value.forEach { (k, v) ->
//            val element = when (v) {
//                is String -> JsonPrimitive(v)
//                is Number -> JsonPrimitive(v)
//                is Boolean -> JsonPrimitive(v)
//                null -> JsonPrimitive(null as String?)
//                else -> {
//                    println("Warning: Unsupported type ${v::class}, skipping...")
//                    null
//                }
//            }
//            element?.let {
//                output.encodeSerializableElement(descriptor, 0, MapSerializer(String.serializer(), JsonElement.serializer()), mapOf(k to it))
//            }
//        }
//        output.endStructure(descriptor)
//    }
//
//    override fun deserialize(decoder: Decoder): Map<String, Any> {
//        val input = decoder.beginStructure(descriptor)
//        val map = mutableMapOf<String, Any>()
//        while (true) {
//            val index = input.decodeElementIndex(descriptor)
//            if (index == CompositeDecoder.DECODE_DONE) break
//            val entryMap = input.decodeSerializableElement(descriptor, index, MapSerializer(String.serializer(), JsonElement.serializer()))
//            entryMap.forEach { (key, value) ->
//                val transformedValue = when (value) {
//                    is JsonPrimitive -> when {
//                        value.isString -> value.content
//                        value.booleanOrNull != null -> value.boolean
//                        value.intOrNull != null -> value.int
//                        value.longOrNull != null -> value.long
//                        value.doubleOrNull != null -> value.double
//                        else -> null
//                    }
//                    else -> {
//                        println("Warning: Non-primitive value in map, skipping...")
//                        null
//                    }
//                }
//                transformedValue?.let {
//                    map[key] = it
//                }
//            }
//        }
//        input.endStructure(descriptor)
//        return map
//    }
// }

//
// object AnyMapSerializer : KSerializer<Map<String, Any>> {
//    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnyMap") {
//        element<Map<String, JsonElement>>("map")
//    }
//
//    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
//        val output = encoder.beginStructure(descriptor)
//        value.forEach { (k, v) ->
//            val element = when (v) {
//                is String -> JsonPrimitive(v)
//                is Number -> JsonPrimitive(v)
//                is Boolean -> JsonPrimitive(v)
//                null -> JsonPrimitive(null as String?)
//                else -> {
//                    println("Warning: Unsupported type ${v::class}, skipping...")
//                    null
//                }
//            }
//            element?.let {
//                output.encodeSerializableElement(descriptor, 0, MapSerializer(String.serializer(), JsonElement.serializer()), mapOf(k to it))
//            }
//        }
//        output.endStructure(descriptor)
//    }
//
//    override fun deserialize(decoder: Decoder): Map<String, Any> {
//        val input = decoder.beginStructure(descriptor)
//        val map = mutableMapOf<String, Any>()
//        while (true) {
//            val index = input.decodeElementIndex(descriptor)
//            if (index == CompositeDecoder.DECODE_DONE) break
//            val entryMap = input.decodeSerializableElement(descriptor, index, MapSerializer(String.serializer(), JsonElement.serializer()))
//            entryMap.forEach { (key, value) ->
//                val transformedValue = when (value) {
//                    is JsonPrimitive -> when {
//                        value.isString -> value.content
//                        value.booleanOrNull != null -> value.boolean
//                        value.intOrNull != null -> value.int
//                        value.longOrNull != null -> value.long
//                        value.doubleOrNull != null -> value.double
//                        else -> null
//                    }
//                    else -> {
//                        println("Warning: Non-primitive value in map, skipping...")
//                        null
//                    }
//                }
//                transformedValue?.let {
//                    map[key] = it
//                }
//            }
//        }
//        input.endStructure(descriptor)
//        return map
//    }
// }
