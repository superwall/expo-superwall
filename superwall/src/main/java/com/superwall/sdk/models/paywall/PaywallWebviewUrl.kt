package com.superwall.sdk.models.paywall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaywallWebviewUrl(
    @SerialName("url")
    val url: String,
    @SerialName("timeout_ms")
    val timeout: Long,
    @SerialName("percentage")
    val score: Int,
) {
    @Serializable
    data class Config(
        @SerialName("max_attempts")
        val maxAttempts: Int,
        @SerialName("endpoints")
        val endpoints: List<PaywallWebviewUrl>,
    )
}
