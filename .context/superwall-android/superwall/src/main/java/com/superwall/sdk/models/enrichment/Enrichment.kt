package com.superwall.sdk.models.enrichment

import com.superwall.sdk.storage.core_data.convertFromJsonElement
import com.superwall.sdk.storage.core_data.convertToJsonElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

@Serializable
data class Enrichment(
    @SerialName("user")
    private val _user: JsonObject,
    @SerialName("device")
    private val _device: JsonObject,
) {
    companion object {
        fun stub() =
            Enrichment(
                JsonObject(emptyMap()),
                JsonObject(mapOf("demandTier" to "silver".convertToJsonElement())),
            )
    }

    val user: Map<String, Any>
        get() =
            _user
                .filterValues { it !is JsonNull }
                .mapNotNull { (key, value) -> value.convertFromJsonElement()?.let { key to it } }
                .toMap()

    val device: Map<String, Any>
        get() =
            _device
                .filterValues { it !is JsonNull }
                .mapNotNull { (key, value) -> value.convertFromJsonElement()?.let { key to it } }
                .toMap()
}
