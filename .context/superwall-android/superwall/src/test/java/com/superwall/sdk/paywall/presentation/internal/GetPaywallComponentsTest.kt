package com.superwall.sdk.paywall.presentation.internal

import android.app.Activity
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetPaywallComponentsTest {
    @Test
    fun `runGetPaywallComponents returns expected components using factory`() =
        runTest {
            Given("a stubbed factory with concrete results") {
                val publisher = MutableSharedFlow<PaywallState>()

                val confirmable = ConfirmableAssignment("exp", Experiment.Variant("v1", Experiment.Variant.VariantType.TREATMENT, "pw"))
                val rulesOutcome =
                    RuleEvaluationOutcome(
                        confirmableAssignment = confirmable,
                        triggerResult = InternalTriggerResult.Paywall(Experiment.presentById("pw")),
                    )

                val expectedView = mockk<com.superwall.sdk.paywall.view.PaywallView>(relaxed = true)
                val expectedPresenter = mockk<Activity>(relaxed = true)

                val factory =
                    object : GetPaywallComponentsFactory {
                        override suspend fun waitForEntitlementsAndConfig(
                            request: PresentationRequest,
                            publisher: MutableSharedFlow<PaywallState>?,
                        ) {}

                        override suspend fun checkDebuggerPresentation(
                            request: PresentationRequest,
                            publisher: MutableSharedFlow<PaywallState>?,
                        ) {}

                        override suspend fun evaluateRules(request: PresentationRequest): Result<RuleEvaluationOutcome> =
                            Result.success(rulesOutcome)

                        override suspend fun confirmHoldoutAssignment(
                            request: PresentationRequest,
                            rulesOutcome: RuleEvaluationOutcome,
                        ) {}

                        override suspend fun getPaywallView(
                            request: PresentationRequest,
                            rulesOutcome: RuleEvaluationOutcome,
                            debugInfo: Map<String, Any>,
                            publisher: MutableSharedFlow<PaywallState>?,
                        ): Result<com.superwall.sdk.paywall.view.PaywallView> = Result.success(expectedView)

                        override suspend fun getPresenterIfNecessary(
                            paywallView: com.superwall.sdk.paywall.view.PaywallView,
                            rulesOutcome: RuleEvaluationOutcome,
                            request: PresentationRequest,
                            publisher: MutableSharedFlow<PaywallState>?,
                        ): Activity? = expectedPresenter

                        override suspend fun confirmPaywallAssignment(
                            confirmableAssignment: ConfirmableAssignment?,
                            request: PresentationRequest,
                            isDebuggerLaunched: Boolean,
                        ) {
                        }
                    }

                val request =
                    PresentationRequest(
                        presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
                        flags =
                            PresentationRequest.Flags(
                                isDebuggerLaunched = false,
                                entitlements = MutableStateFlow<SubscriptionStatus?>(null),
                                isPaywallPresented = false,
                                type = PresentationRequestType.Presentation,
                            ),
                    )

                When("runGetPaywallComponents executes") {
                    val result = runGetPaywallComponents(factory, request, publisher).getOrThrow()

                    Then("it surfaces the factory outputs") {
                        assert(expectedView == result.view)
                        assert(expectedPresenter == result.presenter)
                        assert(rulesOutcome == result.rulesOutcome)
                    }
                }
            }
        }
}
