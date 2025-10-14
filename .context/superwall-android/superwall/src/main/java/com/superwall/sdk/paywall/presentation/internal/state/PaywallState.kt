package com.superwall.sdk.paywall.presentation.internal.state

import com.superwall.sdk.paywall.presentation.PaywallInfo

sealed class PaywallResult {
    data class Purchased(
        val productId: String,
    ) : PaywallResult()

    class Declined : PaywallResult()

    class Restored : PaywallResult()
}

sealed class PaywallState {
    data class Presented(
        val paywallInfo: PaywallInfo,
    ) : PaywallState()

    data class PresentationError(
        val error: Throwable,
    ) : PaywallState()

    data class Dismissed(
        val paywallInfo: PaywallInfo,
        val paywallResult: PaywallResult,
    ) : PaywallState()

    data class Skipped(
        val paywallSkippedReason: PaywallSkippedReason,
    ) : PaywallState()
}
