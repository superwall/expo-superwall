package com.superwall.sdk.store.abstractions.product.receipt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LatestSubscriptionStateSerializer::class)
enum class LatestSubscriptionState {
    GRACE_PERIOD,
    EXPIRED,
    SUBSCRIBED,
    BILLING_RETRY,
    REVOKED,
    UNKNOWN,
}

object LatestSubscriptionStateSerializer : KSerializer<LatestSubscriptionState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LatestSubscriptionState", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LatestSubscriptionState,
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): LatestSubscriptionState {
        val value = decoder.decodeString()
        return LatestSubscriptionState.entries.find {
            it.name.equals(value, ignoreCase = true)
        } ?: LatestSubscriptionState.UNKNOWN
    }
}
