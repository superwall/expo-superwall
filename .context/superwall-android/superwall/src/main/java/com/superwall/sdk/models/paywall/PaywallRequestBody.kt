package com.superwall.sdk.models.paywall

import kotlinx.serialization.Serializable

@Serializable
data class PaywallRequestBody(
    var appUserId: String,
)
