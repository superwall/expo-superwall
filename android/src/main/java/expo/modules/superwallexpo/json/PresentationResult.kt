package expo.modules.superwallexpo.json


import com.superwall.sdk.paywall.presentation.result.PresentationResult

fun PresentationResult.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()

  when (this) {
    is PresentationResult.Holdout -> {
      map["type"] = "Holdout"
      map["experiment"] = Experiment.toJson(experiment)
    }

    is PresentationResult.Paywall -> {
      map["type"] = "Paywall"
      map["experiment"] = Experiment.toJson(experiment)
    }

    is PresentationResult.NoAudienceMatch -> {
      map["type"] = "NoAudienceMatch"
    }

    is PresentationResult.PlacementNotFound -> {
      map["type"] = "PlacementNotFound"
    }

    is PresentationResult.PaywallNotAvailable -> {
      map["type"] = "PaywallNotAvailable"
    }
  }

  return map
}
