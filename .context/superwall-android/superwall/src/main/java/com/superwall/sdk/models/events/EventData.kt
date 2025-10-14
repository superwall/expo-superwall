package com.superwall.sdk.models.events

import com.superwall.sdk.models.serialization.AnySerializer
import com.superwall.sdk.models.serialization.DateSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import java.util.*

@kotlinx.serialization.Serializable
data class EventData(
    @SerialName("event_id")
    val id: String = UUID.randomUUID().toString(),
    @SerialName("event_name")
    val name: String,
    @SerialName("parameters")
    val parameters: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any,
    >,
    @Serializable(with = DateSerializer::class)
    @SerialName("created_at")
    val createdAt: Date,
) {
    companion object {
        fun stub(): EventData =
            EventData(
                name = "opened_application",
                parameters = emptyMap(),
                createdAt = Date(),
            )
    }
}
