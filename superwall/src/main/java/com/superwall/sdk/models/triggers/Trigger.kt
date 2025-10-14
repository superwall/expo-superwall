package com.superwall.sdk.models.triggers

import kotlinx.serialization.Serializable

@Serializable
data class Trigger(
    var eventName: String,
    var rules: List<TriggerRule>,
) {
    companion object {
        fun stub() =
            Trigger(
                eventName = "an_event",
                rules = emptyList(),
            )
    }
}
