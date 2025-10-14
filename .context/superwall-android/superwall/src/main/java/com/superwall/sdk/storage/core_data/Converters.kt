package com.superwall.sdk.storage.core_data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.util.Date

class Converters {
    companion object {
        private val jsonParser by lazy {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        }
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>): String = jsonParser.fromTypedMap(sanitizeMap(map))

    @TypeConverter
    fun toMap(json: String): Map<String, Any> = sanitizeMap(jsonParser.toTypedMap(json))

    @TypeConverter
    fun toDate(timestamp: Long): Date = Date(timestamp)

    @TypeConverter
    fun toTimestamp(date: Date?): Long? = date?.time

    /**
     * Filters out any values that can't be saved to the database.
     */
    fun sanitizeMap(map: Map<String, Any>): Map<String, Any> =
        map.filterValues { value ->
            when (value) {
                is String, is Number, is Boolean, is List<*>, is Map<*, *> -> true
                else -> false
            }
        }
}

fun Json.fromTypedMap(map: Map<String, Any?>): String {
    val jsonObject = JsonObject(map.mapValues { it.value.convertToJsonElement() }.toMap())
    return encodeToString(jsonObject)
}

fun Json.toTypedMap(jsonStr: String): Map<String, Any> {
    val jsonObject = decodeFromString<JsonObject>(jsonStr)
    return jsonObject
        .map { (k, v) -> k to v.convertFromJsonElement() }
        .filter { it.second != null }
        .toMap() as Map<String, Any>
}

fun Json.toNullableTypedMap(jsonStr: String): Map<String, Any> {
    val jsonObject = decodeFromString<JsonObject>(jsonStr)
    return jsonObject
        .map { (k, v) -> k to v.convertFromJsonElement() }
        .toMap() as Map<String, Any>
}

fun Any?.convertToJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is List<*> -> JsonArray(this.map { it.convertToJsonElement() })
        is Array<*> -> JsonArray(this.map { it.convertToJsonElement() })
        is Map<*, *> ->
            JsonObject(
                this.entries.associate {
                    it.key.toString() to it.value.convertToJsonElement()
                },
            )

        else -> throw IllegalArgumentException("Unsupported type: ${this!!::class}")
    }

// Helper function to convert JsonElement back to basic types
fun JsonElement.convertFromJsonElement(): Any? =
    when (this) {
        is JsonNull -> null
        is JsonPrimitive ->
            when {
                this.isString -> this.content
                this.intOrNull != null -> this.int.toDouble()
                this.longOrNull != null -> this.long
                this.doubleOrNull != null -> this.double
                this.booleanOrNull != null -> this.boolean
                else -> this.content
            }

        is JsonArray -> this.map { it.convertFromJsonElement() }
        is JsonObject -> this.map { (k, v) -> k to v.convertFromJsonElement() }.toMap()
    }
