package com.superwall.sdk.paywall.view

import TemplateLogic
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.view.delegate.PaywallViewEventCallback
import com.superwall.sdk.paywall.view.webview.PaywallUIDelegate
import com.superwall.sdk.paywall.view.webview.PaywallWebUI
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
class PaywallMessageHandlerTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var deviceHelper: DeviceHelper
    private lateinit var storage: LocalStorage

    @Before
    fun setUp() {
        deviceHelper = mockk(relaxed = true)
        storage = mockk(relaxed = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_restore_triggersEventCallback() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())

            try {
                Given("a paywall view with an event callback") {
                    val capturedEvents = mutableListOf<PaywallWebEvent>()
                    val latch = CountDownLatch(1)
                    val callback =
                        PaywallViewEventCallback { event, _ ->
                            capturedEvents += event
                            latch.countDown()
                        }

                    val harness = buildHandlerHarness(scope, callback)

                    When("restore message is handled") {
                        harness.handler.handle(PaywallMessage.Restore)
                        latch.await(500, TimeUnit.MILLISECONDS)

                        Then("InitiateRestore event is delivered") {
                            assertTrue(
                                "Expected InitiateRestore event, got $capturedEvents",
                                capturedEvents.contains(PaywallWebEvent.InitiateRestore),
                            )
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_purchase_triggersEventCallback() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())

            try {
                Given("a paywall view with an event callback") {
                    val capturedEvents = mutableListOf<PaywallWebEvent>()
                    val latch = CountDownLatch(1)
                    val callback =
                        PaywallViewEventCallback { event, _ ->
                            capturedEvents += event
                            latch.countDown()
                        }

                    val harness = buildHandlerHarness(scope, callback)

                    When("purchase message is handled") {
                        val productId = "com.example.product"
                        harness.handler.handle(PaywallMessage.Purchase("Monthly Plan", productId))
                        latch.await(500, TimeUnit.MILLISECONDS)

                        Then("InitiatePurchase event is delivered with product id") {
                            val purchaseEvent =
                                capturedEvents
                                    .filterIsInstance<PaywallWebEvent.InitiatePurchase>()
                                    .firstOrNull()
                            assertTrue("Expected InitiatePurchase event", purchaseEvent != null)
                            assertEquals(productId, purchaseEvent?.productId)
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_openUrl_triggersEventCallback() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())

            try {
                Given("a paywall view with an event callback") {
                    val capturedEvents = mutableListOf<PaywallWebEvent>()
                    val latch = CountDownLatch(1)
                    val callback =
                        PaywallViewEventCallback { event, _ ->
                            capturedEvents += event
                            latch.countDown()
                        }

                    val harness = buildHandlerHarness(scope, callback)

                    When("openUrl message is handled") {
                        val testUrl = java.net.URI("https://example.com")
                        harness.handler.handle(PaywallMessage.OpenUrl(testUrl, null))
                        latch.await(500, TimeUnit.MILLISECONDS)

                        Then("OpenedURL event is delivered") {
                            val urlEvent =
                                capturedEvents
                                    .filterIsInstance<PaywallWebEvent.OpenedURL>()
                                    .firstOrNull()
                            assertTrue("Expected OpenedURL event", urlEvent != null)
                            assertEquals(testUrl, urlEvent?.url)
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_custom_triggersEventCallback() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())

            try {
                Given("a paywall view with an event callback") {
                    val capturedEvents = mutableListOf<PaywallWebEvent>()
                    val latch = CountDownLatch(1)
                    val callback =
                        PaywallViewEventCallback { event, _ ->
                            capturedEvents += event
                            latch.countDown()
                        }

                    val harness = buildHandlerHarness(scope, callback)

                    When("custom message is handled") {
                        val customData = "custom_event_data"
                        harness.handler.handle(PaywallMessage.Custom(customData))
                        latch.await(500, TimeUnit.MILLISECONDS)

                        Then("Custom event is delivered with data") {
                            val customEvent =
                                capturedEvents
                                    .filterIsInstance<PaywallWebEvent.Custom>()
                                    .firstOrNull()
                            assertTrue("Expected Custom event", customEvent != null)
                            assertEquals(customData, customEvent?.string)
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun handler_paywallClose_queuesWhenNotReady() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val scope = CoroutineScope(dispatcher + Job())
            mockkObject(TemplateLogic)

            try {
                Given("a handler harness without paywalljsVersion set") {
                    coEvery {
                        TemplateLogic.getBase64EncodedTemplates(any(), any(), any(), any(), any())
                    } returns "encoded-templates"

                    val harness = buildHandlerHarness(scope)

                    When("PaywallClose message is sent before ready") {
                        harness.handler.handle(PaywallMessage.PaywallClose)
                        advanceUntilIdle()

                        Then("message is queued and not evaluated yet") {
                            assertTrue(harness.webUI.evaluateCalls.none { it.contains("paywall_close") })
                        }
                    }

                    When("webview becomes ready") {
                        harness.handler.handle(PaywallMessage.OnReady("3.2.1"))
                        advanceUntilIdle()

                        Then("queued PaywallClose message is flushed") {
                            assertTrue(
                                "Expected paywall_close in evaluate calls: ${harness.webUI.evaluateCalls}",
                                harness.webUI.evaluateCalls.any { it.contains("paywall_close") },
                            )
                        }
                    }
                }
            } finally {
                unmockkObject(TemplateLogic)
                Dispatchers.resetMain()
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
        every { localFactory.makeSuperwallOptions() } returns options
        val redeemer = mockk<WebPaywallRedeemer>(relaxed = true)
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
        }

        override fun scrollTo(
            x: Int,
            y: Int,
        ) {
        }

        override fun setup(
            url: com.superwall.sdk.models.paywall.PaywallURL,
            onRenderCrashed: (Boolean, Int) -> Unit,
        ) {
        }

        override fun evaluate(
            code: String,
            resultCallback: ((String?) -> Unit)?,
        ) {
            evaluateCalls += code
            resultCallback?.invoke(null)
        }

        override fun destroyView() {
        }

        override var onScrollChangeListener: PaywallWebUI.OnScrollChangeListener? = null

        override fun detach(fromView: ViewGroup) {
        }

        override fun attach(toView: ViewGroup) {
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
