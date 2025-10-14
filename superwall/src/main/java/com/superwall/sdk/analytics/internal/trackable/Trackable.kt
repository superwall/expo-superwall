package com.superwall.sdk.analytics.internal.trackable

interface Trackable {
    val rawName: String
    val audienceFilterParams: Map<String, Any>
    val canImplicitlyTriggerPaywall: Boolean

    suspend fun getSuperwallParameters(): Map<String, Any>
}
