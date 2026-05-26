package com.superwall.sdk.identity

import com.superwall.sdk.SdkContext
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.misc.primitives.BaseContext
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.web.WebPaywallRedeemer

/**
 * All dependencies available to identity [IdentityState.Actions].
 *
 * Cross-slice config/test-mode/assignments dispatch goes through [sdkContext].
 */
interface IdentityContext : BaseContext<IdentityState, IdentityContext> {
    val sdkContext: SdkContext
    val webPaywallRedeemer: () -> WebPaywallRedeemer
    val completeReset: () -> Unit
    val notifyUserChange: ((Map<String, Any>) -> Unit)?
}
