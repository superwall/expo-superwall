package com.superwall.sdk.models.paywall

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

@Serializable
class LocalNotification(
    @SerialName("id")
    val id: Int = Random.nextInt(),
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
sealed class LocalNotificationType {
    @SerialName("TRIAL_STARTED")
    object TrialStarted : LocalNotificationType()

    @SerialName("UNSUPPORTED")
    object Unsupported : LocalNotificationType()
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
