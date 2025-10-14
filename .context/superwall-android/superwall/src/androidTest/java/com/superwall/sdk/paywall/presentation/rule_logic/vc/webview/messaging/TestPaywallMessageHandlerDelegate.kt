package com.superwall.sdk.paywall.presentation.rule_logic.vc.webview.messaging

import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandlerDelegate
import com.superwall.sdk.paywall.view.webview.messaging.PaywallWebEvent

class TestPaywallMessageHandlerDelegate(
    override val state: PaywallViewState,
    val onEvent: (PaywallWebEvent) -> Unit = {},
) : PaywallMessageHandlerDelegate {
    override fun updateState(update: PaywallViewState.Updates) {
        // No-op for test
    }

    override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
        onEvent(paywallWebEvent)
    }

    override fun openDeepLink(url: String) {
        TODO("Not yet implemented")
    }

    override fun presentBrowserInApp(url: String) {
        TODO("Not yet implemented")
    }

    override fun presentBrowserExternal(url: String) {
        TODO("Not yet implemented")
    }

    override fun evaluate(
        code: String,
        resultCallback: ((String?) -> Unit)?,
    ) {
        resultCallback?.invoke(null)
    }
}
