package com.superwall.sdk.paywall.presentation.rule_logic.javascript

import android.content.Context
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating

interface RuleEvaluator {
    fun interface Factory {
        suspend fun provideRuleEvaluator(context: Context): ExpressionEvaluating
    }
}
