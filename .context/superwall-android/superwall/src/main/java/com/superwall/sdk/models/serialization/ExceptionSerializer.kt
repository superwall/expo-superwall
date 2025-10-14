package com.superwall.sdk.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = Exception::class)
object ExceptionSerializer : KSerializer<Exception> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Exception", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Exception,
    ) {
        encoder.encodeString(value.message ?: "Unknown exception")
    }

    override fun deserialize(decoder: Decoder): Exception {
        val errorMessage = decoder.decodeString()
        return Exception(errorMessage)
    }
}
