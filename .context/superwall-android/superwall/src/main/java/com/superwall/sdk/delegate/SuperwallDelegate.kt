package com.superwall.sdk.delegate

import android.net.Uri
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.paywall.presentation.PaywallInfo
import java.net.URI

interface SuperwallDelegate {
    fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
    }

    fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {}

    fun handleCustomPaywallAction(withName: String) {}

    fun willDismissPaywall(withInfo: PaywallInfo) {}

    fun willPresentPaywall(withInfo: PaywallInfo) {}

    fun didDismissPaywall(withInfo: PaywallInfo) {}

    fun didPresentPaywall(withInfo: PaywallInfo) {}

    fun paywallWillOpenURL(url: URI) {}

    fun paywallWillOpenDeepLink(url: Uri) {}

    fun handleLog(
        level: String,
        scope: String,
        message: String?,
        info: Map<String, Any>?,
        error: Throwable?,
    ) {
    }

    fun willRedeemLink() {}

    fun didRedeemLink(result: RedemptionResult) {}

    fun userAttributesDidChange(newAttributes: Map<String, Any>) {}

    fun customerInfoDidChange(
        from: CustomerInfo,
        to: CustomerInfo,
    ) {}
}
