package com.superwall.sdk.paywall.presentation.internal.operators

import android.app.Activity
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.paywall.presentation.internal.InternalPresentationLogic
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.view.PaywallView
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Checks conditions for whether the paywall can present before accessing a window on
 * which the paywall can present.
 *
 * @param paywallView The [PaywallView] to present.
 * @param rulesOutcome The output from evaluating rules.
 * @param request The presentation request.
 * @param paywallStatePublisher A [MutableSharedFlow] that gets sent [PaywallState] objects.
 *
 * @return An [Activity] to present on, or null if presentation is not necessary.
 */
internal suspend fun Superwall.getPresenterIfNecessary(
    paywallView: PaywallView,
    rulesOutcome: RuleEvaluationOutcome,
    request: PresentationRequest,
    paywallStatePublisher: MutableSharedFlow<PaywallState>? = null,
): Activity? {
    when (request.flags.type) {
        is PresentationRequestType.GetPaywall -> {
            val sessionId =
                attemptTriggerFire(
                    request = request,
                    triggerResult = rulesOutcome.triggerResult,
                )
            return null
        }

        is PresentationRequestType.GetImplicitPresentationResult,
        is PresentationRequestType.GetPresentationResult,
        is PresentationRequestType.ConfirmAllAssignments,
        -> return null

        is PresentationRequestType.Presentation -> Unit
        else -> Unit
    }

    val sessionId =
        attemptTriggerFire(
            request = request,
            triggerResult = rulesOutcome.triggerResult,
        )

    val currentActivity = dependencyContainer.activityProvider?.getCurrentActivity()

    if (currentActivity == null) {
        Logger.debug(
            logLevel = LogLevel.error,
            scope = LogScope.paywallPresentation,
            message = "Current Activity is null, can't present paywall",
        )
        val error =
            InternalPresentationLogic.presentationError(
                domain = "SWPresentationError",
                code = 103,
                title = "No Activity to present paywall on",
                value = "This usually happens when you call this method before a window was made key and visible.",
            )
        val state = PaywallState.PresentationError(error)
        paywallStatePublisher?.emit(state)
        throw PaywallPresentationRequestStatusReason.NoPresenter()
    }
    return currentActivity
}

internal suspend fun Superwall.attemptTriggerFire(
    request: PresentationRequest,
    triggerResult: InternalTriggerResult,
) {
    // If eventName is null, the paywall is being presented by identifier, which is what the debugger uses and that's not supported.
    val eventName = request.presentationInfo.eventName ?: return

    when (val req = request.presentationInfo) {
        is PresentationInfo.ExplicitTrigger, is PresentationInfo.ImplicitTrigger -> {
            when (triggerResult) {
                is InternalTriggerResult.Error, is InternalTriggerResult.PlacementNotFound ->
                    return

                else -> {} // No-op
            }
        }

        is PresentationInfo.FromIdentifier -> {} // No-op
    }
    val trackedEvent =
        InternalSuperwallEvent.TriggerFire(
            triggerResult = triggerResult,
            triggerName = eventName,
        )
    track(trackedEvent)
}
