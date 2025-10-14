package com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator

import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.paywall.presentation.rule_logic.cel.SuperscriptEvaluator
import com.superwall.sdk.paywall.presentation.rule_logic.tryToMatchOccurrence
import com.superwall.sdk.storage.LocalStorage

interface ExpressionEvaluating {
    suspend fun evaluateExpression(
        rule: TriggerRule,
        eventData: EventData?,
    ): TriggerRuleOutcome
}

internal class CombinedExpressionEvaluator(
    private val storage: LocalStorage,
    private val superscriptEvaluator: SuperscriptEvaluator,
) : ExpressionEvaluating {
    override suspend fun evaluateExpression(
        rule: TriggerRule,
        eventData: EventData?,
    ): TriggerRuleOutcome {
        // Expression matches all
        if (rule.expressionJs == null && rule.expression == null && rule.expressionCEL == null) {
            return rule.tryToMatchOccurrence(storage.coreDataManager, true)
        }

        // If we are evaluating JS/Liquid, we encode rules, otherwise we return null
        // and evaluate superscript only
        val celEvaluation =
            try {
                superscriptEvaluator.evaluateExpression(rule, eventData)
            } catch (e: Exception) {
                TriggerRuleOutcome.noMatch(UnmatchedRule.Source.EXPRESSION, rule.experiment.id)
            }
        return celEvaluation
    }
}
