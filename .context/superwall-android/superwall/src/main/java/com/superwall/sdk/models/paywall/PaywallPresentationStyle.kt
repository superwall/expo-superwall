package com.superwall.sdk.models.paywall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class PaywallPresentationStyle(
    @Transient
    val rawValue: String = "",
) {
    @Serializable
    @SerialName("MODAL")
    data object Modal : PaywallPresentationStyle("MODAL")

    @Serializable
    @SerialName("FULLSCREEN")
    data object Fullscreen : PaywallPresentationStyle("FULLSCREEN")

    @Serializable
    @SerialName("NO_ANIMATION")
    data object FullscreenNoAnimation : PaywallPresentationStyle("NO_ANIMATION")

    @Serializable
    @SerialName("PUSH")
    data object Push : PaywallPresentationStyle("PUSH")

    @Serializable
    @SerialName("DRAWER")
    data class Drawer(
        @SerialName("height")
        val height: Double,
        @SerialName("corner_radius")
        val cornerRadius: Double,
    ) : PaywallPresentationStyle("DRAWER")

    @Serializable
    @SerialName("NONE")
    data object None : PaywallPresentationStyle("NONE")

    @Serializable
    @SerialName("POPUP")
    data class Popup(
        @SerialName("height")
        val height: Double,
        @SerialName("width")
        val width: Double,
        @SerialName("corner_radius")
        val cornerRadius: Double,
    ) : PaywallPresentationStyle("POPUP")

    // Helper methods for Android Intent serialization only
    fun toIntentString(json: Json): String = json.encodeToString(this)

    companion object {
        @Deprecated("Use PaywallPresentationStyle.Modal instead", ReplaceWith("PaywallPresentationStyle.Modal"))
        val MODAL = PaywallPresentationStyle.Modal

        @Deprecated("Use PaywallPresentationStyle.Fullscreen instead", ReplaceWith("PaywallPresentationStyle.Fullscreen"))
        val FULLSCREEN = PaywallPresentationStyle.Fullscreen

        @Deprecated(
            "Use PaywallPresentationStyle.FullscreenNoAnimation instead",
            ReplaceWith("PaywallPresentationStyle.FullscreenNoAnimation"),
        )
        val FULLSCREEN_NO_ANIMATION = PaywallPresentationStyle.FullscreenNoAnimation

        @Deprecated("Use PaywallPresentationStyle.Push instead", ReplaceWith("PaywallPresentationStyle.Push"))
        val PUSH = PaywallPresentationStyle.Push

        @Deprecated("Use PaywallPresentationStyle.Drawer instead", ReplaceWith("PaywallPresentationStyle.Drawer"))
        val DRAWER = PaywallPresentationStyle.Drawer

        @Deprecated("Use PaywallPresentationStyle.None instead", ReplaceWith("PaywallPresentationStyle.None"))
        val NONE = PaywallPresentationStyle.None

        fun fromIntentString(
            json: Json,
            value: String,
        ): PaywallPresentationStyle = json.decodeFromString(value)
    }
}
