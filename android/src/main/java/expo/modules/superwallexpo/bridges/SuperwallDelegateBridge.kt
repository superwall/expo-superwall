package expo.modules.superwallexpo.bridges

import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.internal.RedemptionResult
import java.net.URI
import android.net.Uri
import expo.modules.superwallexpo.json.*
import expo.modules.superwallexpo.SuperwallExpoModule
class SuperwallDelegateBridge : SuperwallDelegate {
  override fun subscriptionStatusDidChange(
    oldValue: SubscriptionStatus,
    newValue: SubscriptionStatus
  ) {
    val data = mapOf<String, Any>(
      "from" to oldValue.toJson(),
      "to" to newValue.toJson()
    )
    sendEvent("subscriptionStatusDidChange", data)
  }

  override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
    val data = mapOf<String, Any?>(
      "eventInfo" to eventInfo.toJson()
    )
    sendEvent("handleSuperwallEvent", data)
  }

  override fun handleCustomPaywallAction(name: String) {
    val data = mapOf<String, Any?>(
      "name" to name
    )
    sendEvent("handleCustomPaywallAction", data)
  }

  override fun willDismissPaywall(withInfo: PaywallInfo) {
    val data = mapOf<String, Any?>(
      "info" to withInfo.toJson()
    )
    sendEvent("willDismissPaywall", data)
  }

  override fun willPresentPaywall(withInfo: PaywallInfo) {
    val data = mapOf<String, Any?>(
      "info" to withInfo.toJson()
    )
    sendEvent("willPresentPaywall", data)
  }

  override fun didDismissPaywall(withInfo: PaywallInfo) {
    val data = mapOf<String, Any?>(
      "info" to withInfo.toJson()
    )
    sendEvent("didDismissPaywall", data)
  }

  override fun didPresentPaywall(withInfo: PaywallInfo) {
    val data = mapOf<String, Any?>(
      "info" to withInfo.toJson()
    )
    sendEvent("didPresentPaywall", data)
  }

  override fun paywallWillOpenURL(url: URI) {
    val data = mapOf<String, Any?>(
      "url" to url.toString()
    )
    sendEvent("paywallWillOpenURL", data)
  }

  override fun paywallWillOpenDeepLink(url: Uri) {
    val data = mapOf<String, Any?>(
      "url" to url.toString()
    )
    sendEvent("paywallWillOpenDeepLink", data)
  }

  override fun handleLog(
    level: String,
    scope: String,
    message: String?,
    info: Map<String, Any>?,
    error: Throwable?
  ) {
    val data = mapOf<String, Any?>(
      "level" to level,
      "scope" to scope,
      "message" to message,
      "info" to info,
      "error" to error?.localizedMessage
    )
    sendEvent("handleLog", data)
  }

  private fun sendEvent(name: String, body: Map<String, Any?>) {
    SuperwallExpoModule.instance?.emitEvent(name, body)
  }

  override fun willRedeemLink() {
    sendEvent("willRedeemLink", emptyMap())
  }

  override fun didRedeemLink(result: RedemptionResult) {
    sendEvent("didRedeemLink", result.toJson())
  }
}
