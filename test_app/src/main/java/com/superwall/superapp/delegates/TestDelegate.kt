package com.superwall.superapp.delegates

import android.net.Uri
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.PaywallInfo
import java.net.URI

sealed class TestDelegateEvent {
    object WillPresentPaywall : TestDelegateEvent()

    object DidPresentPaywall : TestDelegateEvent()

    object WillDismissPaywall : TestDelegateEvent()

    object DidDismissPaywall : TestDelegateEvent()

    object SubscriptionStatusDidChange : TestDelegateEvent()

    object PaywallWillOpenDeepLink : TestDelegateEvent()

    object PaywallWillOpenURL : TestDelegateEvent()

    object HandleCustomPaywallAction : TestDelegateEvent()

    object HandleLog : TestDelegateEvent()
}

class TestDelegate : SuperwallDelegate {
    private val _events = mutableListOf<TestDelegateEvent>()
    val events: List<TestDelegateEvent> = _events

    // Filter out log events
    val eventsWithoutLog: List<TestDelegateEvent>
        get() = _events.filter { it !is TestDelegateEvent.HandleLog }

    fun clearEvents() {
        _events.clear()
    }

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        // Handle general Superwall events if needed
    }

    override fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
        _events.add(TestDelegateEvent.SubscriptionStatusDidChange)
    }

    override fun willPresentPaywall(withInfo: PaywallInfo) {
        _events.add(TestDelegateEvent.WillPresentPaywall)
    }

    override fun didPresentPaywall(withInfo: PaywallInfo) {
        _events.add(TestDelegateEvent.DidPresentPaywall)
    }

    override fun willDismissPaywall(withInfo: PaywallInfo) {
        _events.add(TestDelegateEvent.WillDismissPaywall)
    }

    override fun didDismissPaywall(withInfo: PaywallInfo) {
        _events.add(TestDelegateEvent.DidDismissPaywall)
    }

    override fun paywallWillOpenURL(url: URI) {
        _events.add(TestDelegateEvent.PaywallWillOpenURL)
    }

    override fun paywallWillOpenDeepLink(url: Uri) {
        _events.add(TestDelegateEvent.PaywallWillOpenDeepLink)
    }

    override fun handleCustomPaywallAction(withName: String) {
        _events.add(TestDelegateEvent.HandleCustomPaywallAction)
    }

    override fun handleLog(
        level: String,
        scope: String,
        message: String?,
        info: Map<String, Any>?,
        error: Throwable?,
    ) {
        _events.add(TestDelegateEvent.HandleLog)
    }
}
