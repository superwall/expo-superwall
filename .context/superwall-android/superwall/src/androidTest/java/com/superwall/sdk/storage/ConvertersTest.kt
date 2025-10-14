package com.superwall.sdk.storage

import com.superwall.sdk.storage.core_data.Converters
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.ArrayList
import java.util.Date

class ConvertersTest {
    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun testMapConversion() {
        val originalMap =
            mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "list" to listOf(1, 2, 3),
                "nestedMap" to mapOf("key" to "value"),
            )

        val expectedJson =
            """
            {"string":"value","number":42,"boolean":true,"list":[1,2,3],"nestedMap":{"key":"value"}}
            """.trimIndent()

        val json = converters.fromMap(originalMap)
        println(expectedJson)
        println("\n\n\n")
        println(json)
        assertEquals(expectedJson, json)

        val convertedMap = converters.toMap(json)
        assertEquals(originalMap["string"], convertedMap["string"])
        assertEquals(originalMap["number"], (convertedMap["number"] as Double).toInt())
        assertEquals(originalMap["boolean"], convertedMap["boolean"])
        assertEquals(originalMap["list"], (convertedMap["list"] as ArrayList<Double>).map { it.toInt() })

        @Suppress("UNCHECKED_CAST")
        val nestedMap = convertedMap["nestedMap"] as Map<String, String>
        assertEquals("value", nestedMap["key"])
    }

    @Test
    fun testComplexJsonStructure() {
        val complexMap =
            mapOf(
                "nested" to
                    mapOf(
                        "array" to
                            listOf(
                                mapOf("id" to 1, "name" to "item1"),
                                mapOf("id" to 2, "name" to "item2"),
                            ),
                        "metadata" to
                            mapOf(
                                "version" to 1.0,
                                "active" to true,
                            ),
                    ),
            )

        val expectedJson =
            """
            {"nested":{"array":[{"id":1,"name":"item1"},{"id":2,"name":"item2"}],"metadata":{"version":1.0,"active":true}}}
            """.trimIndent()

        val json = converters.fromMap(complexMap)
        assertEquals(expectedJson, json)

        val convertedMap = converters.toMap(json)

        @Suppress("UNCHECKED_CAST")
        val nested = convertedMap["nested"] as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val array = nested["array"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val metadata = nested["metadata"] as Map<String, Any>

        assertEquals(2, array.size)
        assertEquals(1.0, array[0]["id"])
        assertEquals("item1", array[0]["name"])
        assertEquals(1.0, metadata["version"])
        assertEquals(true, metadata["active"])
    }

    @Test
    fun testDateConversion() {
        val now = Date()
        val timestamp = converters.toTimestamp(now)
        val convertedDate = converters.toDate(timestamp!!)

        assertEquals(now.time, convertedDate.time)
    }

    @Test
    fun testNullDateConversion() {
        val timestamp = converters.toTimestamp(null)
        assertNull(timestamp)
    }

    @Test
    fun testSanitizeMap() {
        val unsanitizedMap =
            mapOf(
                "validString" to "test",
                "validNumber" to 42,
                "validBoolean" to true,
                "validList" to listOf(1, 2, 3),
                "validNestedMap" to mapOf("key" to "value"),
                "invalidObject" to Object(),
            )

        val sanitizedMap = converters.sanitizeMap(unsanitizedMap)

        val expectedJson =
            """
            {"validString":"test","validNumber":42,"validBoolean":true,"validList":[1,2,3],"validNestedMap":{"key":"value"}}
            """.trimIndent()

        val json = converters.fromMap(sanitizedMap)
        assertEquals(expectedJson, json)

        assertTrue(sanitizedMap.containsKey("validString"))
        assertTrue(sanitizedMap.containsKey("validNumber"))
        assertTrue(sanitizedMap.containsKey("validBoolean"))
        assertTrue(sanitizedMap.containsKey("validList"))
        assertTrue(sanitizedMap.containsKey("validNestedMap"))
        assertFalse(sanitizedMap.containsKey("invalidObject"))
    }
}
