package com.superwall.sdk.models.serialization

import com.superwall.sdk.utilities.DateUtils
import com.superwall.sdk.utilities.dateFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.*

@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    private val dateFormat =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() =
                dateFormat(DateUtils.ISO_MILLIS).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
        }

    // Date formats to try when deserializing, in order of preference
    private val dateFormats =
        object : ThreadLocal<List<SimpleDateFormat>>() {
            override fun initialValue() =
                listOf(
                    DateUtils.ISO_MILLIS,
                    DateUtils.ISO_MILLIS + "'Z'",
                    DateUtils.ISO_SECONDS,
                    DateUtils.ISO_SECONDS_TIMEZONE,
                ).map { format ->
                    dateFormat(format).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
        }

    override fun serialize(
        encoder: Encoder,
        value: Date,
    ) {
        encoder.encodeString(dateFormat.get()!!.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        // Handle both JSON number (epoch millis) and string formats
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                // Try parsing as epoch milliseconds first (number)
                element.longOrNull?.let { millis ->
                    return Date(millis)
                }
                // Fall back to string parsing
                val dateString = element.content
                return parseStringDate(dateString)
            }
        }
        // Fallback for non-JSON decoders
        val dateString = decoder.decodeString()
        return parseStringDate(dateString)
    }

    private fun parseStringDate(dateString: String): Date {
        for (format in dateFormats.get()!!) {
            try {
                return format.parse(dateString)!!
            } catch (e: Throwable) {
                // Try next format
            }
        }
        throw IllegalArgumentException("Invalid date format: $dateString")
    }
}
