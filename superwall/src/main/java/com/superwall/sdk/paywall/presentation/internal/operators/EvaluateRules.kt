package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.TriggerResult
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.presentation.rule_logic.RuleLogic

// Defining the data class equivalent of the struct in Swift
data class AssignmentPipelineOutput(
    val triggerResult: TriggerResult,
    var confirmableAssignment: ConfirmableAssignment?,
    val debugInfo: Map<String, Any>,
)

/**
 * Evaluates the rules from the campaign that the event belongs to.
 *
 * @param request The presentation request
 * @return A [RuleEvaluationOutcome] object containing the trigger result,
 * confirmable assignment, and unsaved occurrence.
 */
suspend fun Superwall.evaluateRules(request: PresentationRequest): Result<RuleEvaluationOutcome> {
    val eventData = request.presentationInfo.eventData

    return if (eventData != null) {
        val ruleLogic =
            RuleLogic(
                assignments = dependencyContainer.assignments,
                storage = dependencyContainer.storage,
                factory = dependencyContainer,
                ruleEvaluator = dependencyContainer.provideRuleEvaluator(context),
            )
        ruleLogic
            .evaluateRules(
                event = eventData,
                triggers = dependencyContainer.configManager.triggersByEventName,
            ).toResult()
    } else {
        // Called if the debugger is shown.
        val paywallId =
            request.presentationInfo.identifier
                ?: return Result.failure(
                    PaywallPresentationRequestStatusReason.NoPaywallView(),
                )

        Result.success(
            RuleEvaluationOutcome(
                triggerResult =
                    InternalTriggerResult.Paywall(
                        Experiment.presentById(paywallId),
                    ),
            ),
        )
    }
}
