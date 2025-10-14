package com.superwall.sdk.models.serialization

import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class AnyMap(
    val map: Map<
        String,
        @kotlinx.serialization.Serializable(with = AnySerializer::class)
        Any,
    >,
)

class AnyMapSerializerTest {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
/*
    @Test
    fun testSerializeWithSupportedTypes() {
        var map = AnyMap(mapOf("key1" to 123, "key2" to "value2", "key3" to true))
        val expectedJson = """
            {
              "map": {
                "key1": 123,
                "key2": "value2",
                "key3": true
              }
            }
        """.trimIndent()
        // Check each key
        var json = JSONObject(json.encodeToString(AnyMap.serializer(), map)).getJSONObject("map")
        var expected = JSONObject(expectedJson).getJSONObject("map")

        // Check each key in map
        assertEquals(expected.get("key1"), json.get("key1"))
        assertEquals(expected.get("key2"), json.get("key2"))
        assertEquals(expected.get("key3"), json.get("key3"))
    }

    @Test
    fun testSerializeWithUnsupportedType() {
        val map = AnyMap(mapOf("key1" to 123, "key2" to "value2", "key3" to Any()))
        val expectedJson = """
        {
          "map": {
            "key1": 123,
            "key2": "value2"
          }
        }
    """.trimIndent()


        val encodedString = json.encodeToString(AnyMap.serializer(), map)
        println("encodedString: $encodedString")

        val json = JSONObject(json.encodeToString(AnyMap.serializer(), map)).getJSONObject("map")
        val expected = JSONObject(expectedJson).getJSONObject("map")

        assertEquals(expected.get("key1"), json.get("key1"))
        assertEquals(expected.get("key2"), json.get("key2"))
    }

    @Test
    fun testDeserializeWithSupportedTypes() {
        val jsonStr = """
        {
          "map": {
            "key1": 123,
            "key2": "value2",
            "key3": true
          }
        }
    """.trimIndent()

        val map = json.decodeFromString(AnyMap.serializer(), jsonStr)
        val json = JSONObject(jsonStr).getJSONObject("map")

        assertEquals(json.get("key1"), map.map["key1"])
        assertEquals(json.get("key2"), map.map["key2"])
        assertEquals(json.get("key3"), map.map["key3"])
    }


    @Test
    fun testDeserializeWithUnsupportedType() {
        val jsonStr = """
      {
          "map": {
            "key1": 123,
            "key2": "value2",
            "key3": {}
          }
        }
       """.trimIndent()
        val map = json.decodeFromString(AnyMap.serializer(), jsonStr)

        val json = JSONObject(jsonStr).getJSONObject("map")

        assertEquals(json.get("key1"), map.map["key1"])
        assertEquals(json.get("key2"), map.map["key2"])

        // TODO: Fix this test
        // As "key3" is an unsupported type, we expect it not to be present in the decoded map
//    assertFalse(map.map.containsKey("key3"))
    }
 */
}
