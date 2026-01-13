package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.TrackingLogic
import com.superwall.sdk.analytics.internal.TrackingParameters
import com.superwall.sdk.analytics.internal.TrackingResult
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.misc.SerialTaskManager
import com.superwall.sdk.models.config.FeatureGatingBehavior
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.view.PaywallView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicPresentationInternalsTest {
    private val testDispatcher = StandardTestDispatcher()

    @After
    fun tearDown() {
        runCatching { unmockkObject(TrackingLogic.Companion) }
    }

    @Test
    fun dismissPaywall_invokesDismissActionWhenViewExists() =
        runTest(testDispatcher) {
            Given("dismiss parameters bound to a paywall view") {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val view = mockk<PaywallView>()
                var payload: Triple<PaywallResult, PaywallCloseReason, Boolean>? = null

                val params =
                    DismissParams(
                        dispatcher = dispatcher,
                        paywallView = view,
                        result = PaywallResult.Declined(),
                        closeReason = PaywallCloseReason.SystemLogic,
                    ) { passedView, result, reason, completion ->
                        payload = Triple(result, reason, passedView === view)
                        completion?.invoke()
                    }

                When("dismissPaywall runs") {
                    dismissPaywall(params)

                    Then("it dispatches to the provided dismiss action") {
                        val (result, reason, sameInstance) = requireNotNull(payload)
                        assertTrue(result is PaywallResult.Declined)
                        assertEquals(PaywallCloseReason.SystemLogic, reason)
                        assertTrue(sameInstance)
                    }
                }
            }
        }

    @Test
    fun dismissPaywall_completesWhenViewIsNull() =
        runTest(testDispatcher) {
            Given("dismiss parameters without an attached view") {
                val dispatcher = StandardTestDispatcher(testScheduler)
                var called = false

                val params =
                    DismissParams(
                        dispatcher = dispatcher,
                        paywallView = null,
                        result = PaywallResult.Declined(),
                        closeReason = PaywallCloseReason.ManualClose,
                    ) { _, _, _, _ ->
                        called = true
                    }

                When("dismissPaywall executes") {
                    dismissPaywall(params)

                    Then("the dismiss callback is not invoked") {
                        assertFalse(called)
                    }
                }
            }
        }

    @Test
    fun performTrackAndPresent_successTracksAndPresents() =
        runTest(testDispatcher) {
            Given("a successful tracking context") {
                mockkObject(TrackingLogic.Companion)
                every { TrackingLogic.checkNotSuperwallEvent(any()) } answers { }

                val tracked = mutableListOf<UserInitiatedEvent.Track>()
                var createdRequest: PresentationRequest? = null
                var presentedPublisher: MutableSharedFlow<PaywallState>? = null

                val context =
                    TrackAndPresentContext(
                        track = { event ->
                            tracked += event
                            Result.success(
                                TrackingResult(EventData.stub(), TrackingParameters.stub()),
                            )
                        },
                        makePresentationRequest = { info, _, _, _ ->
                            createPresentationRequest(info).also { createdRequest = it }
                        },
                        isPaywallPresented = { false },
                        present = { request, publisher ->
                            presentedPublisher = publisher
                            assertSame(createdRequest, request)
                        },
                    )

                val publisher = MutableSharedFlow<PaywallState>(replay = 1)

                When("performTrackAndPresent executes") {
                    performTrackAndPresent(
                        context,
                        TrackAndPresentRequest(
                            placement = "my_event",
                            params = mapOf("foo" to "bar"),
                            paywallOverrides = null,
                            isFeatureGatable = true,
                            publisher = publisher,
                        ),
                    )

                    Then("the event is tracked and presentation invoked") {
                        assertEquals(1, tracked.size)
                        assertNotNull(createdRequest)
                        assertSame(publisher, presentedPublisher)
                    }
                }
            }
        }

    @Test
    fun performTrackAndPresent_emitsErrorWhenTrackingFails() =
        runTest(testDispatcher) {
            Given("a tracking context that fails") {
                mockkObject(TrackingLogic.Companion)
                every { TrackingLogic.checkNotSuperwallEvent(any()) } answers { }

                val publisher = MutableSharedFlow<PaywallState>(replay = 1)
                val context =
                    TrackAndPresentContext(
                        track = { Result.failure(Exception("boom")) },
                        makePresentationRequest = { _, _, _, _ -> mockk() },
                        isPaywallPresented = { false },
                        present = { _, _ -> },
                    )

                val errorDeferred =
                    async {
                        publisher.first { it is PaywallState.PresentationError } as PaywallState.PresentationError
                    }

                When("performTrackAndPresent executes") {
                    performTrackAndPresent(
                        context,
                        TrackAndPresentRequest(
                            placement = "test",
                            params = null,
                            paywallOverrides = null,
                            isFeatureGatable = false,
                            publisher = publisher,
                        ),
                    )

                    testScheduler.advanceUntilIdle()
                    val errorState = withTimeout(1_000) { errorDeferred.await() }

                    Then("a presentation error state is emitted") {
                        assertNotNull(errorState.error)
                    }
                }
            }
        }

    @Test
    fun registerPaywall_routesHandlersForLifecycleStates() =
        runTest(testDispatcher) {
            Given("a register context capturing lifecycle callbacks") {
                mockkObject(TrackingLogic.Companion)
                every { TrackingLogic.checkNotSuperwallEvent(any()) } answers { }

                val dispatcher = StandardTestDispatcher(testScheduler)
                val collectionScope = CoroutineScope(dispatcher)
                val serialTaskManager = SerialTaskManager(collectionScope)
                val handler = PaywallPresentationHandler()

                val presented = mutableListOf<PaywallInfo>()
                val dismissed = mutableListOf<PaywallResult>()
                val skipped = mutableListOf<PaywallSkippedReason>()
                val completions = mutableListOf<Unit>()
                val errors = mutableListOf<Throwable>()

                handler.onPresent { presented += it }
                handler.onDismiss { _, result -> dismissed += result }
                handler.onSkip { skipped += it }
                handler.onError { errors += it }

                val presentationRequest = createPresentationRequest(PresentationInfo.ExplicitTrigger(EventData.stub()))
                var capturedPublisher: MutableSharedFlow<PaywallState>? = null

                val registerContext =
                    RegisterContext(
                        dispatcher = dispatcher,
                        collectionScope = collectionScope,
                        serialTaskManager = serialTaskManager,
                        trackAndPresentContext =
                            TrackAndPresentContext(
                                track = {
                                    Result.success(
                                        TrackingResult(EventData.stub(), TrackingParameters.stub()),
                                    )
                                },
                                makePresentationRequest = { _, _, _, _ -> presentationRequest },
                                isPaywallPresented = { false },
                                present = { _, publisher -> capturedPublisher = publisher },
                            ),
                    )

                try {
                    When("registerPaywall executes and emits lifecycle states") {
                        registerPaywall(
                            registerContext,
                            RegisterRequest(
                                placement = "placement",
                                params = mapOf("key" to "value"),
                                handler = handler,
                                completion = { completions += Unit },
                                paywallOverrides = PaywallOverrides(),
                            ),
                        )

                        dispatcher.scheduler.advanceUntilIdle()

                        val publisher = requireNotNull(capturedPublisher)
                        val baseInfo =
                            PaywallInfo.empty().copy(
                                featureGatingBehavior = FeatureGatingBehavior.NonGated,
                                closeReason = PaywallCloseReason.ManualClose,
                            )

                        publisher.emit(PaywallState.Presented(baseInfo))
                        publisher.emit(PaywallState.Dismissed(baseInfo, PaywallResult.Purchased(productId = "prod")))
                        publisher.emit(PaywallState.Skipped(PaywallSkippedReason.NoAudienceMatch()))

                        Then("handlers capture each lifecycle event") {
                            assertEquals(1, presented.size)
                            assertEquals(1, dismissed.size)
                            assertEquals(1, skipped.size)
                            assertEquals(2, completions.size)
                            assertTrue(errors.isEmpty())
                        }
                    }
                } finally {
                    collectionScope.cancel()
                }
            }
        }

    @Test
    fun registerPaywall_propagatesWebViewFailureError() =
        runTest(testDispatcher) {
            Given("a register context where the paywall fails to load") {
                mockkObject(TrackingLogic.Companion)
                every { TrackingLogic.checkNotSuperwallEvent(any()) } answers { }

                val dispatcher = StandardTestDispatcher(testScheduler)
                val collectionScope = CoroutineScope(dispatcher)
                val serialTaskManager = SerialTaskManager(collectionScope)
                val handler = PaywallPresentationHandler()
                val errors = mutableListOf<Throwable>()
                handler.onError { errors += it }

                val presentationRequest = createPresentationRequest(PresentationInfo.ExplicitTrigger(EventData.stub()))
                var capturedPublisher: MutableSharedFlow<PaywallState>? = null

                val registerContext =
                    RegisterContext(
                        dispatcher = dispatcher,
                        collectionScope = collectionScope,
                        serialTaskManager = serialTaskManager,
                        trackAndPresentContext =
                            TrackAndPresentContext(
                                track = {
                                    Result.success(
                                        TrackingResult(EventData.stub(), TrackingParameters.stub()),
                                    )
                                },
                                makePresentationRequest = { _, _, _, _ -> presentationRequest },
                                isPaywallPresented = { false },
                                present = { _, publisher -> capturedPublisher = publisher },
                            ),
                    )

                try {
                    When("registerPaywall executes and the publisher emits a dismissed state with a webview failure") {
                        registerPaywall(
                            registerContext,
                            RegisterRequest(
                                placement = "placement",
                                params = null,
                                handler = handler,
                                completion = null,
                                paywallOverrides = null,
                            ),
                        )

                        dispatcher.scheduler.advanceUntilIdle()

                        val publisher = requireNotNull(capturedPublisher)
                        val failingInfo =
                            PaywallInfo.empty().copy(
                                featureGatingBehavior = FeatureGatingBehavior.Gated,
                                closeReason = PaywallCloseReason.WebViewFailedToLoad,
                            )

                        publisher.emit(PaywallState.Dismissed(failingInfo, PaywallResult.Declined()))

                        Then("the error handler receives the generated throwable") {
                            assertEquals(1, errors.size)
                        }
                    }
                } finally {
                    collectionScope.cancel()
                }
            }
        }
}

private fun createPresentationRequest(info: PresentationInfo): PresentationRequest =
    PresentationRequest(
        presentationInfo = info,
        paywallOverrides = null,
        flags =
            PresentationRequest.Flags(
                isDebuggerLaunched = false,
                entitlements = MutableStateFlow<SubscriptionStatus?>(null),
                isPaywallPresented = false,
                type = PresentationRequestType.Presentation,
            ),
    )
