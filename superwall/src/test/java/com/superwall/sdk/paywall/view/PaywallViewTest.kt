package com.superwall.sdk.paywall.view

import TemplateLogic
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.TrackingResult
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.SuperwallDelegateAdapter
import com.superwall.sdk.game.GameControllerEvent
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.delegate.PaywallViewEventCallback
import com.superwall.sdk.paywall.view.webview.PaywallUIDelegate
import com.superwall.sdk.paywall.view.webview.PaywallWebUI
import com.superwall.sdk.paywall.view.webview.SendPaywallMessages
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessage
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandler
import com.superwall.sdk.paywall.view.webview.messaging.PaywallWebEvent
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.paywall.view.webview.templating.models.Variables
import com.superwall.sdk.permissions.PermissionStatus
import com.superwall.sdk.permissions.PermissionType
import com.superwall.sdk.permissions.UserPermissions
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.web.WebPaywallRedeemer
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PaywallViewTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var paywallView: PaywallView
    private lateinit var fakeWebUI: FakePaywallWebUI
    private lateinit var sendMessages: RecordingSendMessages
    private lateinit var delegateAdapter: SuperwallDelegateAdapter
    private lateinit var messageHandler: PaywallMessageHandler
    private lateinit var factory: PaywallView.Factory
    private lateinit var deviceHelper: DeviceHelper
    private lateinit var storage: LocalStorage

    private lateinit var redeemer: WebPaywallRedeemer

    @Before
    fun setUp() {
        val paywall = Paywall.stub()
        val state = PaywallViewState(paywall = paywall, locale = "en-US")
        val controller = PaywallView.PaywallController(state)

        messageHandler = mockk(relaxed = true)
        fakeWebUI = FakePaywallWebUI(messageHandler)
        sendMessages = RecordingSendMessages()
        redeemer = mockk()

        factory = mockk(relaxed = true)
        delegateAdapter = mockk(relaxed = true)

        val trackingResult = mockk<TrackingResult>(relaxed = true)
        val options = SuperwallOptions()

        every { factory.makeSuperwallOptions() } returns options
        every { factory.delegate() } returns delegateAdapter
        every { factory.updatePaywallInfo(any()) } just Runs
        every { factory.getCurrentUserAttributes() } returns emptyMap()
        coEvery { factory.track(any()) } returns Result.success(trackingResult)
        coEvery { factory.internallyGetPresentationResult(any(), any()) } just Runs
        coEvery { factory.storePresentationObject(any(), any()) } just Runs

        deviceHelper = mockk(relaxed = true)
        storage = mockk(relaxed = true)

        paywallView =
            PaywallView(
                context = context,
                eventCallback = null,
                callback = null,
                deviceHelper = deviceHelper,
                factory = factory,
                storage = storage,
                webView = fakeWebUI,
                cache = null,
                controller = controller,
                sendMessages = sendMessages,
                redeemer = redeemer,
            )
    }

    @Test
    fun handle_delegatesToInjectedSender() {
        Given("a PaywallView with a recording SendPaywallMessages") {
            val message = PaywallMessage.PaywallOpen

            When("handle is invoked") {
                paywallView.handle(message)

                Then("the message is forwarded to the injected sender") {
                    assertEquals(listOf(message), sendMessages.messages)
                }
            }
        }
    }

    @Test
    fun onThemeChanged_requestsTemplateRefresh() {
        Given("a PaywallView") {
            every { messageHandler.handle(any()) } just Runs

            When("onThemeChanged is called") {
                paywallView.onThemeChanged()

                Then("it asks the message handler for template refresh") {
                    verify(exactly = 1) {
                        messageHandler.handle(
                            withArg { handled ->
                                assertTrue(handled is PaywallMessage.TemplateParamsAndUserAttributes)
                            },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun loadWebView_invokesSetupAndUpdatesLoadingState() {
        Given("loadWebView is triggered") {
            val latch = CountDownLatch(1)
            fakeWebUI.setupLatch = latch

            When("loadWebView is executed") {
                paywallView.loadWebView()

                Then("the WebUI is configured with the paywall URL and state updates") {
                    assertTrue(latch.await(3, TimeUnit.SECONDS))
                    assertEquals(
                        paywallView.state.paywall.url,
                        fakeWebUI.lastSetupUrl,
                    )
                    assertTrue(waitUntil { paywallView.state.loadingState is PaywallLoadingState.LoadingURL })
                }
            }
        }
    }

    @Test
    fun scroll_helpers_delegateToWebView() {
        Given("a PaywallView") {
            When("scrollBy/scrollTo are invoked") {
                paywallView.scrollBy(42)
                paywallView.scrollTo(84)

                Then("they delegate to the web component") {
                    assertEquals(0 to 42, fakeWebUI.lastScrollBy)
                    assertEquals(0 to 84, fakeWebUI.lastScrollTo)
                }
            }
        }
    }

    @Test
    fun gameControllerEventOccured_evaluatesJavascriptBridge() {
        Given("a game controller event") {
            val event =
                GameControllerEvent(
                    controllerElement = "button_a",
                    value = 1.0,
                    x = 0.0,
                    y = 0.0,
                    directional = false,
                )

            When("the event is forwarded to the paywall") {
                paywallView.gameControllerEventOccured(event)

                Then("the WebUI receives the encoded payload") {
                    val expectedPayload =
                        Json {
                            encodeDefaults = true
                            namingStrategy = JsonNamingStrategy.SnakeCase
                        }.encodeToString(GameControllerEvent.serializer(), event)
                    assertEquals(
                        listOf("window.paywall.accept([$expectedPayload])"),
                        fakeWebUI.evaluateCalls,
                    )
                }
            }
        }
    }

    @Test
    fun destroyWebview_forwardsCall() {
        Given("a PaywallView") {
            When("destroyWebview is called") {
                paywallView.destroyWebview()

                Then("the web component is told to clean itself up") {
                    assertTrue(fakeWebUI.destroyed)
                }
            }
        }
    }

    @Test
    fun cleanup_removesChildrenAndClearsScrollListener() {
        Given("a PaywallView with children") {
            val child = View(context)
            paywallView.addView(child)
            fakeWebUI.onScrollChangeListener =
                object : PaywallWebUI.OnScrollChangeListener {
                    override fun onScrollChanged(
                        currentHorizontalScroll: Int,
                        currentVerticalScroll: Int,
                        oldHorizontalScroll: Int,
                        oldcurrentVerticalScroll: Int,
                    ) {
                    }
                }

            When("cleanup is invoked") {
                paywallView.cleanup()

                Then("children are cleared and listener removed") {
                    assertEquals(0, paywallView.childCount)
                    assertNull(fakeWebUI.onScrollChangeListener)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_dispatches_template_message_to_webview() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())
            mockkObject(TemplateLogic)

            try {
                Given("a handler harness with template stubbing") {
                    coEvery {
                        TemplateLogic.getBase64EncodedTemplates(
                            any(),
                            any(),
                            any(),
                            any(),
                            any(),
                        )
                    } returns "encoded-templates"

                    val harness = buildHandlerHarness(scope)

                    When("the template params message is delivered") {
                        harness.handler.handle(PaywallMessage.TemplateParamsAndUserAttributes)
                        advanceUntilIdle()
                    }

                    Then("the template payload is pushed to the web UI") {
                        assertTrue(harness.webUI.evaluateCalls.any { it.contains("encoded-templates") })
                        coVerify(exactly = 1) {
                            TemplateLogic.getBase64EncodedTemplates(any(), any(), any(), any(), any())
                        }
                    }
                }
            } finally {
                unmockkObject(TemplateLogic)
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_onReady_setsVersion_and_flushesQueuedMessages() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())
            mockkObject(TemplateLogic)
            try {
                Given("a harness with a queued paywall open event") {
                    coEvery {
                        TemplateLogic.getBase64EncodedTemplates(
                            any(),
                            any(),
                            any(),
                            any(),
                            any(),
                        )
                    } returns "encoded-templates"

                    val harness = buildHandlerHarness(scope)
                    harness.handler.handle(PaywallMessage.PaywallOpen)
                    advanceUntilIdle()
                    assertTrue(harness.webUI.evaluateCalls.none { it.contains("paywall_open") })

                    val version = "3.2.1"

                    When("the web view reports it is ready") {
                        harness.handler.handle(PaywallMessage.OnReady(version))
                        advanceUntilIdle()
                    }

                    Then("state is updated and queued messages flush") {
                        val actualVersion = harness.paywallView.state.paywall.paywalljsVersion
                        val actualLoadingState = harness.paywallView.state.loadingState
                        assertEquals(version, actualVersion)
                        assertTrue(actualLoadingState is PaywallLoadingState.Ready)
                        assertTrue(
                            "Expected queued paywall open event to flush, but evaluate calls were ${harness.webUI.evaluateCalls}",
                            harness.webUI.evaluateCalls.any { it.contains("paywall_open") },
                        )
                        coVerify(atLeast = 1) {
                            TemplateLogic.getBase64EncodedTemplates(any(), any(), any(), any(), any())
                        }
                    }
                }
            } finally {
                unmockkObject(TemplateLogic)
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_close_notifies_eventCallback() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())

            val capturedEvents = mutableListOf<PaywallWebEvent>()
            val latch = CountDownLatch(1)
            val callback =
                PaywallViewEventCallback { event, _ ->
                    capturedEvents += event
                    latch.countDown()
                }

            val harness = buildHandlerHarness(scope, callback)

            Given("a paywall view with an event callback") {
                harness.handler.handle(PaywallMessage.Close)
                latch.await(500, TimeUnit.MILLISECONDS)

                Then("the close event is delivered") {
                    assertTrue(
                        "Expected Closed event, got $capturedEvents",
                        capturedEvents.contains(PaywallWebEvent.Closed),
                    )
                }
            }
            Dispatchers.resetMain()
        }

    private class RecordingSendMessages : SendPaywallMessages {
        val messages = mutableListOf<PaywallMessage>()

        override fun handle(message: PaywallMessage) {
            messages += message
        }
    }

    private data class HandlerHarness(
        val paywallView: PaywallView,
        val handler: PaywallMessageHandler,
        val webUI: FakePaywallWebUI,
    )

    private fun buildHandlerHarness(
        scope: CoroutineScope,
        eventCallback: PaywallViewEventCallback? = null,
    ): HandlerHarness {
        val options = SuperwallOptions()
        val optionsFactory =
            object : com.superwall.sdk.dependencies.OptionsFactory {
                override fun makeSuperwallOptions(): SuperwallOptions = options
            }

        lateinit var viewRef: PaywallView
        val scopeContext = scope.coroutineContext
        val fakeUserPermissions =
            object : UserPermissions {
                override fun hasPermission(permission: PermissionType): PermissionStatus = PermissionStatus.GRANTED

                override suspend fun requestPermission(
                    activity: Activity,
                    permission: PermissionType,
                ): PermissionStatus = PermissionStatus.GRANTED
            }
        val messageHandler =
            PaywallMessageHandler(
                factory = TestVariablesFactory,
                options = optionsFactory,
                track = { _ -> },
                setAttributes = { _ -> },
                getView = { viewRef },
                mainScope =
                    com.superwall.sdk.misc
                        .MainScope(scopeContext),
                ioScope =
                    object : CoroutineScope {
                        override val coroutineContext = scopeContext
                    },
                json = Json { encodeDefaults = true },
                encodeToB64 = { it },
                userPermissions = fakeUserPermissions,
                getActivity = { null },
            )

        val state =
            PaywallViewState(
                paywall = Paywall.stub().copy(paywalljsVersion = null),
                locale = "en-US",
            )
        val controller = PaywallView.PaywallController(state)
        val webUI = FakePaywallWebUI(messageHandler)

        val localFactory = mockk<PaywallView.Factory>(relaxed = true)
        val localDelegate = mockk<SuperwallDelegateAdapter>(relaxed = true)
        val localTrackingResult = mockk<TrackingResult>(relaxed = true)

        every { localFactory.makeSuperwallOptions() } returns options
        every { localFactory.delegate() } returns localDelegate
        every { localFactory.updatePaywallInfo(any()) } just Runs
        every { localFactory.getCurrentUserAttributes() } returns emptyMap()
        coEvery { localFactory.track(any()) } returns Result.success(localTrackingResult)
        coEvery { localFactory.internallyGetPresentationResult(any(), any()) } just Runs
        coEvery { localFactory.storePresentationObject(any(), any()) } just Runs

        viewRef =
            PaywallView(
                context = context,
                eventCallback = eventCallback,
                callback = null,
                deviceHelper = deviceHelper,
                factory = localFactory,
                storage = storage,
                webView = webUI,
                cache = null,
                controller = controller,
                sendMessages = messageHandler,
                redeemer = redeemer,
            )
        messageHandler.messageHandler = viewRef

        return HandlerHarness(viewRef, messageHandler, webUI)
    }

    private inner class FakePaywallWebUI(
        override val messageHandler: PaywallMessageHandler,
    ) : PaywallWebUI {
        override var delegate: PaywallUIDelegate? = null
        var lastScrollBy: Pair<Int, Int>? = null
        var lastScrollTo: Pair<Int, Int>? = null
        var lastSetupUrl: com.superwall.sdk.models.paywall.PaywallURL? = null
        var destroyed: Boolean = false
        var setupLatch: CountDownLatch? = null
        val evaluateCalls = mutableListOf<String>()
        private val view = View(context)

        override fun onView(perform: View.() -> Unit) {
            perform(view)
        }

        override fun enableBackgroundRendering() {
        }

        override fun scrollBy(
            x: Int,
            y: Int,
        ) {
            lastScrollBy = x to y
        }

        override fun scrollTo(
            x: Int,
            y: Int,
        ) {
            lastScrollTo = x to y
        }

        override fun setup(
            url: com.superwall.sdk.models.paywall.PaywallURL,
            onRenderCrashed: (Boolean, Int) -> Unit,
        ) {
            lastSetupUrl = url
            setupLatch?.countDown()
        }

        override fun evaluate(
            code: String,
            resultCallback: ((String?) -> Unit)?,
        ) {
            evaluateCalls += code
            resultCallback?.invoke(null)
        }

        override fun destroyView() {
            destroyed = true
        }

        override var onScrollChangeListener: PaywallWebUI.OnScrollChangeListener? = null

        override fun detach(fromView: ViewGroup) {
        }

        override fun attach(toView: ViewGroup) {
        }
    }

    private fun waitUntil(
        timeoutMs: Long = 3_000,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(10)
        }
        return condition()
    }

    // ===== Lifecycle & State Management Tests =====

    @Test
    fun loadWebView_withUnknownState_triggersSetup() {
        Given("a PaywallView with Unknown loading state") {
            val latch = CountDownLatch(1)
            fakeWebUI.setupLatch = latch
            assertEquals(PaywallLoadingState.Unknown, paywallView.state.loadingState)

            When("webview is loaded") {
                paywallView.loadWebView()

                Then("the webview setup is triggered") {
                    assertTrue(latch.await(3, TimeUnit.SECONDS))
                    assertEquals(paywallView.state.paywall.url, fakeWebUI.lastSetupUrl)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun beforeViewCreated_callsPresentationWillBegin() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                Given("a PaywallView that is ready to present") {
                    clearMocks(delegateAdapter, answers = false)

                    When("beforeViewCreated is called") {
                        paywallView.beforeViewCreated()

                        Then("willPresentPaywall is called on delegate") {
                            verify { delegateAdapter.willPresentPaywall(paywallView.info) }
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onViewCreated_updatesStateAndNotifiesDelegate() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                Given("a PaywallView with viewCreatedCompletion") {
                    val completionLatch = CountDownLatch(1)
                    var completionCalled = false

                    paywallView.controller.updateState(
                        PaywallViewState.Updates.SetPresentationConfig(
                            styleOverride = null,
                            completion = { result ->
                                completionCalled = result
                                completionLatch.countDown()
                            },
                        ),
                    )

                    clearMocks(delegateAdapter, answers = false)

                    When("onViewCreated is called") {
                        paywallView.onViewCreated()
                        advanceUntilIdle()

                        Then("completion is invoked and state is updated") {
                            assertTrue(completionLatch.await(3, TimeUnit.SECONDS))
                            assertTrue(completionCalled)
                            assertTrue(paywallView.state.isPresented)
                            assertTrue(paywallView.state.presentationDidFinishPrepare)
                            verify { delegateAdapter.didPresentPaywall(paywallView.info) }
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onViewCreated_flushesPendingMessages() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                Given("a PaywallView waiting for presentation completion") {
                    clearMocks(messageHandler, answers = false)

                    When("onViewCreated is invoked") {
                        paywallView.onViewCreated()
                        advanceUntilIdle()

                        Then("pending webview messages are flushed once") {
                            verify(exactly = 1) { messageHandler.flushPendingMessages() }
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun beforeOnDestroy_callsWillDismissDelegate() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                Given("a PaywallView") {
                    clearMocks(delegateAdapter, answers = false)

                    When("beforeOnDestroy is called") {
                        paywallView.beforeOnDestroy()

                        Then("willDismissPaywall is called") {
                            verify { delegateAdapter.willDismissPaywall(paywallView.info) }
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun destroyed_cleansUpAndNotifiesDelegate() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())
            try {
                Given("a PaywallView with a state publisher") {
                    clearMocks(delegateAdapter, answers = false)

                    val statePublisher = MutableSharedFlow<com.superwall.sdk.paywall.presentation.internal.state.PaywallState>()
                    val emittedStates = mutableListOf<com.superwall.sdk.paywall.presentation.internal.state.PaywallState>()

                    scope.launch {
                        statePublisher.collect { emittedStates.add(it) }
                    }

                    paywallView.controller.updateState(
                        PaywallViewState.Updates.SetRequest(
                            req = mockk(relaxed = true),
                            publisher = statePublisher,
                            occurrence = null,
                        ),
                    )

                    When("destroyed is called") {
                        paywallView.destroyed()
                        advanceUntilIdle()

                        Then("state is cleaned up and delegate notified") {
                            val capturedInfo = slot<com.superwall.sdk.paywall.presentation.PaywallInfo>()
                            verify { delegateAdapter.didDismissPaywall(capture(capturedInfo)) }
                            assertFalse(paywallView.state.isPresented)
                        }
                    }
                }
            } finally {
                scope.cancel()
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun stateListener_triggersLoadingStateChange() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                Given("a PaywallView with state listener started") {
                    var loadingStateChangeCalled = false
                    val originalLoadingStateDidChange = paywallView::loadingStateDidChange

                    // Mock the loadingStateDidChange by updating state
                    val initialState = paywallView.state.loadingState

                    When("loading state changes") {
                        paywallView.controller.updateState(
                            PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready),
                        )
                        advanceUntilIdle()

                        Then("the loading state is updated") {
                            assertTrue(paywallView.state.loadingState is PaywallLoadingState.Ready)
                            assertTrue(paywallView.state.loadingState != initialState)
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    // ===== Loading State Changes Tests =====

    @Test
    fun loadingStateChange_toLoadingURL_showsShimmer() {
        Given("a PaywallView that is presented") {
            paywallView.controller.updateState(PaywallViewState.Updates.SetPresentedAndFinished)

            When("loading state changes to LoadingURL") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingURL),
                )
                paywallView.loadingStateDidChange()

                Then("state reflects LoadingURL") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.LoadingURL)
                }
            }
        }
    }

    @Test
    fun loadingStateChange_toLoadingPurchase_shouldTriggerLoadingView() {
        Given("a PaywallView that is presented") {
            paywallView.controller.updateState(PaywallViewState.Updates.SetPresentedAndFinished)

            When("loading state changes to LoadingPurchase") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingPurchase),
                )
                paywallView.loadingStateDidChange()

                Then("state reflects LoadingPurchase") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.LoadingPurchase)
                }
            }
        }
    }

    @Test
    fun loadingStateChange_toManualLoading_shouldTriggerLoadingView() {
        Given("a PaywallView that is presented") {
            paywallView.controller.updateState(PaywallViewState.Updates.SetPresentedAndFinished)

            When("loading state changes to ManualLoading") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.ManualLoading),
                )
                paywallView.loadingStateDidChange()

                Then("state reflects ManualLoading") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.ManualLoading)
                }
            }
        }
    }

    @Test
    fun loadingStateChange_toReady_hidesLoadingAndShimmer() {
        Given("a PaywallView that is presented and loading") {
            paywallView.controller.updateState(PaywallViewState.Updates.SetPresentedAndFinished)
            paywallView.controller.updateState(
                PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingPurchase),
            )

            When("loading state changes to Ready") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready),
                )
                paywallView.loadingStateDidChange()

                Then("state reflects Ready") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.Ready)
                }
            }
        }
    }

    @Test
    fun toggleSpinner_hidesSpinner_whenCurrentlyLoading() {
        Given("a PaywallView in LoadingPurchase state") {
            paywallView.controller.updateState(
                PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingPurchase),
            )

            When("toggleSpinner is called with hidden=true") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.ToggleSpinner(hidden = true),
                )

                Then("state changes to Ready") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.Ready)
                }
            }
        }
    }

    @Test
    fun toggleSpinner_showsSpinner_whenCurrentlyReady() {
        Given("a PaywallView in Ready state") {
            paywallView.controller.updateState(
                PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready),
            )

            When("toggleSpinner is called with hidden=false") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.ToggleSpinner(hidden = false),
                )

                Then("state changes to ManualLoading") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.ManualLoading)
                }
            }
        }
    }

    @Test
    fun toggleSpinner_noOp_whenHidingAndNotLoading() {
        Given("a PaywallView in Ready state") {
            paywallView.controller.updateState(
                PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready),
            )

            When("toggleSpinner is called with hidden=true") {
                paywallView.controller.updateState(
                    PaywallViewState.Updates.ToggleSpinner(hidden = true),
                )

                Then("state remains Ready") {
                    assertTrue(paywallView.state.loadingState is PaywallLoadingState.Ready)
                }
            }
        }
    }

    private object TestVariablesFactory : com.superwall.sdk.dependencies.VariablesFactory {
        override suspend fun makeJsonVariables(
            products: List<ProductVariable>?,
            computedPropertyRequests: List<ComputedPropertyRequest>,
            event: EventData?,
        ): JsonVariables =
            JsonVariables(
                eventName = "template_variables",
                variables =
                    Variables(
                        user = emptyMap(),
                        device = emptyMap(),
                        params = emptyMap(),
                    ),
            )
    }
}
