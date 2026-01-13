package com.superwall.sdk.delegate

import android.net.Uri
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.paywall.presentation.PaywallInfo
import java.net.URI

interface SuperwallDelegateJava {
    fun handleCustomPaywallAction(name: String) {}

    fun willRedeemLink() {}

    fun didRedeemLink(result: RedemptionResult) {}

    fun willDismissPaywall(paywallInfo: PaywallInfo) {}

    fun willPresentPaywall(paywallInfo: PaywallInfo) {}

    fun didDismissPaywall(paywallInfo: PaywallInfo) {}

    fun didPresentPaywall(paywallInfo: PaywallInfo) {}

    fun paywallWillOpenURL(url: URI) {}

    fun paywallWillOpenDeepLink(url: Uri) {}

    fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {}

    fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
    }

    fun handleLog(
        level: String,
        scope: String,
        message: String? = null,
        info: Map<String, Any>? = null,
        error: Throwable? = null,
    ) {
    }

    fun userAttributesDidChange(newAttributes: Map<String, Any>) {}

    fun customerInfoDidChange(
        from: CustomerInfo,
        to: CustomerInfo,
    ) {}
}
