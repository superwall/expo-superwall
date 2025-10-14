package com.superwall.sdk.paywall.presentation.internal

import android.app.Activity
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

sealed class PresentationRequestType {
    object Presentation : PresentationRequestType()

    object ConfirmAllAssignments : PresentationRequestType()

    data class GetPaywall(
        val adapter: PaywallViewDelegateAdapter,
    ) : PresentationRequestType()

    object GetPresentationResult : PresentationRequestType()

    object GetImplicitPresentationResult : PresentationRequestType()

    val description: String
        get() =
            when (this) {
                is Presentation -> "presentation"
                is GetPaywall -> "getPaywallViewController"
                is GetPresentationResult -> "getPresentationResult"
                is GetImplicitPresentationResult -> "getImplicitPresentationResult"
                is ConfirmAllAssignments -> "confirmAllAssignments"
                else -> "Unknown"
            }

    val paywallViewDelegateAdapter: PaywallViewDelegateAdapter?
        get() = if (this is GetPaywall) this.adapter else null

    companion object {
        fun areEqual(
            lhs: PresentationRequestType,
            rhs: PresentationRequestType,
        ): Boolean =
            when {
                lhs is GetPaywall && rhs is GetPaywall -> lhs.adapter == rhs.adapter
                else -> lhs == rhs
            }
    }
}

data class PresentationRequest(
    val presentationInfo: PresentationInfo,
    var presenter: WeakReference<Activity>? = null,
    var paywallOverrides: PaywallOverrides? = null,
    var flags: Flags,
) {
    data class Flags(
        var isDebuggerLaunched: Boolean,
        var entitlements: StateFlow<SubscriptionStatus?>,
        var isPaywallPresented: Boolean,
        var type: PresentationRequestType,
    )

    /**
     * The source function type that initiated the presentation request.
     */
    val presentationSourceType: String?
        get() =
            when (presentationInfo) {
                is PresentationInfo.ImplicitTrigger -> "implicit"
                is PresentationInfo.ExplicitTrigger, is PresentationInfo.FromIdentifier ->
                    when (flags.type) {
                        is PresentationRequestType.GetPaywall -> "getPaywall"
                        is PresentationRequestType.Presentation -> "register"
                        is PresentationRequestType.GetPresentationResult -> null
                        is PresentationRequestType.GetImplicitPresentationResult -> null
                        is PresentationRequestType.ConfirmAllAssignments -> null
                        else -> null
                    }
            }
}
