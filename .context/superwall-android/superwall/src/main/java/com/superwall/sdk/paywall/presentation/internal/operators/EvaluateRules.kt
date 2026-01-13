package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.config.Assignments
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.presentation.rule_logic.RuleLogic
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating
import com.superwall.sdk.storage.LocalStorage

/**
 * Evaluates the rules from the campaign that the event belongs to.
 *
 * @param request The presentation request
 * @return A [RuleEvaluationOutcome] object containing the trigger result,
 * confirmable assignment, and unsaved occurrence.
 */
suspend fun Superwall.evaluateRules(request: PresentationRequest): Result<RuleEvaluationOutcome> =
    evaluateRules(
        assignments = dependencyContainer.assignments,
        storage = dependencyContainer.storage,
        factory = dependencyContainer,
        expressionEvaluating = dependencyContainer.provideRuleEvaluator(context),
        triggersByEventName = dependencyContainer.configManager.triggersByEventName,
        request = request,
    )

internal suspend fun evaluateRules(
    assignments: Assignments,
    storage: LocalStorage,
    factory: RuleAttributesFactory,
    expressionEvaluating: ExpressionEvaluating,
    triggersByEventName: Map<String, Trigger>,
    request: PresentationRequest,
): Result<RuleEvaluationOutcome> {
    val eventData = request.presentationInfo.eventData

    return if (eventData != null) {
        val ruleLogic =
            RuleLogic(
                assignments = assignments,
                storage = storage,
                factory = factory,
                ruleEvaluator = expressionEvaluating,
            )
        ruleLogic
            .evaluateRules(
                event = eventData,
                triggers = triggersByEventName,
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
