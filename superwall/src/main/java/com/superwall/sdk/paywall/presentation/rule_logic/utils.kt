package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.storage.core_data.CoreDataManager

internal suspend fun TriggerRule.tryToMatchOccurrence(
    storage: CoreDataManager,
    expressionMatched: Boolean,
): TriggerRuleOutcome {
    if (expressionMatched) {
        occurrence?.let { occurrence ->
            val count = storage.countTriggerRuleOccurrences(occurrence) + 1
            val shouldFire = count <= occurrence.maxCount

            if (shouldFire) {
                return TriggerRuleOutcome.match(this, occurrence)
            } else {
                return TriggerRuleOutcome.noMatch(
                    UnmatchedRule.Source.OCCURRENCE,
                    experiment.id,
                )
            }
        } ?: run {
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallPresentation,
                message = "No occurrence parameter found for trigger rule.",
            )
            return TriggerRuleOutcome.match(this)
        }
    }
    return TriggerRuleOutcome.noMatch(UnmatchedRule.Source.EXPRESSION, experiment.id)
}
