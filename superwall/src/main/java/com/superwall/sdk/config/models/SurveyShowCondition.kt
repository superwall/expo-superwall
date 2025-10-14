package com.superwall.sdk.config.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.util.Base64.Decoder
import java.util.Base64.Encoder

@Serializable(with = SurveyShowConditionSerializer::class)
enum class SurveyShowCondition(
    val rawValue: String,
) {
    @SerialName("ON_MANUAL_CLOSE")
    ON_MANUAL_CLOSE("ON_MANUAL_CLOSE"),

    @SerialName("ON_PURCHASE")
    ON_PURCHASE("ON_PURCHASE"),
}

@Serializer(forClass = SurveyShowCondition::class)
object SurveyShowConditionSerializer : KSerializer<SurveyShowCondition> {
    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: SurveyShowCondition,
    ) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): SurveyShowCondition {
        val rawValue = decoder.decodeString()
        return SurveyShowCondition.values().find { it.rawValue == rawValue }
            ?: throw IllegalArgumentException("Unsupported survey condition.")
    }
}
