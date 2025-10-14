package com.superwall.sdk.models.triggers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class RawInterval(
    @SerialName("type")
    val type: IntervalType,
    val minutes: Int? = null,
) {
    @Serializable
    enum class IntervalType {
        @SerialName("MINUTES")
        MINUTES,

        @SerialName("INFINITY")
        INFINITY,
    }
}

@Serializable
data class TriggerRuleOccurrence(
    @SerialName("key")
    val key: String,
    @SerialName("max_count")
    var maxCount: Int,
    @SerialName("interval")
    val rawInterval: RawInterval,
) {
    @Transient
    val interval: Interval =
        when (rawInterval.type) {
            RawInterval.IntervalType.MINUTES -> Interval.Minutes(rawInterval.minutes ?: 0)
            RawInterval.IntervalType.INFINITY -> Interval.Infinity
        }

    sealed class Interval {
        object Infinity : Interval()

        data class Minutes(
            val minutes: Int,
        ) : Interval()
    }

    companion object {
        fun stub() =
            TriggerRuleOccurrence(
                key = "abc",
                maxCount = 10,
                rawInterval = RawInterval(RawInterval.IntervalType.INFINITY),
            )
    }
}
