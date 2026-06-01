package com.superwall.sdk.storage.core_data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ConvertersTest {
    @Test
    fun `Date is encoded as Long epoch millis`() {
        val date = Date(1_700_000_000_000L)
        val element = date.convertToJsonElement()
        assertTrue(element is JsonPrimitive)
        assertEquals(1_700_000_000_000L, (element as JsonPrimitive).long)
    }

    @Test
    fun `Date subclasses still encode as epoch millis`() {
        // java.sql.Date / Timestamp are common offenders that don't always
        // pass `is Date` under R8; the else-branch class check covers them.
        val sqlDate: Date = java.sql.Timestamp(1_700_000_000_000L)
        val element = sqlDate.convertToJsonElement()
        assertTrue(element is JsonPrimitive)
    }

    @Test
    fun `Date nested inside a Map is encoded`() {
        val map = mapOf<String, Any?>("ts" to Date(1L), "name" to "x")
        val element = map.convertToJsonElement()
        val obj = element as JsonObject
        assertEquals(1L, (obj["ts"] as JsonPrimitive).long)
        assertEquals("x", (obj["name"] as JsonPrimitive).content)
    }

    @Test
    fun `Date nested inside a List is encoded`() {
        val list = listOf<Any?>(Date(2L), "x")
        val element = list.convertToJsonElement()
        val arr = element as JsonArray
        assertEquals(2L, (arr[0] as JsonPrimitive).long)
    }

    @Test
    fun `Unknown type falls back to JsonNull rather than throwing`() {
        class Mystery
        val element = Mystery().convertToJsonElement()
        assertEquals(JsonNull, element)
    }

    @Test
    fun `null encodes as JsonNull`() {
        val nothing: Any? = null
        assertEquals(JsonNull, nothing.convertToJsonElement())
    }
}
