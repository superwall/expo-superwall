package com.superwall.sdk.models.entitlements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Entitlement(
    @SerialName("identifier")
    val id: String,
    @SerialName("type")
    val type: Type = Type.SERVICE_LEVEL,
) {
    constructor(id: String) : this(id, type = Type.SERVICE_LEVEL)

    @Serializable
    enum class Type(
        val raw: String,
    ) {
        @SerialName("SERVICE_LEVEL")
        SERVICE_LEVEL("SERVICE_LEVEL"),
    }
}
