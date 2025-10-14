package com.superwall.sdk.paywall.presentation.get_presentation_result

import com.superwall.sdk.Superwall
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationPipelineError
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.getPaywallComponents
import com.superwall.sdk.paywall.presentation.result.PresentationResult

internal suspend fun Superwall.getPresentationResult(request: PresentationRequest): PresentationResult =
    getPaywallComponents(request)
        .map {
            GetPresentationResultLogic.convertTriggerResult(it.rulesOutcome.triggerResult)
        }.fold(
            onSuccess = {
                return@fold it
            },
            onFailure = {
                if (it is PresentationPipelineError) {
                    return@fold handle(it, request.flags.type)
                } else {
                    throw it
                }
            },
        )

private fun handle(
    error: PresentationPipelineError,
    requestType: PresentationRequestType,
): PresentationResult {
    if (requestType != PresentationRequestType.GetImplicitPresentationResult) {
        Logger.debug(
            logLevel = LogLevel.info,
            scope = LogScope.paywallPresentation,
            message = "Paywall presentation error: $error",
        )
    }

    return when (error) {
        is PaywallPresentationRequestStatusReason.NoPaywallView -> PresentationResult.PaywallNotAvailable()
        is PaywallPresentationRequestStatusReason.NoAudienceMatch -> PresentationResult.NoAudienceMatch()
        is PaywallPresentationRequestStatusReason.Holdout -> PresentationResult.Holdout(error.experiment)
        is PaywallPresentationRequestStatusReason.PlacementNotFound -> PresentationResult.PlacementNotFound()
        is PaywallPresentationRequestStatusReason.DebuggerPresented,
        is PaywallPresentationRequestStatusReason.NoPresenter,
        is PaywallPresentationRequestStatusReason.PaywallAlreadyPresented,
        is PaywallPresentationRequestStatusReason.NoConfig,
        is PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout,
        -> PresentationResult.PaywallNotAvailable()
    }
}
