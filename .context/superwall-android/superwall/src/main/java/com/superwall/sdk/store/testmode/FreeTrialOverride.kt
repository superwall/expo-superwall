package com.superwall.sdk.store.testmode

import kotlinx.serialization.Serializable

@Serializable
enum class FreeTrialOverride(
    val displayName: String,
) {
    UseDefault("Use Default"),
    ForceAvailable("Force Available"),
    ForceUnavailable("Force Unavailable"),
}
