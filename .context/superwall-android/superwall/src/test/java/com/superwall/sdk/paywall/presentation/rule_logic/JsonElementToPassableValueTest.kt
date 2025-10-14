package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.paywall.presentation.rule_logic.cel.toPassableValue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonElementToPassableValueTest {
    @Test
    fun `test JsonPrimitive string conversion to PassableValue`() {
        val jsonString = JsonPrimitive("test string")
        val passableValue = jsonString.toPassableValue()

        assertTrue(passableValue is PassableValue.StringValue)
        assertEquals("test string", (passableValue as PassableValue.StringValue).value)

        val jsonStringWithDollar = JsonPrimitive("$100.50")
        val passableValueWithDollar = jsonStringWithDollar.toPassableValue()

        assertTrue(passableValueWithDollar is PassableValue.StringValue)
        assertEquals("$100.50", (passableValueWithDollar as PassableValue.StringValue).value)
    }

    @Test
    fun `test JsonPrimitive number conversion to PassableValue`() {
        val jsonInt = JsonPrimitive(42)
        val passableIntValue = jsonInt.toPassableValue()

        assertTrue(passableIntValue is PassableValue.IntValue)
        assertEquals(42, (passableIntValue as PassableValue.IntValue).value)

        val jsonLong = JsonPrimitive(1000000000000L)
        val passableLongValue = jsonLong.toPassableValue()

        assertTrue(passableLongValue is PassableValue.UIntValue)
        assertEquals(1000000000000UL, (passableLongValue as PassableValue.UIntValue).value)

        val jsonDouble = JsonPrimitive(3.14159)
        val passableDoubleValue = jsonDouble.toPassableValue()

        assertTrue(passableDoubleValue is PassableValue.FloatValue)
        assertEquals(3.14159, (passableDoubleValue as PassableValue.FloatValue).value, 0.0)
    }

    @Test
    fun `test JsonPrimitive boolean conversion to PassableValue`() {
        val jsonTrue = JsonPrimitive(true)
        val passableTrueValue = jsonTrue.toPassableValue()

        assertTrue(passableTrueValue is PassableValue.BoolValue)
        assertEquals(true, (passableTrueValue as PassableValue.BoolValue).value)

        val jsonFalse = JsonPrimitive(false)
        val passableFalseValue = jsonFalse.toPassableValue()

        assertTrue(passableFalseValue is PassableValue.BoolValue)
        assertEquals(false, (passableFalseValue as PassableValue.BoolValue).value)
    }

    @Test
    fun `test JsonObject conversion to PassableValue`() {
        val jsonObject =
            buildJsonObject {
                put("string", "value")
                put("int", 42)
                put("double", 3.14)
                put("boolean", true)
                put("null", JsonNull)
            }

        val passableValue = jsonObject.toPassableValue()

        assertTrue(passableValue is PassableValue.MapValue)
        val resultMap = (passableValue as PassableValue.MapValue).value

        assertEquals(5, resultMap.size)

        assertTrue(resultMap["string"] is PassableValue.StringValue)
        assertEquals("value", (resultMap["string"] as PassableValue.StringValue).value)

        assertTrue(resultMap["int"] is PassableValue.IntValue)
        assertEquals(42, (resultMap["int"] as PassableValue.IntValue).value)

        assertTrue(resultMap["double"] is PassableValue.FloatValue)
        assertEquals(3.14, (resultMap["double"] as PassableValue.FloatValue).value, 0.0)

        assertTrue(resultMap["boolean"] is PassableValue.BoolValue)
        assertEquals(true, (resultMap["boolean"] as PassableValue.BoolValue).value)

        assertTrue(resultMap["null"] is PassableValue.StringValue)
        assertEquals("null", (resultMap["null"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test JsonArray conversion to PassableValue`() {
        val jsonArray =
            buildJsonArray {
                add("string")
                add(42)
                add(3.14)
                add(true)
                add(JsonNull)
            }

        val passableValue = jsonArray.toPassableValue()

        assertTrue(passableValue is PassableValue.ListValue)
        val resultList = (passableValue as PassableValue.ListValue).value

        assertEquals(5, resultList.size)

        assertTrue(resultList[0] is PassableValue.StringValue)
        assertEquals("string", (resultList[0] as PassableValue.StringValue).value)

        assertTrue(resultList[1] is PassableValue.IntValue)
        assertEquals(42, (resultList[1] as PassableValue.IntValue).value)

        assertTrue(resultList[2] is PassableValue.FloatValue)
        assertEquals(3.14, (resultList[2] as PassableValue.FloatValue).value, 0.0)

        assertTrue(resultList[3] is PassableValue.BoolValue)
        assertEquals(true, (resultList[3] as PassableValue.BoolValue).value)

        assertTrue(resultList[4] is PassableValue.StringValue)
        assertEquals("null", (resultList[4] as PassableValue.StringValue).value)
    }

    @Test
    fun `test nested JsonObject conversion to PassableValue`() {
        val nestedJsonObject =
            buildJsonObject {
                put("name", "John")
                put("age", 30)
                put(
                    "address",
                    buildJsonObject {
                        put("street", "123 Main St")
                        put("city", "New York")
                        put("zipcode", 10001)
                    },
                )
                put(
                    "hobbies",
                    buildJsonArray {
                        add("reading")
                        add("gaming")
                        add("coding")
                    },
                )
            }

        val passableValue = nestedJsonObject.toPassableValue()

        assertTrue(passableValue is PassableValue.MapValue)
        val resultMap = (passableValue as PassableValue.MapValue).value

        assertEquals(4, resultMap.size)

        assertEquals("John", (resultMap["name"] as PassableValue.StringValue).value)
        assertEquals(30, (resultMap["age"] as PassableValue.IntValue).value)

        assertTrue(resultMap["address"] is PassableValue.MapValue)
        val addressMap = (resultMap["address"] as PassableValue.MapValue).value

        assertEquals(3, addressMap.size)
        assertEquals("123 Main St", (addressMap["street"] as PassableValue.StringValue).value)
        assertEquals("New York", (addressMap["city"] as PassableValue.StringValue).value)
        assertEquals(10001, (addressMap["zipcode"] as PassableValue.IntValue).value)

        assertTrue(resultMap["hobbies"] is PassableValue.ListValue)
        val hobbiesList = (resultMap["hobbies"] as PassableValue.ListValue).value

        assertEquals(3, hobbiesList.size)
        assertEquals("reading", (hobbiesList[0] as PassableValue.StringValue).value)
        assertEquals("gaming", (hobbiesList[1] as PassableValue.StringValue).value)
        assertEquals("coding", (hobbiesList[2] as PassableValue.StringValue).value)
    }

    @Test
    fun `test nested JsonArray conversion to PassableValue`() {
        val nestedJsonArray =
            buildJsonArray {
                add("string")
                add(
                    buildJsonArray {
                        add(1)
                        add(2)
                        add(3)
                    },
                )
                add(
                    buildJsonObject {
                        put("key", "value")
                    },
                )
            }

        val passableValue = nestedJsonArray.toPassableValue()

        assertTrue(passableValue is PassableValue.ListValue)
        val resultList = (passableValue as PassableValue.ListValue).value

        assertEquals(3, resultList.size)

        assertTrue(resultList[0] is PassableValue.StringValue)
        assertEquals("string", (resultList[0] as PassableValue.StringValue).value)

        assertTrue(resultList[1] is PassableValue.ListValue)
        val nestedList = (resultList[1] as PassableValue.ListValue).value

        assertEquals(3, nestedList.size)
        assertEquals(1, (nestedList[0] as PassableValue.IntValue).value)
        assertEquals(2, (nestedList[1] as PassableValue.IntValue).value)
        assertEquals(3, (nestedList[2] as PassableValue.IntValue).value)

        assertTrue(resultList[2] is PassableValue.MapValue)
        val nestedMap = (resultList[2] as PassableValue.MapValue).value

        assertEquals(1, nestedMap.size)
        assertEquals("value", (nestedMap["key"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test complex nested JsonElement conversion to PassableValue`() {
        val complexJson =
            buildJsonObject {
                put(
                    "user",
                    buildJsonObject {
                        put("name", "John Doe")
                        put("age", 30)
                        put("isActive", true)
                        put(
                            "scores",
                            buildJsonArray {
                                add(85)
                                add(92)
                                add(78)
                            },
                        )
                    },
                )
                put(
                    "metadata",
                    buildJsonObject {
                        put("version", "1.0.0")
                        put("timestamp", 1625097600000L)
                        put(
                            "settings",
                            buildJsonObject {
                                put("darkMode", true)
                                put("fontSize", 14)
                            },
                        )
                    },
                )
                put(
                    "tags",
                    buildJsonArray {
                        add("important")
                        add("featured")
                        add(
                            buildJsonObject {
                                put("id", 1)
                                put("name", "special")
                            },
                        )
                    },
                )
            }

        val passableValue = complexJson.toPassableValue()

        assertTrue(passableValue is PassableValue.MapValue)
        val resultMap = (passableValue as PassableValue.MapValue).value

        assertEquals(3, resultMap.size)

        assertTrue(resultMap["user"] is PassableValue.MapValue)
        val userMap = (resultMap["user"] as PassableValue.MapValue).value

        assertEquals(4, userMap.size)
        assertEquals("John Doe", (userMap["name"] as PassableValue.StringValue).value)
        assertEquals(30, (userMap["age"] as PassableValue.IntValue).value)
        assertEquals(true, (userMap["isActive"] as PassableValue.BoolValue).value)

        assertTrue(userMap["scores"] is PassableValue.ListValue)
        val scoresList = (userMap["scores"] as PassableValue.ListValue).value

        assertEquals(3, scoresList.size)
        assertEquals(85, (scoresList[0] as PassableValue.IntValue).value)
        assertEquals(92, (scoresList[1] as PassableValue.IntValue).value)
        assertEquals(78, (scoresList[2] as PassableValue.IntValue).value)

        assertTrue(resultMap["metadata"] is PassableValue.MapValue)
        val metadataMap = (resultMap["metadata"] as PassableValue.MapValue).value

        assertEquals(3, metadataMap.size)
        assertEquals("1.0.0", (metadataMap["version"] as PassableValue.StringValue).value)
        assertEquals(1625097600000UL, (metadataMap["timestamp"] as PassableValue.UIntValue).value)

        assertTrue(metadataMap["settings"] is PassableValue.MapValue)
        val settingsMap = (metadataMap["settings"] as PassableValue.MapValue).value

        assertEquals(2, settingsMap.size)
        assertEquals(true, (settingsMap["darkMode"] as PassableValue.BoolValue).value)
        assertEquals(14, (settingsMap["fontSize"] as PassableValue.IntValue).value)

        assertTrue(resultMap["tags"] is PassableValue.ListValue)
        val tagsList = (resultMap["tags"] as PassableValue.ListValue).value

        assertEquals(3, tagsList.size)
        assertEquals("important", (tagsList[0] as PassableValue.StringValue).value)
        assertEquals("featured", (tagsList[1] as PassableValue.StringValue).value)

        assertTrue(tagsList[2] is PassableValue.MapValue)
        val tagMap = (tagsList[2] as PassableValue.MapValue).value

        assertEquals(2, tagMap.size)
        assertEquals(1, (tagMap["id"] as PassableValue.IntValue).value)
        assertEquals("special", (tagMap["name"] as PassableValue.StringValue).value)
    }
}
