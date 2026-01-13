package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.config.Assignments
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.MatchedItem
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerPreloadBehavior
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.VariantOption
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating
import com.superwall.sdk.storage.LocalStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EvaluateRulesOperatorTest {
    private val assignments = mockk<Assignments>(relaxed = true)
    private val storage = mockk<LocalStorage>(relaxed = true)
    private val factory = mockk<RuleAttributesFactory>(relaxed = true)
    private val evaluator = mockk<ExpressionEvaluating>()

    private fun rule(experimentId: String): TriggerRule =
        TriggerRule(
            experimentId = experimentId,
            experimentGroupId = "group",
            variants =
                listOf(
                    VariantOption(
                        type = Experiment.Variant.VariantType.TREATMENT,
                        id = "variant",
                        percentage = 100,
                        paywallId = "paywall",
                    ),
                ),
            preload = TriggerRule.TriggerPreload(TriggerPreloadBehavior.ALWAYS),
        )

    private fun presentationRequest(
        info: PresentationInfo,
        type: PresentationRequestType = PresentationRequestType.Presentation,
    ): PresentationRequest =
        PresentationRequest(
            presentationInfo = info,
            flags =
                PresentationRequest.Flags(
                    isDebuggerLaunched = false,
                    entitlements = MutableStateFlow(null),
                    isPaywallPresented = false,
                    type = type,
                ),
        )

    @Test
    fun eventDataReturnsRuleLogicOutcome() =
        runTest {
            Given("a request with event data and matching trigger") {
                val event = EventData.stub().copy(name = "event")
                val request = presentationRequest(PresentationInfo.ExplicitTrigger(event))
                val triggerRule = rule("exp")
                val triggers = mapOf("event" to Trigger("event", listOf(triggerRule)))
                val matchedVariant = Experiment.Variant(id = "v1", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "paywall")
                every { storage.getConfirmedAssignments() } returns mapOf("exp" to matchedVariant)
                every { assignments.unconfirmedAssignments } returns emptyMap()
                coEvery { evaluator.evaluateExpression(triggerRule, event) } returns TriggerRuleOutcome.Match(MatchedItem(triggerRule))

                val result =
                    When("evaluateRules is invoked") {
                        evaluateRules(assignments, storage, factory, evaluator, triggers, request)
                    }

                Then("the rule evaluation outcome is a paywall") {
                    assertTrue(result.isSuccess)
                    val outcome = result.getOrNull()
                    assertNotNull(outcome)
                    assertTrue(outcome is RuleEvaluationOutcome)
                    assertTrue(outcome?.triggerResult is InternalTriggerResult.Paywall)
                }
            }
        }

    @Test
    fun debuggerFallbackReturnsPaywall() =
        runTest {
            Given("a debugger request identified by paywall id") {
                val request = presentationRequest(PresentationInfo.FromIdentifier(identifier = "pw", freeTrialOverride = false))

                val result =
                    When("evaluateRules is invoked without event data") {
                        evaluateRules(assignments, storage, factory, evaluator, emptyMap(), request)
                    }

                Then("the result is a paywall outcome with matching identifier") {
                    assertTrue(result.isSuccess)
                    val outcome = result.getOrNull()
                    assertNotNull(outcome)
                    val triggerResult = outcome?.triggerResult
                    assertTrue(triggerResult is InternalTriggerResult.Paywall)
                    assertEquals("pw", (triggerResult as InternalTriggerResult.Paywall).experiment.variant.paywallId)
                }
            }
        }

    @Test
    fun missingIdentifierFails() =
        runTest {
            Given("a presentation request without event data or identifier") {
                val info = mockk<PresentationInfo>(relaxed = true)
                every { info.eventData } returns null
                every { info.identifier } returns null
                every { info.freeTrialOverride } returns null
                val request = presentationRequest(info)

                val result =
                    When("evaluateRules is invoked") {
                        evaluateRules(assignments, storage, factory, evaluator, emptyMap(), request)
                    }

                Then("the evaluation fails") {
                    assertTrue(result.isFailure)
                }
            }
        }
}
