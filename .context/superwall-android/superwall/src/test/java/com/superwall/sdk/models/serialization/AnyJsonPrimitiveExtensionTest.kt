package com.superwall.sdk.models.serialization

import com.superwall.sdk.assertFalse
import com.superwall.sdk.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonPrimitivesTest {
    @Test
    fun testIsJsonPrimitable() {
        assertTrue("Hello".isJsonPrimitable())
        assertTrue(true.isJsonPrimitable())
        assertTrue(42.isJsonPrimitable())
        assertTrue(42L.isJsonPrimitable())
        assertTrue(3.14f.isJsonPrimitable())
        assertTrue(3.14.isJsonPrimitable())

        assertFalse(listOf(1, 2, 3).isJsonPrimitable())
        assertFalse(hashMapOf("a" to 1, "b" to 2).isJsonPrimitable())
    }

    @Test
    fun testJsonPrimitive() {
        assertEquals(JsonPrimitive("Hello"), "Hello".jsonPrimitive())
        assertEquals(JsonPrimitive(true), true.jsonPrimitive())
        assertEquals(JsonPrimitive(42), 42.jsonPrimitive())
        assertEquals(JsonPrimitive(42L), 42L.jsonPrimitive())
        assertEquals(JsonPrimitive(3.14f), 3.14f.jsonPrimitive())
        assertEquals(JsonPrimitive(3.14), 3.14.jsonPrimitive())

        assertNull(listOf(1, 2, 3).jsonPrimitive())
        assertNull(hashMapOf("a" to 1, "b" to 2).jsonPrimitive())
    }
}
