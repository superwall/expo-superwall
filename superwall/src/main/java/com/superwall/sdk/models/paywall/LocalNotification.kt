package com.superwall.sdk.models.paywall

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable
data class LocalNotification(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerialName("type")
    val type: LocalNotificationType,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("body")
    val body: String,
    @SerialName("delay")
    val delay: Long,
)

@Serializable(with = LocalNotificationTypeSerializer::class)
sealed class LocalNotificationType(
    val raw: String,
) {
    @SerialName("TRIAL_STARTED")
    object TrialStarted : LocalNotificationType("TRIAL_STARTED")

    @SerialName("UNSUPPORTED")
    object Unsupported : LocalNotificationType("TRIAL_STARTED")
}

@Serializer(forClass = LocalNotificationType::class)
object LocalNotificationTypeSerializer : KSerializer<LocalNotificationType> {
    override fun deserialize(decoder: Decoder): LocalNotificationType =
        when (decoder.decodeString()) {
            "TRIAL_STARTED" -> LocalNotificationType.TrialStarted
            else -> LocalNotificationType.Unsupported
        }

    override fun serialize(
        encoder: Encoder,
        value: LocalNotificationType,
    ) {
        encoder.encodeString(
            when (value) {
                LocalNotificationType.TrialStarted -> "TRIAL_STARTED"
                LocalNotificationType.Unsupported -> "UNSUPPORTED"
            },
        )
    }
}
