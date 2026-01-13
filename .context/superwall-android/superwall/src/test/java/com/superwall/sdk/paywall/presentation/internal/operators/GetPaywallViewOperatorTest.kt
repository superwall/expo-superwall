package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Superwall
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.webview.webViewExists
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.core_data.CoreDataManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class GetPaywallViewOperatorTest {
    private val dependencyContainer = mockk<DependencyContainer>(relaxed = true)
    private val paywallManager = mockk<PaywallManager>(relaxed = true)
    private val storage = mockk<LocalStorage>(relaxed = true)
    private val coreDataManager = mockk<CoreDataManager>(relaxed = true)
    private val superwall = mockk<Superwall>(relaxed = true)
    private val paywallView = mockk<PaywallView>(relaxed = true)

    private val experiment =
        Experiment(
            id = "exp",
            groupId = "group",
            variant = Experiment.Variant(id = "variant", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "paywall"),
        )

    private val request =
        PresentationRequest(
            presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
            flags =
                PresentationRequest.Flags(
                    isDebuggerLaunched = false,
                    entitlements = MutableStateFlow(null),
                    isPaywallPresented = false,
                    type = com.superwall.sdk.paywall.presentation.internal.PresentationRequestType.Presentation,
                ),
        )

    private val rulesOutcome = RuleEvaluationOutcome(triggerResult = InternalTriggerResult.Paywall(experiment))

    @Before
    fun setup() {
        mockkObject(Superwall.Companion)
        every { Superwall.instance } returns superwall
        every { dependencyContainer.paywallManager } returns paywallManager
        every { dependencyContainer.storage } returns storage
        every { storage.coreDataManager } returns coreDataManager
        every { dependencyContainer.makePaywallRequest(any(), any(), any(), any(), any()) } returns
            PaywallRequest(
                eventData = request.presentationInfo.eventData,
                responseIdentifiers = ResponseIdentifiers(experiment.variant.paywallId, experiment),
                overrides = PaywallRequest.Overrides(null, null),
                isDebuggerLaunched = false,
                presentationSourceType = request.presentationSourceType,
                retryCount = 0,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun failsWhenWebViewMissing() =
        runTest {
            Given("the device has no WebView available") {
                mockkStatic(::webViewExists)
                every { webViewExists() } returns false

                val result =
                    When("getPaywallView is executed") {
                        getPaywallView(
                            request = request,
                            rulesOutcome = rulesOutcome,
                            debugInfo = emptyMap(),
                            paywallStatePublisher = null,
                            dependencyContainer = dependencyContainer,
                        )
                    }

                Then("a no-paywall-view error is returned") {
                    assertTrue(result.isFailure)
                    val reason = result.exceptionOrNull()
                    assertNotNull(reason)
                    assertTrue(reason is PaywallPresentationRequestStatusReason.NoPaywallView)
                }
            }
        }

    @Test
    fun returnsPaywallViewWhenSuccessful() =
        runTest {
            Given("the WebView exists and paywall manager returns a view") {
                mockkStatic(::webViewExists)
                every { webViewExists() } returns true
                coEvery {
                    paywallManager.getPaywallView(
                        request = any(),
                        isForPresentation = any(),
                        isPreloading = any(),
                        delegate = any(),
                    )
                } returns Either.Success(paywallView)

                val result =
                    When("getPaywallView is executed") {
                        getPaywallView(
                            request = request,
                            rulesOutcome = rulesOutcome,
                            debugInfo = emptyMap(),
                            paywallStatePublisher = null,
                            dependencyContainer = dependencyContainer,
                        )
                    }

                Then("the paywall view is returned") {
                    assertTrue(result.isSuccess)
                    assertSame(paywallView, result.getOrNull())
                }
            }
        }

    @Test
    fun emitsErrorWhenPaywallManagerFails() =
        runTest {
            Given("the WebView exists but paywall manager throws") {
                mockkStatic(::webViewExists)
                every { webViewExists() } returns true
                val error = IllegalStateException("boom")
                coEvery {
                    paywallManager.getPaywallView(
                        request = any(),
                        isForPresentation = any(),
                        isPreloading = any(),
                        delegate = any(),
                    )
                } returns Either.Failure(error)
                val states = MutableSharedFlow<PaywallState>(replay = 1)

                val result =
                    When("getPaywallView is executed") {
                        getPaywallView(
                            request = request,
                            rulesOutcome = rulesOutcome,
                            debugInfo = mapOf("context" to "test"),
                            paywallStatePublisher = states,
                            dependencyContainer = dependencyContainer,
                        )
                    }

                Then("a failure is propagated and presentation error published") {
                    assertTrue(result.isFailure)
                    val reason = result.exceptionOrNull()
                    assertNotNull(reason)
                    assertTrue(reason is PaywallPresentationRequestStatusReason.NoPaywallView)
                    val published = states.replayCache.firstOrNull()
                    assertNotNull(published)
                    assertTrue(published is PaywallState.PresentationError)
                    val thrown = runCatching { throw (published as PaywallState.PresentationError).error }.exceptionOrNull()
                    assertNotNull(thrown)
                    assertEquals(error, thrown)
                }
            }
        }
}
