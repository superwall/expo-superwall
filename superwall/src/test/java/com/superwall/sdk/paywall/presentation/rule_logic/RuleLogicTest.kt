package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.config.Assignments
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.MatchedItem
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerPreloadBehavior
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.models.triggers.VariantOption
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating
import com.superwall.sdk.storage.LocalStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleLogicTest {
    private val assignments = mockk<Assignments>(relaxed = true)
    private val storage = mockk<LocalStorage>(relaxed = true)
    private val factory = mockk<RuleAttributesFactory>(relaxed = true)
    private val evaluator = mockk<ExpressionEvaluating>()

    private fun rule(
        experimentId: String,
        experimentGroupId: String = "group",
    ): TriggerRule =
        TriggerRule(
            experimentId = experimentId,
            experimentGroupId = experimentGroupId,
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

    private fun trigger(rule: TriggerRule): Trigger = Trigger(eventName = "event", rules = listOf(rule))

    @Test
    fun returnsPlacementNotFoundWhenTriggerMissing() =
        runTest {
            Given("an event without configured triggers") {
                val logic = RuleLogic(assignments, storage, factory, evaluator)
                val event = EventData.stub().copy(name = "event")

                val result =
                    When("evaluateRules is called") {
                        logic.evaluateRules(event, emptyMap())
                    }

                Then("placement-not-found is returned") {
                    assertTrue(result is Either.Success<RuleEvaluationOutcome, Throwable>)
                    val outcome = (result as Either.Success<RuleEvaluationOutcome, Throwable>).value
                    assertEquals(InternalTriggerResult.PlacementNotFound, outcome.triggerResult)
                }
            }
        }

    @Test
    fun returnsNoAudienceMatchWhenNoRuleMatches() =
        runTest {
            Given("an event whose triggers fail expression evaluation") {
                val rule = rule("exp")
                val triggers = mapOf("event" to trigger(rule))
                val event = EventData.stub().copy(name = "event")
                coEvery { evaluator.evaluateExpression(rule, event) } returns
                    TriggerRuleOutcome.NoMatch(UnmatchedRule(UnmatchedRule.Source.EXPRESSION, "exp"))

                val result =
                    When("evaluateRules executes") {
                        RuleLogic(assignments, storage, factory, evaluator).evaluateRules(event, triggers)
                    }

                Then("no-audience-match is produced") {
                    assertTrue(result is Either.Success<RuleEvaluationOutcome, Throwable>)
                    val outcome = (result as Either.Success<RuleEvaluationOutcome, Throwable>).value
                    assertTrue(outcome.triggerResult is InternalTriggerResult.NoAudienceMatch)
                    val unmatched = (outcome.triggerResult as InternalTriggerResult.NoAudienceMatch).unmatchedRules
                    assertEquals(1, unmatched.size)
                }
            }
        }

    @Test
    fun returnsErrorWhenVariantMissing() =
        runTest {
            Given("a trigger without a stored assignment") {
                val rule = rule("missing")
                val triggers = mapOf("event" to trigger(rule))
                val event = EventData.stub().copy(name = "event")
                coEvery { evaluator.evaluateExpression(rule, event) } returns TriggerRuleOutcome.Match(MatchedItem(rule))
                every { storage.getConfirmedAssignments() } returns emptyMap()
                every { assignments.unconfirmedAssignments } returns emptyMap()

                val result =
                    When("evaluateRules resolves the rule") {
                        RuleLogic(assignments, storage, factory, evaluator).evaluateRules(event, triggers)
                    }

                Then("an error outcome is returned") {
                    assertTrue(result is Either.Success<RuleEvaluationOutcome, Throwable>)
                    val outcome = (result as Either.Success<RuleEvaluationOutcome, Throwable>).value
                    val triggerResult = outcome.triggerResult
                    assertTrue(triggerResult is InternalTriggerResult.Error)
                    assertTrue((triggerResult as InternalTriggerResult.Error).error is PaywallNotFoundException)
                }
            }
        }

    @Test
    fun returnsHoldoutOutcome() =
        runTest {
            Given("a holdout assignment") {
                val experimentId = "holdout"
                val rule = rule(experimentId)
                val triggers = mapOf("event" to trigger(rule))
                val event = EventData.stub().copy(name = "event")
                val matchedOccurrence = TriggerRuleOccurrence.stub()
                coEvery { evaluator.evaluateExpression(rule, event) } returns
                    TriggerRuleOutcome.Match(MatchedItem(rule, matchedOccurrence))
                every { storage.getConfirmedAssignments() } returns emptyMap()
                val holdoutVariant = Experiment.Variant(id = "h1", type = Experiment.Variant.VariantType.HOLDOUT, paywallId = null)
                every { assignments.unconfirmedAssignments } returns mapOf(experimentId to holdoutVariant)

                val result =
                    When("evaluateRules processes the trigger") {
                        RuleLogic(assignments, storage, factory, evaluator).evaluateRules(event, triggers)
                    }

                Then("a holdout outcome is emitted with confirmable assignment") {
                    assertTrue(result is Either.Success<RuleEvaluationOutcome, Throwable>)
                    val outcome = (result as Either.Success<RuleEvaluationOutcome, Throwable>).value
                    val triggerResult = outcome.triggerResult
                    assertTrue(triggerResult is InternalTriggerResult.Holdout)
                    assertEquals(holdoutVariant, (triggerResult as InternalTriggerResult.Holdout).experiment.variant)
                    assertNotNull(outcome.confirmableAssignment)
                    assertEquals(matchedOccurrence, outcome.unsavedOccurrence)
                }
            }
        }

    @Test
    fun returnsPaywallOutcomeWithConfirmedAssignment() =
        runTest {
            Given("a confirmed treatment assignment") {
                val experimentId = "treat"
                val rule = rule(experimentId)
                val triggers = mapOf("event" to trigger(rule))
                val event = EventData.stub().copy(name = "event")
                coEvery { evaluator.evaluateExpression(rule, event) } returns TriggerRuleOutcome.Match(MatchedItem(rule))
                val treatmentVariant = Experiment.Variant(id = "t1", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "paywall")
                every { storage.getConfirmedAssignments() } returns mapOf(experimentId to treatmentVariant)
                every { assignments.unconfirmedAssignments } returns emptyMap()

                val result =
                    When("evaluateRules executes") {
                        RuleLogic(assignments, storage, factory, evaluator).evaluateRules(event, triggers)
                    }

                Then("a paywall outcome is returned without confirmable assignment") {
                    assertTrue(result is Either.Success<RuleEvaluationOutcome, Throwable>)
                    val outcome = (result as Either.Success<RuleEvaluationOutcome, Throwable>).value
                    val triggerResult = outcome.triggerResult
                    assertTrue(triggerResult is InternalTriggerResult.Paywall)
                    assertEquals(treatmentVariant, (triggerResult as InternalTriggerResult.Paywall).experiment.variant)
                    assertNull(outcome.confirmableAssignment)
                }
            }
        }
}
