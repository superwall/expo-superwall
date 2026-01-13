package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.core_data.CoreDataManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetExperimentOperatorTest {
    private val storage = mockk<LocalStorage>(relaxed = true)
    private val coreDataManager = mockk<CoreDataManager>(relaxed = true)

    init {
        every { storage.coreDataManager } returns coreDataManager
    }

    private fun presentationRequest(type: PresentationRequestType = PresentationRequestType.Presentation): PresentationRequest =
        PresentationRequest(
            presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
            flags =
                PresentationRequest.Flags(
                    isDebuggerLaunched = false,
                    entitlements = MutableStateFlow(null),
                    isPaywallPresented = false,
                    type = type,
                ),
        )

    private val experiment =
        Experiment(
            id = "exp",
            groupId = "group",
            variant = Experiment.Variant(id = "variant", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "paywall"),
        )

    @Test
    fun paywallResultReturnsExperiment() =
        runTest {
            Given("a paywall trigger result") {
                val request = presentationRequest()
                val outcome = RuleEvaluationOutcome(triggerResult = InternalTriggerResult.Paywall(experiment))

                val result =
                    When("getExperiment is called") {
                        getExperiment(
                            request = request,
                            rulesOutcome = outcome,
                            debugInfo = emptyMap(),
                            paywallStatePublisher = null,
                            storage = storage,
                            activateSession = { _, _ -> error("should not activate") },
                        )
                    }

                Then("the resolved experiment is returned") {
                    assertEquals(experiment, result)
                }
            }
        }

    @Test
    fun holdoutPublishesSkippedStateAndThrows() =
        runTest {
            Given("a holdout trigger result") {
                val request = presentationRequest()
                val states = MutableSharedFlow<PaywallState>(replay = 1)
                var activated = false
                val holdoutExperiment =
                    experiment.copy(
                        variant = experiment.variant.copy(type = Experiment.Variant.VariantType.HOLDOUT, paywallId = null),
                    )
                val outcome =
                    RuleEvaluationOutcome(
                        confirmableAssignment = null,
                        unsavedOccurrence = TriggerRuleOccurrence.stub(),
                        triggerResult = InternalTriggerResult.Holdout(holdoutExperiment),
                    )

                val thrown =
                    When("getExperiment processes the holdout outcome") {
                        runCatching {
                            getExperiment(
                                request = request,
                                rulesOutcome = outcome,
                                debugInfo = emptyMap(),
                                paywallStatePublisher = states,
                                storage = storage,
                                activateSession = { _, _ -> activated = true },
                            )
                        }.exceptionOrNull()
                    }

                Then("a holdout error is thrown and state is published") {
                    assertNotNull(thrown)
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.Holdout)
                    assertEquals(holdoutExperiment, (thrown as PaywallPresentationRequestStatusReason.Holdout).experiment)
                    assertTrue(activated)
                    val published = states.replayCache.firstOrNull()
                    assertNotNull(published)
                    assertTrue(published is PaywallState.Skipped)
                    assertTrue((published as PaywallState.Skipped).paywallSkippedReason is PaywallSkippedReason.Holdout)
                }
            }
        }

    @Test
    fun noAudienceMatchPublishesSkippedStateAndThrows() =
        runTest {
            Given("a no-audience-match result") {
                val request = presentationRequest()
                val states = MutableSharedFlow<PaywallState>(replay = 1)
                var activated = false
                val outcome = RuleEvaluationOutcome(triggerResult = InternalTriggerResult.NoAudienceMatch(emptyList()))

                val thrown =
                    When("getExperiment executes") {
                        runCatching {
                            getExperiment(
                                request = request,
                                rulesOutcome = outcome,
                                debugInfo = emptyMap(),
                                paywallStatePublisher = states,
                                storage = storage,
                                activateSession = { _, _ -> activated = true },
                            )
                        }.exceptionOrNull()
                    }

                Then("a no-audience-match error is emitted and state published") {
                    assertNotNull(thrown)
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.NoAudienceMatch)
                    assertTrue(activated)
                    val published = states.replayCache.firstOrNull()
                    assertNotNull(published)
                    assertTrue(published is PaywallState.Skipped)
                    assertTrue((published as PaywallState.Skipped).paywallSkippedReason is PaywallSkippedReason.NoAudienceMatch)
                }
            }
        }

    @Test
    fun placementNotFoundPublishesSkippedStateAndThrows() =
        runTest {
            Given("a placement-not-found result") {
                val request = presentationRequest()
                val states = MutableSharedFlow<PaywallState>(replay = 1)
                val outcome = RuleEvaluationOutcome(triggerResult = InternalTriggerResult.PlacementNotFound)

                val thrown =
                    When("getExperiment executes") {
                        runCatching {
                            getExperiment(
                                request = request,
                                rulesOutcome = outcome,
                                debugInfo = emptyMap(),
                                paywallStatePublisher = states,
                                storage = storage,
                            )
                        }.exceptionOrNull()
                    }

                Then("a placement-not-found error is thrown and state published") {
                    assertNotNull(thrown)
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.PlacementNotFound)
                    val published = states.replayCache.firstOrNull()
                    assertNotNull(published)
                    assertTrue(published is PaywallState.Skipped)
                    assertTrue((published as PaywallState.Skipped).paywallSkippedReason is PaywallSkippedReason.PlacementNotFound)
                }
            }
        }

    @Test
    fun errorResultPublishesPresentationError() =
        runTest {
            Given("an error trigger result") {
                val request = presentationRequest(PresentationRequestType.GetPresentationResult)
                val states = MutableSharedFlow<PaywallState>(replay = 1)
                val outcome = RuleEvaluationOutcome(triggerResult = InternalTriggerResult.Error(Exception("boom")))

                val thrown =
                    When("getExperiment executes") {
                        runCatching {
                            getExperiment(
                                request = request,
                                rulesOutcome = outcome,
                                debugInfo = mapOf("key" to "value"),
                                paywallStatePublisher = states,
                                storage = storage,
                            )
                        }.exceptionOrNull()
                    }

                Then("a no-paywall-view error is thrown and a presentation error state published") {
                    assertNotNull(thrown)
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.NoPaywallView)
                    val published = states.replayCache.firstOrNull()
                    assertNotNull(published)
                    assertTrue(published is PaywallState.PresentationError)
                }
            }
        }
}
