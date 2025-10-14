package com.superwall.sdk.paywall.view.webview

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.VariablesFactory
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandler
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandlerDelegate
import com.superwall.sdk.paywall.view.webview.messaging.PaywallWebEvent
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.paywall.view.webview.templating.models.Variables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.net.URI

class PaywallMessageHandlerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUpMain() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownMain() {
        Dispatchers.resetMain()
    }

    private open class FakeDelegate(
        initial: PaywallViewState,
    ) : PaywallMessageHandlerDelegate {
        private var _state: PaywallViewState = initial
        override val state: PaywallViewState
            get() = _state

        override fun updateState(update: PaywallViewState.Updates) {
            _state = update.transform(_state)
        }

        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {}

        override fun openDeepLink(url: String) {}

        override fun presentBrowserInApp(url: String) {}

        override fun presentBrowserExternal(url: String) {}

        override fun evaluate(
            code: String,
            resultCallback: ((String?) -> Unit)?,
        ) {
            resultCallback?.invoke(null)
        }
    }

    private class FakeVariablesFactory : VariablesFactory {
        override suspend fun makeJsonVariables(
            products: List<ProductVariable>?,
            computedPropertyRequests: List<ComputedPropertyRequest>,
            event: EventData?,
        ): JsonVariables = JsonVariables("template_variables", Variables(emptyMap(), emptyMap(), emptyMap()))
    }

    @Test
    fun onReady_setsVersion_via_updateState() =
        runTest {
            Given("a PaywallMessageHandler with a delegate and initial state") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = FakeDelegate(state)
                // Use a cancelled IO scope so didLoadWebView doesn't run (avoids tracking).
                val cancelledIoScope = IOScope(Dispatchers.Unconfined)
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope =
                            object : kotlinx.coroutines.CoroutineScope {
                                override val coroutineContext = kotlinx.coroutines.Job().apply { cancel() } + Dispatchers.Unconfined
                            },
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("an OnReady message is handled") {
                    handler.handle(PaywallMessage.OnReady(paywallJsVersion = "2.0.0"))

                    Then("the delegate state reflects the updated version via state update") {
                        assertEquals("2.0.0", delegate.state.paywall.paywalljsVersion)
                        // didLoadWebView didn't run, so no endAt and not Ready yet
                        assertEquals(null, delegate.state.paywall.webviewLoadingInfo.endAt)
                        assertEquals(PaywallLoadingState.Unknown, delegate.state.loadingState)
                    }
                }
            }
        }

    @Test
    fun onReady_fullFlow_setsEndAt_and_setsReady() =
        runTest {
            Given("a real PaywallMessageHandler flow with injected dependencies") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = FakeDelegate(state)
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("OnReady is handled and async flows complete") {
                    handler.handle(PaywallMessage.OnReady(paywallJsVersion = "3.1.4"))

                    // Allow launched coroutines on Main/IO to run
                    advanceUntilIdle()

                    Then("endAt is set and loading state becomes Ready") {
                        assertEquals("3.1.4", delegate.state.paywall.paywalljsVersion)
                        assertNotNull(delegate.state.paywall.webviewLoadingInfo.endAt)
                        assertEquals(PaywallLoadingState.Ready, delegate.state.loadingState)
                    }
                }
            }
        }

    @Test
    fun handle_fullFlow_processes_messages_end_to_end() =
        runTest {
            Given("a PaywallMessageHandler and a recording delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val evals = mutableListOf<String>()
                        val events = mutableListOf<PaywallWebEvent>()
                        val openedInApp = mutableListOf<String>()
                        val openedExternal = mutableListOf<String>()
                        val deepLinks = mutableListOf<String>()

                        override fun evaluate(
                            code: String,
                            resultCallback: ((String?) -> Unit)?,
                        ) {
                            evals.add(code)
                            resultCallback?.invoke(null)
                        }

                        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
                            events.add(paywallWebEvent)
                        }

                        override fun presentBrowserInApp(url: String) {
                            openedInApp.add(url)
                        }

                        override fun presentBrowserExternal(url: String) {
                            openedExternal.add(url)
                        }

                        override fun openDeepLink(url: String) {
                            deepLinks.add(url)
                        }
                    }

                val tracked = mutableListOf<String>()
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { evt -> tracked.add(evt.superwallPlacement.rawName) },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("messages are handled before readiness (queue), then after ready") {
                    // Before ready: queue these events
                    handler.handle(PaywallMessage.PaywallOpen)
                    handler.handle(PaywallMessage.PaywallClose)

                    // Now ready
                    handler.handle(PaywallMessage.OnReady("4.0.0"))
                    advanceUntilIdle()

                    // Now send additional messages post-ready
                    handler.handle(PaywallMessage.TemplateParamsAndUserAttributes)
                    handler.handle(PaywallMessage.OpenUrl(URI("https://example.com")))
                    handler.handle(PaywallMessage.OpenUrlInBrowser(URI("https://example.com/ext")))
                    handler.handle(PaywallMessage.OpenDeepLink(URI("myapp://path")))
                    handler.handle(PaywallMessage.Restore)
                    handler.handle(PaywallMessage.Purchase(product = "primary", productId = "p1"))
                    handler.handle(PaywallMessage.RequestReview(PaywallMessage.RequestReview.Type.INAPP))
                    handler.handle(PaywallMessage.Close)
                    advanceUntilIdle()

                    Then("state, evaluations, events, and tracking reflect full handling") {
                        // State
                        assertEquals("4.0.0", delegate.state.paywall.paywalljsVersion)
                        assertNotNull(delegate.state.paywall.webviewLoadingInfo.endAt)
                        assertEquals(PaywallLoadingState.Ready, delegate.state.loadingState)

                        // Evaluations should include a script that posts templates
                        // and JSON events for paywall_open and paywall_close
                        val payloads = extractAccept64Payloads(delegate.evals)
                        val joined = payloads.joinToString("\n")
                        assert(joined.contains("template_variables"))
                        val hasOpen = payloads.any { it.contains("\"event_name\":\"paywall_open\"") }
                        val hasClose = payloads.any { it.contains("\"event_name\":\"paywall_close\"") }
                        assert(hasOpen)
                        assert(hasClose)

                        // Events observed on delegate
                        // Close (haptic) emits Closed event
                        assert(eventsContain<PaywallWebEvent.Closed>(delegate.events))
                        // Restore and Purchase
                        assert(eventsContain<PaywallWebEvent.InitiateRestore>(delegate.events))
                        assert(eventsContain<PaywallWebEvent.InitiatePurchase>(delegate.events))
                        // Request review
                        assert(eventsContain<PaywallWebEvent.RequestReview>(delegate.events))

                        // Deep link and open URL tracking via delegate
                        assertEquals(listOf("https://example.com"), delegate.openedInApp)
                        assertEquals(listOf("https://example.com/ext"), delegate.openedExternal)
                        assertEquals(listOf("myapp://path"), delegate.deepLinks)

                        // Tracking called for webview load complete
                        assert(tracked.isNotEmpty())
                    }
                }
            }
        }

    private inline fun <reified T : PaywallWebEvent> eventsContain(list: List<PaywallWebEvent>): Boolean = list.any { it is T }

    private fun extractAccept64Payloads(evals: List<String>): List<String> {
        val prefix = "window.paywall.accept64('"
        val suffix = "');"
        val result = mutableListOf<String>()
        for (s in evals) {
            var idx = 0
            while (true) {
                val start = s.indexOf(prefix, idx)
                if (start == -1) break
                val end = s.indexOf(suffix, start + prefix.length)
                if (end == -1) break
                result.add(s.substring(start + prefix.length, end))
                idx = end + suffix.length
            }
        }
        return result
    }

    @Test
    fun restoreFailed_emits_event_to_webview() =
        runTest {
            Given("a ready handler and delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val evals = mutableListOf<String>()

                        override fun evaluate(
                            code: String,
                            resultCallback: ((String?) -> Unit)?,
                        ) {
                            evals.add(code)
                            resultCallback?.invoke(null)
                        }
                    }
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("OnReady is handled then RestoreFailed arrives") {
                    handler.handle(PaywallMessage.OnReady("5.0.0"))
                    advanceUntilIdle()
                    val before = delegate.evals.size
                    handler.handle(PaywallMessage.RestoreFailed("oops"))
                    advanceUntilIdle()

                    Then("webview receives a restore_fail event JSON") {
                        val anyRestoreFail = delegate.evals.any { it.contains("restore_fail") }
                        assert(anyRestoreFail)
                    }
                }
            }
        }

    @Test
    fun custom_emits_delegate_event() =
        runTest {
            Given("a handler and recording delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val events = mutableListOf<PaywallWebEvent>()
                        val deepLinks = mutableListOf<String>()

                        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
                            events.add(paywallWebEvent)
                        }

                        override fun openDeepLink(url: String) {
                            deepLinks.add(url)
                        }
                    }
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("Custom message arrives") {
                    handler.handle(PaywallMessage.Custom("data:xyz"))

                    Then("delegate receives the custom event with content") {
                        assert(eventsContain<PaywallWebEvent.Custom>(delegate.events))
                    }
                }
            }
        }

    @Test
    fun paywallOpenClose_after_ready_emit_events_to_webview() =
        runTest {
            Given("a ready handler and delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val evals = mutableListOf<String>()

                        override fun evaluate(
                            code: String,
                            resultCallback: ((String?) -> Unit)?,
                        ) {
                            evals.add(code)
                            resultCallback?.invoke(null)
                        }
                    }
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("OnReady then PaywallOpen/PaywallClose are handled") {
                    handler.handle(PaywallMessage.OnReady("6.0.0"))
                    delay(100)

                    val before = delegate.evals.size
                    handler.handle(PaywallMessage.PaywallOpen)
                    handler.handle(PaywallMessage.PaywallClose)
                    delay(100)

                    Then("evaluate contains paywall_open and paywall_close events") {
                        val joined = delegate.evals.drop(before).joinToString("\n")
                        assert(joined.contains("paywall_open"))
                        assert(joined.contains("paywall_close"))
                    }
                }
            }
        }

    @Test
    fun openUrl_variants_emit_delegate_events() =
        runTest {
            Given("a handler and recording events delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val events = mutableListOf<PaywallWebEvent>()
                        val deepLinks = mutableListOf<String>()

                        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
                            events.add(paywallWebEvent)
                        }
                    }
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("OpenUrl, OpenUrlInBrowser, OpenDeepLink arrive") {
                    handler.handle(PaywallMessage.OpenUrl(URI("https://example.org")))
                    handler.handle(PaywallMessage.OpenUrlInBrowser(URI("https://example.org/e")))
                    val deep = URI("myapp://abc/path?x=1&y=2")
                    handler.handle(PaywallMessage.OpenDeepLink(deep))
                    advanceUntilIdle()

                    Then("delegate receives corresponding events with deep link recorded") {
                        assert(eventsContain<PaywallWebEvent.OpenedURL>(delegate.events))
                        assert(eventsContain<PaywallWebEvent.OpenedUrlInChrome>(delegate.events))
                        // Deep link callback is exercised in the full-flow test
                    }
                }
            }
        }

    @Test
    fun requestReview_external_emits_external_event() =
        runTest {
            Given("a handler and recording events delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate =
                    object : FakeDelegate(state) {
                        val events = mutableListOf<PaywallWebEvent>()

                        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
                            events.add(paywallWebEvent)
                        }
                    }
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                    )
                handler.messageHandler = delegate

                When("RequestReview external arrives") {
                    handler.handle(PaywallMessage.RequestReview(PaywallMessage.RequestReview.Type.EXTERNAL))

                    Then("delegate receives external request review event") {
                        val hasExternal =
                            delegate.events.any {
                                it is PaywallWebEvent.RequestReview && it.type == PaywallWebEvent.RequestReview.Type.EXTERNAL
                            }
                        assert(hasExternal)
                    }
                }
            }
        }
}
