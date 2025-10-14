package com.superwall.sdk.models.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FeatureGatingBehaviorSerializer::class)
sealed class FeatureGatingBehavior {
    @SerialName("GATED")
    object Gated : FeatureGatingBehavior()

    @SerialName("NON_GATED")
    object NonGated : FeatureGatingBehavior()
}

@Serializer(forClass = FeatureGatingBehavior::class)
object FeatureGatingBehaviorSerializer : KSerializer<FeatureGatingBehavior> {
    override fun serialize(
        encoder: Encoder,
        value: FeatureGatingBehavior,
    ) {
        val serialName =
            when (value) {
                is FeatureGatingBehavior.Gated -> "GATED"
                is FeatureGatingBehavior.NonGated -> "NON_GATED"
            }
        encoder.encodeString(serialName)
    }

    override fun deserialize(decoder: Decoder): FeatureGatingBehavior =
        when (val value = decoder.decodeString()) {
            "GATED" -> FeatureGatingBehavior.Gated
            "NON_GATED" -> FeatureGatingBehavior.NonGated
            else -> FeatureGatingBehavior.NonGated
        }
}
