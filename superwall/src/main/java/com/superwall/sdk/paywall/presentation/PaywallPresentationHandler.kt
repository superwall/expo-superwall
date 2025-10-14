package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason

class PaywallPresentationHandler {
    // A block called when the paywall did present
    internal var onPresentHandler: ((PaywallInfo) -> Unit)? = null

    // A block called when the paywall did dismiss
    internal var onDismissHandler: ((PaywallInfo, PaywallResult) -> Unit)? = null

    // A block called when an error occurred while trying to present a paywall
    internal var onErrorHandler: ((Throwable) -> Unit)? = null

    // A block called when a paywall is skipped, but no error has occurred
    internal var onSkipHandler: ((PaywallSkippedReason) -> Unit)? = null

    // Sets the handler that will be called when the paywall did present
    fun onPresent(handler: (PaywallInfo) -> Unit) {
        onPresentHandler = handler
    }

    // Sets the handler that will be called when the paywall did dismiss
    fun onDismiss(handler: (PaywallInfo, PaywallResult) -> Unit) {
        onDismissHandler = handler
    }

    // Sets the handler that will be called when an error occurred while trying to present a paywall
    fun onError(handler: (Throwable) -> Unit) {
        onErrorHandler = handler
    }

    // Sets the handler that will be called when a paywall is skipped, but no error has occurred
    fun onSkip(handler: (PaywallSkippedReason) -> Unit) {
        onSkipHandler = handler
    }
}
