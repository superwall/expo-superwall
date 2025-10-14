package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.paywall.presentation.rule_logic.cel.toPassableValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapToPassableValueTest {
    @Test
    fun `test Map toPassableValue with simple values`() {
        val map =
            mapOf(
                "string" to "value",
                "int" to 42,
                "double" to 3.14,
                "boolean" to true,
                "long" to 1000L,
                "ulong" to 2000UL,
            )

        val passableValue = map.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(6, resultMap.size)

        assertTrue(resultMap["string"] is PassableValue.StringValue)
        assertEquals("value", (resultMap["string"] as PassableValue.StringValue).value)

        assertTrue(resultMap["int"] is PassableValue.IntValue)
        assertEquals(42, (resultMap["int"] as PassableValue.IntValue).value)

        assertTrue(resultMap["double"] is PassableValue.FloatValue)
        assertEquals(3.14, (resultMap["double"] as PassableValue.FloatValue).value, 0.0)

        assertTrue(resultMap["boolean"] is PassableValue.BoolValue)
        assertEquals(true, (resultMap["boolean"] as PassableValue.BoolValue).value)

        assertTrue(resultMap["long"] is PassableValue.UIntValue)
        assertEquals(1000UL, (resultMap["long"] as PassableValue.UIntValue).value)

        assertTrue(resultMap["ulong"] is PassableValue.UIntValue)
        assertEquals(2000UL, (resultMap["ulong"] as PassableValue.UIntValue).value)
    }

    @Test
    fun `test Map toPassableValue with nested structures`() {
        val nestedMap =
            mapOf(
                "nested_map" to
                    mapOf(
                        "key1" to "value1",
                        "key2" to 42,
                    ),
                "nested_list" to listOf(1, 2, 3),
                "mixed_list" to
                    listOf(
                        "string",
                        mapOf("inner_key" to "inner_value"),
                        42,
                    ),
            )

        val passableValue = nestedMap.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(3, resultMap.size)

        assertTrue(resultMap["nested_map"] is PassableValue.MapValue)
        val nestedMapValue = (resultMap["nested_map"] as PassableValue.MapValue).value
        assertEquals(2, nestedMapValue.size)
        assertEquals("value1", (nestedMapValue["key1"] as PassableValue.StringValue).value)
        assertEquals(42, (nestedMapValue["key2"] as PassableValue.IntValue).value)

        assertTrue(resultMap["nested_list"] is PassableValue.ListValue)
        val nestedListValue = (resultMap["nested_list"] as PassableValue.ListValue).value
        assertEquals(3, nestedListValue.size)
        assertEquals(1, (nestedListValue[0] as PassableValue.IntValue).value)
        assertEquals(2, (nestedListValue[1] as PassableValue.IntValue).value)
        assertEquals(3, (nestedListValue[2] as PassableValue.IntValue).value)

        assertTrue(resultMap["mixed_list"] is PassableValue.ListValue)
        val mixedListValue = (resultMap["mixed_list"] as PassableValue.ListValue).value
        assertEquals(3, mixedListValue.size)
        assertEquals("string", (mixedListValue[0] as PassableValue.StringValue).value)
        assertTrue(mixedListValue[1] is PassableValue.MapValue)
        assertEquals(42, (mixedListValue[2] as PassableValue.IntValue).value)

        val innerMap = (mixedListValue[1] as PassableValue.MapValue).value
        assertEquals("inner_value", (innerMap["inner_key"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test Map toPassableValue with null values`() {
        val mapWithNulls =
            mapOf(
                "key1" to "value",
                "key2" to null,
            )

        val passableValue = mapWithNulls.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(2, resultMap.size)

        assertTrue(resultMap["key1"] is PassableValue.StringValue)
        assertEquals("value", (resultMap["key1"] as PassableValue.StringValue).value)

        assertTrue(resultMap["key2"] is PassableValue.NullValue)
    }

    @Test
    fun `test Map toPassableValue with dollar sign in strings`() {
        val mapWithDollarSigns =
            mapOf(
                "price" to "$10.99",
                "code" to "discount$123",
            )

        val passableValue = mapWithDollarSigns.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(2, resultMap.size)

        assertTrue(resultMap["price"] is PassableValue.StringValue)
        assertEquals("10.99", (resultMap["price"] as PassableValue.StringValue).value)

        assertTrue(resultMap["code"] is PassableValue.StringValue)
        assertEquals("discount123", (resultMap["code"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test Map toPassableValue with non-string keys`() {
        val mixedKeyMap =
            mapOf(
                "string_key" to "value1",
                42 to "value2",
                true to "value3",
            )

        val passableValue = mixedKeyMap.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value

        assertEquals(1, resultMap.size)
        assertTrue(resultMap.containsKey("string_key"))
        assertEquals("value1", (resultMap["string_key"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test Map toPassableValue with complex objects`() {
        @Serializable
        data class User(
            val name: String,
            val age: Int,
        )

        val complexMap =
            mapOf(
                "user" to User("John", 30),
                "settings" to
                    mapOf(
                        "darkMode" to true,
                        "notifications" to listOf("email", "push"),
                    ),
            )

        val passableValue = complexMap.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(2, resultMap.size)
        println(resultMap)
        assertTrue(resultMap["user"] is PassableValue.StringValue)
        val userMap = (resultMap["user"] as PassableValue.StringValue).value

        assertTrue(resultMap["settings"] is PassableValue.MapValue)
        val settingsMap = (resultMap["settings"] as PassableValue.MapValue).value
        assertEquals(2, settingsMap.size)
        assertEquals(true, (settingsMap["darkMode"] as PassableValue.BoolValue).value)

        assertTrue(settingsMap["notifications"] is PassableValue.ListValue)
        val notificationsList = (settingsMap["notifications"] as PassableValue.ListValue).value
        assertEquals(2, notificationsList.size)
        assertEquals("email", (notificationsList[0] as PassableValue.StringValue).value)
        assertEquals("push", (notificationsList[1] as PassableValue.StringValue).value)
    }

    @Test
    fun `test Map toPassableValue with JsonElement values`() {
        val jsonObject =
            buildJsonObject {
                put("name", "John")
                put("age", 30)
                put("isActive", true)
            }

        val mapWithJson =
            mapOf(
                "user_data" to jsonObject,
            )

        val passableValue = mapWithJson.toPassableValue()
        assertTrue(passableValue is PassableValue.MapValue)

        val resultMap = (passableValue as PassableValue.MapValue).value
        assertEquals(1, resultMap.size)

        assertTrue(resultMap["user_data"] is PassableValue.MapValue)
        val jsonMap = (resultMap["user_data"] as PassableValue.MapValue).value
        assertEquals(3, jsonMap.size)
        assertEquals("John", (jsonMap["name"] as PassableValue.StringValue).value)
        assertEquals(30, (jsonMap["age"] as PassableValue.IntValue).value)
        assertEquals(true, (jsonMap["isActive"] as PassableValue.BoolValue).value)
    }
}
