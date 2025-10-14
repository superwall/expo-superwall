package com.superwall.sdk.analytics.internal

data class TrackingParameters(
    val delegateParams: Map<String, Any>,
    val audienceFilterParams: Map<String, Any>,
) {
    companion object {
        fun stub(): TrackingParameters =
            TrackingParameters(
                delegateParams = emptyMap(),
                audienceFilterParams = emptyMap(),
            )
    }
}
