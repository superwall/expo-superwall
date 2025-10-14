package com.superwall.sdk.paywall.presentation.internal.state

sealed class PaywallErrors(
    val msg: String?,
) : Throwable(msg) {
    class Timeout(
        message: String?,
    ) : PaywallErrors(message)
}
