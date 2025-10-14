@file:Suppress("ktlint:standard:value-parameter-comment")

package com.superwall.sdk.models.paywall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaywallPresentationInfo(
    // The presentation style of the paywall
    @SerialName("style")
    val style: PaywallPresentationStyle,
    // The delay in milliseconds before switching from the loading view to
    // the paywall view.
    @SerialName("delay")
    val delay: Long,
)
