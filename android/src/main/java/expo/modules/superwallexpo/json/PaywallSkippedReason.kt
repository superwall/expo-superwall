package expo.modules.superwallexpo.json

import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason

  fun PaywallSkippedReason.toJson(): Map<String, Any> {
      val map = mutableMapOf<String, Any>()

      when (this) {
        is PaywallSkippedReason.Holdout -> {
          map["type"] = "Holdout"
          map["experiment"] = Experiment.toJson(experiment)
        }

        is PaywallSkippedReason.NoAudienceMatch -> {
          map["type"] = "NoAudienceMatch"
        }

        is PaywallSkippedReason.PlacementNotFound -> {
          map["type"] = "PlacementNotFound"
        }

        is PaywallSkippedReason.UserIsSubscribed -> {
          map["type"] = "UserIsSubscribed"
        }
      }

  return map
}
