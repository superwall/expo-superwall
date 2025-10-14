package com.superwall.sdk.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreloadingDisabled(
    @SerialName("all")
    val all: Boolean,
    @SerialName("triggers")
    val triggers: Set<String>,
) {
    companion object {
        fun stub() =
            PreloadingDisabled(
                all = false,
                triggers = emptySet(),
            )
    }
}
