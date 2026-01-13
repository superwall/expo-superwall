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
    private val json = Json { serializersModule = SerializersModule { contextual(DateSerializer) } }

    @Test
    fun `test date serializer`() {
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

    @Test
    fun `test date deserializer with Z suffix`() {
        val dateWithZ = "\"2023-05-15T13:46:52.789Z\""
        val deserializedDate = json.decodeFromString(DateSerializer, dateWithZ)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = deserializedDate }
        assertEquals(2023, calendar.get(YEAR))
        assertEquals(4, calendar.get(MONTH)) // May is month 4 (0-indexed)
        assertEquals(15, calendar.get(DAY_OF_MONTH))
        assertEquals(13, calendar.get(HOUR_OF_DAY))
        assertEquals(46, calendar.get(MINUTE))
        assertEquals(52, calendar.get(SECOND))
        assertEquals(789, calendar.get(MILLISECOND))
    }

    @Test
    fun `test date deserializer without milliseconds`() {
        val dateWithoutMillis = "\"2023-05-15T13:46:52\""
        val deserializedDate = json.decodeFromString(DateSerializer, dateWithoutMillis)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = deserializedDate }
        assertEquals(2023, calendar.get(YEAR))
        assertEquals(4, calendar.get(MONTH))
        assertEquals(15, calendar.get(DAY_OF_MONTH))
        assertEquals(13, calendar.get(HOUR_OF_DAY))
        assertEquals(46, calendar.get(MINUTE))
        assertEquals(52, calendar.get(SECOND))
    }

    @Test
    fun `test date deserializer without milliseconds with Z suffix`() {
        val dateWithoutMillisWithZ = "\"2023-05-15T13:46:52Z\""
        val deserializedDate = json.decodeFromString(DateSerializer, dateWithoutMillisWithZ)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = deserializedDate }
        assertEquals(2023, calendar.get(YEAR))
        assertEquals(4, calendar.get(MONTH))
        assertEquals(15, calendar.get(DAY_OF_MONTH))
        assertEquals(13, calendar.get(HOUR_OF_DAY))
        assertEquals(46, calendar.get(MINUTE))
        assertEquals(52, calendar.get(SECOND))
    }

    @Test
    fun `test date deserializer with epoch milliseconds`() {
        // 1765536941000 = 2025-12-12T10:55:41.000Z
        val epochMillis = "1765536941000"
        val deserializedDate = json.decodeFromString(DateSerializer, epochMillis)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = deserializedDate }
        assertEquals(2025, calendar.get(YEAR))
        assertEquals(11, calendar.get(MONTH)) // December is month 11 (0-indexed)
        assertEquals(12, calendar.get(DAY_OF_MONTH))
        assertEquals(10, calendar.get(HOUR_OF_DAY))
        assertEquals(55, calendar.get(MINUTE))
        assertEquals(41, calendar.get(SECOND))
    }
}
