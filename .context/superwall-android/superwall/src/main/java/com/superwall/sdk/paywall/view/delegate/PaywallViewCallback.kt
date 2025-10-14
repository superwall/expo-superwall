package com.superwall.sdk.paywall.view.delegate

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.webview.messaging.PaywallWebEvent

interface PaywallViewCallback {
    fun onFinished(
        paywall: PaywallView,
        result: PaywallResult,
        shouldDismiss: Boolean,
    )
}

fun interface PaywallViewEventCallback {
    suspend fun eventDidOccur(
        paywallEvent: PaywallWebEvent,
        paywallView: PaywallView,
    )
}

sealed class PaywallLoadingState {
    object Unknown : PaywallLoadingState()

    object LoadingPurchase : PaywallLoadingState()

    object LoadingURL : PaywallLoadingState()

    object ManualLoading : PaywallLoadingState()

    object Ready : PaywallLoadingState()
}
