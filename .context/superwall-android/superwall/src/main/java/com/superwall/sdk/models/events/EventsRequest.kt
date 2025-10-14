package com.superwall.sdk.models.events

@kotlinx.serialization.Serializable
data class EventsRequest(
    val events: List<EventData>,
)
