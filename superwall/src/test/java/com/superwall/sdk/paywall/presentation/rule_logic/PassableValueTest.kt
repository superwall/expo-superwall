package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.paywall.presentation.rule_logic.cel.toPassableValue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassableValueTest {
    @Test
    fun `test primitive types conversion to PassableValue`() {
        val intValue = 42
        val intPassable = intValue.toPassableValue()
        assertTrue(intPassable is PassableValue.IntValue)
        assertEquals(intValue, (intPassable as PassableValue.IntValue).value)

        val longValue = 42L
        val longPassable = longValue.toPassableValue()
        assertTrue(longPassable is PassableValue.UIntValue)
        assertEquals(longValue.toULong(), (longPassable as PassableValue.UIntValue).value)

        val uLongValue = 42UL
        val uLongPassable = uLongValue.toPassableValue()
        assertTrue(uLongPassable is PassableValue.UIntValue)
        assertEquals(uLongValue, (uLongPassable as PassableValue.UIntValue).value)

        val doubleValue = 42.5
        val doublePassable = doubleValue.toPassableValue()
        assertTrue(doublePassable is PassableValue.FloatValue)
        assertEquals(doubleValue, (doublePassable as PassableValue.FloatValue).value, 0.0)

        val stringValue = "test"
        val stringPassable = stringValue.toPassableValue()
        assertTrue(stringPassable is PassableValue.StringValue)
        assertEquals(stringValue, (stringPassable as PassableValue.StringValue).value)

        val dollarStringValue = "test$123"
        val dollarStringPassable = dollarStringValue.toPassableValue()
        assertTrue(dollarStringPassable is PassableValue.StringValue)
        assertEquals("test123", (dollarStringPassable as PassableValue.StringValue).value)

        val boolValue = true
        val boolPassable = boolValue.toPassableValue()
        assertTrue(boolPassable is PassableValue.BoolValue)
        assertEquals(boolValue, (boolPassable as PassableValue.BoolValue).value)
    }

    @Test
    fun `test List conversion to PassableValue`() {
        val listValue = listOf(1, "test", true, 42.5)
        val listPassable = listValue.toPassableValue()

        assertTrue(listPassable is PassableValue.ListValue)
        val passableList = (listPassable as PassableValue.ListValue).value

        assertEquals(4, passableList.size)
        assertTrue(passableList[0] is PassableValue.IntValue)
        assertEquals(1, (passableList[0] as PassableValue.IntValue).value)

        assertTrue(passableList[1] is PassableValue.StringValue)
        assertEquals("test", (passableList[1] as PassableValue.StringValue).value)

        assertTrue(passableList[2] is PassableValue.BoolValue)
        assertEquals(true, (passableList[2] as PassableValue.BoolValue).value)

        assertTrue(passableList[3] is PassableValue.FloatValue)
        assertEquals(42.5, (passableList[3] as PassableValue.FloatValue).value, 0.0)

        val listWithNull = listOf(1, null, "test")
        val listWithNullPassable = listWithNull.toPassableValue()

        assertTrue(listWithNullPassable is PassableValue.ListValue)
        val passableListWithNull = (listWithNullPassable as PassableValue.ListValue).value

        assertEquals(3, passableListWithNull.size)
        assertTrue(passableListWithNull[0] is PassableValue.IntValue)
        assertTrue(passableListWithNull[1] is PassableValue.NullValue)
        assertTrue(passableListWithNull[2] is PassableValue.StringValue)
    }

    @Test
    fun `test Map conversion to PassableValue`() {
        val mapValue =
            mapOf(
                "int" to 1,
                "string" to "test",
                "bool" to true,
                "double" to 42.5,
            )
        val mapPassable = mapValue.toPassableValue()

        assertTrue(mapPassable is PassableValue.MapValue)
        val passableMap = (mapPassable as PassableValue.MapValue).value

        assertEquals(4, passableMap.size)
        assertTrue(passableMap["int"] is PassableValue.IntValue)
        assertEquals(1, (passableMap["int"] as PassableValue.IntValue).value)

        assertTrue(passableMap["string"] is PassableValue.StringValue)
        assertEquals("test", (passableMap["string"] as PassableValue.StringValue).value)

        assertTrue(passableMap["bool"] is PassableValue.BoolValue)
        assertEquals(true, (passableMap["bool"] as PassableValue.BoolValue).value)

        assertTrue(passableMap["double"] is PassableValue.FloatValue)
        assertEquals(42.5, (passableMap["double"] as PassableValue.FloatValue).value, 0.0)

        val mapWithNull =
            mapOf(
                "value" to 1,
                "null" to null,
            )
        val mapWithNullPassable = mapWithNull.toPassableValue()

        assertTrue(mapWithNullPassable is PassableValue.MapValue)
        val passableMapWithNull = (mapWithNullPassable as PassableValue.MapValue).value

        assertEquals(2, passableMapWithNull.size)
        assertTrue(passableMapWithNull["value"] is PassableValue.IntValue)
        assertTrue(passableMapWithNull["null"] is PassableValue.NullValue)

        val mixedKeyMap =
            mapOf(
                "string_key" to "value",
                42 to "ignored",
            )
        val mixedKeyMapPassable = mixedKeyMap.toPassableValue()

        assertTrue(mixedKeyMapPassable is PassableValue.MapValue)
        val passableMixedKeyMap = (mixedKeyMapPassable as PassableValue.MapValue).value

        assertEquals(1, passableMixedKeyMap.size)
        assertTrue(passableMixedKeyMap.containsKey("string_key"))
        assertTrue(!passableMixedKeyMap.containsKey("42"))
    }

    @Test
    fun `test nested structures conversion to PassableValue`() {
        val nestedStructure =
            mapOf(
                "list" to listOf(1, 2, 3),
                "map" to mapOf("key" to "value"),
            )
        val nestedPassable = nestedStructure.toPassableValue()

        assertTrue(nestedPassable is PassableValue.MapValue)
        val passableNested = (nestedPassable as PassableValue.MapValue).value

        assertTrue(passableNested["list"] is PassableValue.ListValue)
        val nestedList = (passableNested["list"] as PassableValue.ListValue).value
        assertEquals(3, nestedList.size)

        assertTrue(passableNested["map"] is PassableValue.MapValue)
        val nestedMap = (passableNested["map"] as PassableValue.MapValue).value
        assertEquals("value", (nestedMap["key"] as PassableValue.StringValue).value)
    }

    @Test
    fun `test JsonElement conversion to PassableValue`() {
        val jsonObject =
            buildJsonObject {
                put("int", 1)
                put("string", "test")
                put("bool", true)
                put("double", 42.5)
            }
        val jsonObjectPassable = jsonObject.toPassableValue()

        assertTrue(jsonObjectPassable is PassableValue.MapValue)
        val passableJsonObject = (jsonObjectPassable as PassableValue.MapValue).value

        assertEquals(4, passableJsonObject.size)
        assertTrue(passableJsonObject["int"] is PassableValue.IntValue)
        assertTrue(passableJsonObject["string"] is PassableValue.StringValue)
        assertTrue(passableJsonObject["bool"] is PassableValue.BoolValue)
        assertTrue(passableJsonObject["double"] is PassableValue.FloatValue)

        val jsonArray =
            buildJsonArray {
                add(1)
                add("test")
                add(true)
                add(42.5)
            }
        val jsonArrayPassable = jsonArray.toPassableValue()

        assertTrue(jsonArrayPassable is PassableValue.ListValue)
        val passableJsonArray = (jsonArrayPassable as PassableValue.ListValue).value

        assertEquals(4, passableJsonArray.size)
        assertTrue(passableJsonArray[0] is PassableValue.IntValue)
        assertTrue(passableJsonArray[1] is PassableValue.StringValue)
        assertTrue(passableJsonArray[2] is PassableValue.BoolValue)
        assertTrue(passableJsonArray[3] is PassableValue.FloatValue)

        val jsonIntPrimitive = JsonPrimitive(42)
        val jsonIntPassable = jsonIntPrimitive.toPassableValue()
        assertTrue(jsonIntPassable is PassableValue.IntValue)

        val jsonStringPrimitive = JsonPrimitive("test")
        val jsonStringPassable = jsonStringPrimitive.toPassableValue()
        assertTrue(jsonStringPassable is PassableValue.StringValue)

        val jsonBoolPrimitive = JsonPrimitive(true)
        val jsonBoolPassable = jsonBoolPrimitive.toPassableValue()
        assertTrue(jsonBoolPassable is PassableValue.BoolValue)

        val jsonDoublePrimitive = JsonPrimitive(42.5)
        val jsonDoublePassable = jsonDoublePrimitive.toPassableValue()
        assertTrue(jsonDoublePassable is PassableValue.FloatValue)
    }

    @Test
    fun `test fallback to string for non-serializable objects`() {
        class NonSerializable {
            override fun toString(): String = "NonSerializable object"
        }

        val nonSerializable = NonSerializable()
        val nonSerializablePassable = nonSerializable.toPassableValue()

        assertTrue(nonSerializablePassable is PassableValue.StringValue)
        assertEquals("NonSerializable object", (nonSerializablePassable as PassableValue.StringValue).value)
    }
}
