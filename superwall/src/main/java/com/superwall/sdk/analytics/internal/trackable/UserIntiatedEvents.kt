package com.superwall.sdk.analytics.internal.trackable

interface TrackableUserInitiatedEvent : Trackable

sealed class UserInitiatedEvent(
    override val rawName: String,
    override val canImplicitlyTriggerPaywall: Boolean,
    override var audienceFilterParams: Map<String, Any> = emptyMap(),
    val isFeatureGatable: Boolean,
) : TrackableUserInitiatedEvent {
    class Track(
        rawName: String,
        canImplicitlyTriggerPaywall: Boolean,
        isFeatureGatable: Boolean,
        customParameters: Map<String, Any> = emptyMap(),
    ) : UserInitiatedEvent(
            rawName,
            canImplicitlyTriggerPaywall,
            customParameters,
            isFeatureGatable,
        ) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = hashMapOf("is_feature_gatable" to isFeatureGatable)
    }
}
