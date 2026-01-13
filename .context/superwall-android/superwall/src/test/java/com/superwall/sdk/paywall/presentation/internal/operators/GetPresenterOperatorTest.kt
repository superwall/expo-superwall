package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.view.PaywallView
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GetPresenterOperatorTest {
    @After
    fun tearDown() {
        runCatching { unmockkStatic("com.superwall.sdk.analytics.internal.TrackingKt") }
    }

    @Test
    fun `getPresenterIfNecessary short-circuits for GetPaywall requests`() =
        runTest {
            Given("a get paywall request") {
                val paywallView = mockk<PaywallView>()
                val rulesOutcome =
                    RuleEvaluationOutcome(
                        triggerResult =
                            InternalTriggerResult.Paywall(
                                Experiment.presentById("abc"),
                            ),
                    )
                val request = createRequest(PresentationRequestType.GetPaywall(mockk(relaxed = true)))

                var attempts = 0

                When("getPresenterIfNecessary executes") {
                    val presenter =
                        getPresenterIfNecessary(
                            paywallView,
                            rulesOutcome,
                            request,
                            paywallStatePublisher = null,
                            attemptTriggerFire = { _, _ -> attempts++ },
                            activity = { error("activity should not be evaluated for GetPaywall") },
                        )

                    Then("it returns null and fires the trigger once") {
                        assert(presenter == null)
                        assert(1 == attempts)
                    }
                }
            }
        }

    @Test
    fun `getPresenterIfNecessary emits error when no activity is available`() =
        runTest {
            Given("a presentation request without an activity") {
                val paywallView = mockk<PaywallView>()
                val rulesOutcome =
                    RuleEvaluationOutcome(
                        triggerResult =
                            InternalTriggerResult.Paywall(
                                Experiment.presentById("abc"),
                            ),
                    )
                val request = createRequest(PresentationRequestType.Presentation)
                val publisher = MutableSharedFlow<PaywallState>(replay = 1)

                val errorDeferred =
                    async {
                        publisher.first { it is PaywallState.PresentationError } as PaywallState.PresentationError
                    }

                When("getPresenterIfNecessary executes") {
                    assertFailsWith<com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason.NoPresenter> {
                        getPresenterIfNecessary(
                            paywallView,
                            rulesOutcome,
                            request,
                            paywallStatePublisher = publisher,
                            attemptTriggerFire = { _, _ -> },
                            activity = { null },
                        )
                    }

                    val errorState = withTimeout(1_000) { errorDeferred.await() }

                    Then("a presentation error is emitted before throwing") {
                        assertTrue(errorState is PaywallState.PresentationError)
                    }
                }
            }
        }

    @Test
    fun `attemptTriggerFire tracks only when an event name exists and trigger succeeded`() =
        runTest {
            Given("a superwall instance with tracking mocked") {
                val superwall = mockk<com.superwall.sdk.Superwall>(relaxed = true)

                mockkStatic("com.superwall.sdk.analytics.internal.TrackingKt")
                coEvery { superwall.track(any()) } returns Result.success(mockk())

                val paywallResult = InternalTriggerResult.Paywall(Experiment.presentById("paywall-id"))

                When("trigger fires without an event name") {
                    superwall.attemptTriggerFire(
                        createRequest(
                            type = PresentationRequestType.Presentation,
                            info = PresentationInfo.FromIdentifier("identifier", false),
                        ),
                        paywallResult,
                    )

                    Then("no tracking occurs") {
                        coVerify(exactly = 0) { superwall.track(any()) }
                    }
                }

                When("trigger fires with an error result") {
                    superwall.attemptTriggerFire(
                        createRequest(
                            type = PresentationRequestType.Presentation,
                            info = PresentationInfo.ExplicitTrigger(EventData.stub()),
                        ),
                        InternalTriggerResult.Error(Exception("boom")),
                    )

                    Then("tracking is still skipped") {
                        coVerify(exactly = 0) { superwall.track(any()) }
                    }
                }

                When("trigger fires with a valid event and paywall result") {
                    clearMocks(superwall, answers = false)
                    coEvery { superwall.track(any()) } returns Result.success(mockk())
                    superwall.attemptTriggerFire(
                        createRequest(
                            type = PresentationRequestType.Presentation,
                            info = PresentationInfo.ExplicitTrigger(EventData.stub(name = "launch")),
                        ),
                        paywallResult,
                    )

                    Then("a TriggerFire event is tracked") {
                        coVerify(exactly = 1) {
                            superwall.track(
                                match<InternalSuperwallEvent.TriggerFire> { it.triggerName == "launch" },
                            )
                        }
                    }
                }
            }
        }

    private fun EventData.Companion.stub(name: String) = EventData.stub().copy(name = name)

    private fun createRequest(
        type: PresentationRequestType,
        info: PresentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
    ): PresentationRequest =
        PresentationRequest(
            presentationInfo = info,
            flags =
                PresentationRequest.Flags(
                    isDebuggerLaunched = false,
                    entitlements = MutableStateFlow<SubscriptionStatus?>(null),
                    isPaywallPresented = false,
                    type = type,
                ),
        )
}
