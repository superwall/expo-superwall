package com.superwall.sdk.config.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = OnDeviceCachingSerializer::class)
sealed class OnDeviceCaching {
    @SerialName("ENABLED")
    object Enabled : OnDeviceCaching()

    @SerialName("DISABLED")
    object Disabled : OnDeviceCaching()
}

@Serializer(forClass = OnDeviceCaching::class)
object OnDeviceCachingSerializer : KSerializer<OnDeviceCaching> {
    override fun serialize(
        encoder: Encoder,
        value: OnDeviceCaching,
    ) {
        val serialName =
            when (value) {
                is OnDeviceCaching.Enabled -> "ENABLED"
                is OnDeviceCaching.Disabled -> "DISABLED"
            }
        encoder.encodeString(serialName)
    }

    override fun deserialize(decoder: Decoder): OnDeviceCaching =
        when (val value = decoder.decodeString()) {
            "ENABLED" -> OnDeviceCaching.Enabled
            "DISABLED" -> OnDeviceCaching.Disabled
            else -> OnDeviceCaching.Disabled
        }
}
