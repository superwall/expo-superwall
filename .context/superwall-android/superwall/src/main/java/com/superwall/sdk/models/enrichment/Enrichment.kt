package com.superwall.sdk.models.enrichment

import com.superwall.sdk.storage.core_data.convertToJsonElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Enrichment(
    @SerialName("user")
    val user: JsonObject,
    @SerialName("device")
    val device: JsonObject,
) {
    companion object {
        fun stub() =
            Enrichment(
                JsonObject(emptyMap()),
                JsonObject(mapOf("demandTier" to "silver".convertToJsonElement())),
            )
    }
}
