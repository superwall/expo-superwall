package com.superwall.sdk.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawFeatureFlag(
    @SerialName("key")
    val key: String,
    @SerialName("enabled")
    val enabled: Boolean,
)

@Serializable
data class FeatureFlags(
    @SerialName("enable_config_refresh_v2") var enableConfigRefresh: Boolean,
    @SerialName("enable_session_events") var enableSessionEvents: Boolean,
    @SerialName("enable_postback") var enablePostback: Boolean,
    @SerialName("enable_userid_seed") var enableUserIdSeed: Boolean,
    @SerialName("disable_verbose_events") var disableVerboseEvents: Boolean,
    @SerialName("enable_multiple_paywall_urls") var enableMultiplePaywallUrls: Boolean,
    @SerialName("enable_cel_logging") var enableCELLogging: Boolean,
    @SerialName("web_2_app") var web2App: Boolean,
)

fun List<RawFeatureFlag>.value(
    key: String,
    default: Boolean,
): Boolean {
    val featureFlag = this.firstOrNull { it.key == key }
    return featureFlag?.enabled ?: default
}
