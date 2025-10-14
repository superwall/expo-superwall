package com.superwall.sdk.models.serialization

import com.superwall.sdk.utilities.dateFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*
import java.util.Calendar.*

@ExperimentalSerializationApi
class DateSerializerTest {
    @Test
    fun `test date serializer`() {
        val json = Json { serializersModule = SerializersModule { contextual(DateSerializer) } }

        val calendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time =
                    dateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse("2023-05-15T00:00:00.000Z")!!
                add(HOUR_OF_DAY, 13)
                add(MINUTE, 46)
                add(SECOND, 52)
                add(MILLISECOND, 789)
            }

        val originalDate = calendar.time

        val jsonString = json.encodeToString(DateSerializer, originalDate)
        assertEquals(jsonString, "\"2023-05-15T13:46:52.789\"")

        val deserializedDate = json.decodeFromString(DateSerializer, jsonString)
        assertEquals(originalDate, deserializedDate)
    }
}
