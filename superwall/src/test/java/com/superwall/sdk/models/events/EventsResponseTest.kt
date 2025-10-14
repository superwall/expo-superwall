package com.superwall.sdk.models.events

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsResponseTest {
    @Test
    fun testSerialization() {
        val response = EventsResponse(EventsResponse.Status.OK, listOf(1, 2, 3))
        val json = Json.encodeToString(EventsResponse.serializer(), response)
        val expectedJson = """{"status":"OK","invalidIndexes":[1,2,3]}"""
        assertEquals(expectedJson, json)
    }

    @Test
    fun testDeserialization() {
        val json = """{"status":"PARTIAL_SUCCESS","invalidIndexes":null}"""
        val response = Json.decodeFromString(EventsResponse.serializer(), json)
        assertEquals(EventsResponse.Status.PARTIAL_SUCCESS, response.status)
        assertEquals(null, response.invalidIndexes)
    }

    @Test
    fun testDeserializationWithInvalidStatus() {
        val json = """{"status":"INVALID_STATUS","invalidIndexes":null}"""
        val response = Json.decodeFromString(EventsResponse.serializer(), json)
        assertEquals(EventsResponse.Status.PARTIAL_SUCCESS, response.status)
        assertEquals(null, response.invalidIndexes)
    }
}
