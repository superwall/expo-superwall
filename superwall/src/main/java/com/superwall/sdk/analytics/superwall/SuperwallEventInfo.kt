package com.superwall.sdk.analytics.superwall

@Deprecated("Use SuperwallEventInfo instead")
typealias SuperwallPlacementInfo = SuperwallEventInfo

data class SuperwallEventInfo(
    public val event: SuperwallEvent,
    public val params: Map<String, Any>,
) {
    @Deprecated("Use event instead")
    val placement: SuperwallEvent
        get() = event
}
