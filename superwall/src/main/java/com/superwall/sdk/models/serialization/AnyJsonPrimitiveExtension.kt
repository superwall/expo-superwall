package com.superwall.sdk.models.serialization

import kotlinx.serialization.json.JsonPrimitive

fun Any.isJsonPrimitable(): Boolean =
    when (this) {
        is String -> true
        is Boolean -> true
        is Int -> true
        is Long -> true
        is Float -> true
        is Double -> true
        else -> false
    }

fun Any.jsonPrimitive(): JsonPrimitive? =
    when (this) {
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        else -> null
    }
