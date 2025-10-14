package com.superwall.sdk.models.enrichment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class EnrichmentRequest(
    @SerialName("user")
    val user: Map<String, JsonElement?>,
    @SerialName("device")
    val device: Map<String, JsonElement>,
)
