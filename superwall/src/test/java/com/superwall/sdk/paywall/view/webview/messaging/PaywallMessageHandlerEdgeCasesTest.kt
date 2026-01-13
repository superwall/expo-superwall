package com.superwall.sdk.paywall.view.webview.messaging

import android.app.Activity
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
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.paywall.view.webview.templating.models.Variables
import com.superwall.sdk.permissions.PermissionStatus
import com.superwall.sdk.permissions.PermissionType
import com.superwall.sdk.permissions.UserPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PaywallMessageHandlerEdgeCasesTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingDelegate(
        initial: PaywallViewState,
    ) : PaywallMessageHandlerDelegate {
        private var _state: PaywallViewState = initial
        override val state: PaywallViewState
            get() = _state

        val events = mutableListOf<PaywallWebEvent>()
        val evaluations = mutableListOf<String>()

        override fun updateState(update: PaywallViewState.Updates) {
            _state = update.transform(_state)
        }

        override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
            events.add(paywallWebEvent)
        }

        override fun openDeepLink(url: String) {}

        override fun presentBrowserInApp(url: String) {}

        override fun presentBrowserExternal(url: String) {}

        override fun evaluate(
            code: String,
            resultCallback: ((String?) -> Unit)?,
        ) {
            evaluations.add(code)
            resultCallback?.invoke(null)
        }

        override fun presentPaymentSheet(url: String) {
        }
    }

    private class FakeVariablesFactory : VariablesFactory {
        override suspend fun makeJsonVariables(
            products: List<ProductVariable>?,
            computedPropertyRequests: List<ComputedPropertyRequest>,
            event: EventData?,
        ): JsonVariables = JsonVariables("template_variables", Variables(emptyMap(), emptyMap(), emptyMap()))
    }

    private class FakeUserPermissions : UserPermissions {
        var permissionToReturn: PermissionStatus = PermissionStatus.GRANTED
        var requestedPermissions = mutableListOf<PermissionType>()

        override fun hasPermission(permission: PermissionType): PermissionStatus = permissionToReturn

        override suspend fun requestPermission(
            activity: Activity,
            permission: PermissionType,
        ): PermissionStatus {
            requestedPermissions.add(permission)
            return permissionToReturn
        }
    }

    @Test
    fun handleCustom_with_null_messageHandler_does_not_crash() =
        runTest {
            Given("a PaywallMessageHandler without a delegate set") {
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                // Note: messageHandler is not set

                When("a Custom message is handled") {
                    handler.handle(
                        PaywallMessage
                            .Custom("test"),
                    )
                    advanceUntilIdle()

                    Then("it does not crash") {
                        // Test passes if no exception is thrown
                    }
                }
            }
        }

    // Note: handleCustomPlacement test requires Android instrumentation test
    // because JSONObject is an Android class

    @Test
    fun hapticFeedback_disabled_with_game_controller_enabled() =
        runTest {
            Given("options with haptic feedback disabled and game controller enabled") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val options =
                    SuperwallOptions().apply {
                        paywalls.isHapticFeedbackEnabled = false
                        isGameControllerEnabled = true
                    }

                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = options
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a Close message is handled (which triggers haptic feedback)") {
                    handler.handle(PaywallMessage.Close)
                    advanceUntilIdle()

                    Then("the close event is still processed") {
                        assertEquals(1, delegate.events.size)
                    }
                }
            }
        }

    @Test
    fun hapticFeedback_enabled_with_game_controller_enabled() =
        runTest {
            Given("options with haptic feedback enabled and game controller enabled") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val options =
                    SuperwallOptions().apply {
                        paywalls.isHapticFeedbackEnabled = true
                        isGameControllerEnabled = true
                    }

                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = options
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a Close message is handled") {
                    handler.handle(PaywallMessage.Close)
                    advanceUntilIdle()

                    Then("the close event is processed") {
                        assertEquals(1, delegate.events.size)
                    }
                }
            }
        }

    @Test
    fun hapticFeedback_disabled_with_game_controller_disabled() =
        runTest {
            Given("options with haptic feedback disabled and game controller disabled") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val options =
                    SuperwallOptions().apply {
                        paywalls.isHapticFeedbackEnabled = false
                        isGameControllerEnabled = false
                    }

                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = options
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a Close message is handled") {
                    handler.handle(PaywallMessage.Close)
                    advanceUntilIdle()

                    Then("the close event is processed") {
                        assertEquals(1, delegate.events.size)
                    }
                }
            }
        }

    @Test
    fun requestReview_EXTERNAL_type_maps_correctly() =
        runTest {
            Given("a handler with a delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a RequestReview EXTERNAL message is handled") {
                    handler.handle(
                        PaywallMessage.RequestReview(
                            PaywallMessage.RequestReview.Type.EXTERNAL,
                        ),
                    )
                    advanceUntilIdle()

                    Then("it emits EXTERNAL review event") {
                        assertEquals(1, delegate.events.size)
                        val event = delegate.events[0] as PaywallWebEvent.RequestReview
                        assertEquals(PaywallWebEvent.RequestReview.Type.EXTERNAL, event.type)
                    }
                }
            }
        }

    @Test
    fun requestReview_INAPP_type_maps_correctly() =
        runTest {
            Given("a handler with a delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a RequestReview INAPP message is handled") {
                    handler.handle(
                        PaywallMessage.RequestReview(
                            PaywallMessage.RequestReview.Type.INAPP,
                        ),
                    )
                    advanceUntilIdle()

                    Then("it emits INAPP review event") {
                        assertEquals(1, delegate.events.size)
                        val event = delegate.events[0] as PaywallWebEvent.RequestReview
                        assertEquals(PaywallWebEvent.RequestReview.Type.INAPP, event.type)
                    }
                }
            }
        }

    @Test
    fun requestPermission_emits_event_with_correct_permission_type() =
        runTest {
            Given("a handler with a delegate") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                val fakePermissions = FakeUserPermissions()
                fakePermissions.permissionToReturn = PermissionStatus.GRANTED

                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { it },
                        userPermissions = fakePermissions,
                        getActivity = { null },
                    )
                handler.messageHandler = delegate

                When("a RequestPermission message is handled") {
                    handler.handle(
                        PaywallMessage.RequestPermission(
                            permissionType = PermissionType.NOTIFICATION,
                            requestId = "test-request-123",
                        ),
                    )
                    advanceUntilIdle()

                    Then("it emits RequestPermission event with correct data") {
                        assertEquals(1, delegate.events.size)
                        val event = delegate.events[0] as PaywallWebEvent.RequestPermission
                        assertEquals(PermissionType.NOTIFICATION, event.permissionType)
                        assertEquals("test-request-123", event.requestId)
                    }
                }
            }
        }

    @Test
    fun requestPermission_without_activity_returns_unsupported() =
        runTest {
            Given("a handler without an activity provider") {
                val paywall = Paywall.stub()
                val state = PaywallViewState(paywall = paywall, locale = "en-US")
                val delegate = RecordingDelegate(state)
                var encodedMessages = mutableListOf<String>()

                val handler =
                    PaywallMessageHandler(
                        factory = FakeVariablesFactory(),
                        options =
                            object : OptionsFactory {
                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                        track = { _ -> },
                        setAttributes = { _ -> },
                        getView = { null },
                        mainScope = MainScope(Dispatchers.Unconfined),
                        ioScope = IOScope(Dispatchers.Unconfined),
                        encodeToB64 = { msg ->
                            encodedMessages.add(msg)
                            msg
                        },
                        userPermissions = FakeUserPermissions(),
                        getActivity = { null }, // No activity available
                    )
                handler.messageHandler = delegate

                When("a RequestPermission message is handled") {
                    handler.handle(
                        PaywallMessage.RequestPermission(
                            permissionType = PermissionType.NOTIFICATION,
                            requestId = "test-request-456",
                        ),
                    )
                    advanceUntilIdle()

                    Then("the permission result contains unsupported status") {
                        // Verify the encoded message contains "unsupported"
                        val lastMessage = encodedMessages.lastOrNull() ?: ""
                        assertEquals(true, lastMessage.contains("unsupported"))
                    }
                }
            }
        }
}
