package expo.modules.superwallexpo.json


import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason

class Experiment {
  companion object {
    fun toJson(experiment: Experiment): Map<String, Any> {
      val variantMap = experiment.variant.toJson()

      return mapOf(
        "id" to experiment.id,
        "groupId" to experiment.groupId,
        "variant" to variantMap
      )
    }
  }
}
