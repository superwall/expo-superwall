package expo.modules.superwallexpo.json

import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason

fun PaywallPresentationRequestStatus.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  val statusValue = when (this) {
    PaywallPresentationRequestStatus.Presentation -> "presentation"
    PaywallPresentationRequestStatus.NoPresentation -> "noPresentation"
    PaywallPresentationRequestStatus.Timeout -> "timeout"
  }
  map["status"] = statusValue
  return map
}

fun PaywallPresentationRequestStatusReason.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  when (this) {
    is PaywallPresentationRequestStatusReason.DebuggerPresented -> map["reason"] = "debuggerPresented"
    is PaywallPresentationRequestStatusReason.PaywallAlreadyPresented -> map["reason"] = "paywallAlreadyPresented"
    is PaywallPresentationRequestStatusReason.Holdout -> {
      map["reason"] = "holdout"
      map["experiment"] = Experiment.toJson(this.experiment)
    }
    is PaywallPresentationRequestStatusReason.NoAudienceMatch -> map["reason"] = "noAudienceMatch"
    is PaywallPresentationRequestStatusReason.PlacementNotFound -> map["reason"] = "placementNotFound"
    is PaywallPresentationRequestStatusReason.NoPaywallView -> map["reason"] = "noPaywallViewController"
    is PaywallPresentationRequestStatusReason.NoPresenter -> map["reason"] = "noPresenter"
    is PaywallPresentationRequestStatusReason.NoConfig -> map["reason"] = "noConfig"
    is PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout -> map["reason"] = "subscriptionStatusTimeout"
  }
  return map
}
