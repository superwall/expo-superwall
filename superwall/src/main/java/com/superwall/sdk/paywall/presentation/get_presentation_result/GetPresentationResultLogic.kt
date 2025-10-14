package com.superwall.sdk.paywall.presentation.get_presentation_result

import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.paywall.presentation.result.PresentationResult

object GetPresentationResultLogic {
    fun convertTriggerResult(triggerResult: InternalTriggerResult): PresentationResult =
        when (triggerResult) {
            is InternalTriggerResult.PlacementNotFound -> PresentationResult.PlacementNotFound()
            is InternalTriggerResult.Holdout -> PresentationResult.Holdout(triggerResult.experiment)
            is InternalTriggerResult.Error -> PresentationResult.PaywallNotAvailable()
            is InternalTriggerResult.NoAudienceMatch -> PresentationResult.NoAudienceMatch()
            is InternalTriggerResult.Paywall -> PresentationResult.Paywall(triggerResult.experiment)
        }
}
