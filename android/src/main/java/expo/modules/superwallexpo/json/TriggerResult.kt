package expo.modules.superwallexpo.json

import com.superwall.sdk.models.triggers.TriggerResult

class TriggerResult {
  companion object {
    fun toJson(triggerResult: TriggerResult): Map<String, Any> {
      val map = mutableMapOf<String, Any>()
      when (triggerResult) {
        is TriggerResult.PlacementNotFound -> {
          map["result"] = "placementNotFound"
        }
        is TriggerResult.NoAudienceMatch -> {
          map["result"] = "noAudienceMatch"
        }
        is TriggerResult.Paywall -> {
          map["result"] = "paywall"
          map["experiment"] = Experiment.toJson(triggerResult.experiment)
        }
        is TriggerResult.Holdout -> {
          map["result"] = "holdout"
          map["experiment"] = Experiment.toJson(triggerResult.experiment)
        }
        is TriggerResult.Error -> {
          map["result"] = "error"
          // `error` matches iOS; `errorMessage` is the old Android-only key.
          val message = triggerResult.error.localizedMessage ?: triggerResult.error.toString()
          map["error"] = message
          map["errorMessage"] = message
        }
      }
      return map
    }
  }
}
