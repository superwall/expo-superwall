package com.superwall.sdk.models.events

import com.superwall.sdk.models.SerializableEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class EventsResponse(
    val status: Status,
    val invalidIndexes: List<Int>? = null,
) : SerializableEntity {
    @Serializable(with = StatusSerializer::class)
    enum class Status {
        OK,
        PARTIAL_SUCCESS,
    }
}

object StatusSerializer : KSerializer<EventsResponse.Status> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Status", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: EventsResponse.Status,
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): EventsResponse.Status {
        val value = decoder.decodeString()
        return try {
            EventsResponse.Status.valueOf(value.toUpperCase())
        } catch (e: IllegalArgumentException) {
            EventsResponse.Status.PARTIAL_SUCCESS
        }
    }
}
