package com.superwall.sdk.models.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert.assertEquals
import org.junit.Test

class ExceptionSerializerTest {
    private val json =
        Json {
            serializersModule = SerializersModule { contextual(Exception::class, ExceptionSerializer) }
        }

    @Test
    fun `Exception is serialized and deserialized correctly`() {
        val originalException = Exception("Test exception")
        val jsonString = json.encodeToString(ExceptionSerializer, originalException)
        val restoredException = json.decodeFromString(ExceptionSerializer, jsonString)

        assertEquals(originalException.message, restoredException.message)
    }

    @Test
    fun `Exception with null message is serialized and deserialized correctly`() {
        val originalException = Exception()
        val jsonString = json.encodeToString(ExceptionSerializer, originalException)
        val restoredException = json.decodeFromString(ExceptionSerializer, jsonString)

        assertEquals("Unknown exception", restoredException.message)
    }
}
