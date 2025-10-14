package com.superwall.sdk.analytics.session

import com.superwall.sdk.models.serialization.DateSerializer
import kotlinx.serialization.*
import java.util.*

@Serializable
data class AppSession(
    @SerialName("app_session_id") var id: String = UUID.randomUUID().toString(),
    @SerialName("app_session_start_ts")
    @Serializable(with = DateSerializer::class)
    var startAt: Date = Date(),
    @Serializable(with = DateSerializer::class)
    @SerialName("app_session_end_ts")
    var endAt: Date? = null,
) {
    companion object {
        fun stub(): AppSession = AppSession()
    }
}
