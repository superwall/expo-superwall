package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CustomCallbackRegistryTest {
    private lateinit var registry: CustomCallbackRegistry

    @Before
    fun setUp() {
        registry = CustomCallbackRegistry()
    }

    @Test
    fun register_stores_handler_correctly() =
        runTest {
            Given("a CustomCallbackRegistry") {
                val handlerInvocations = mutableListOf<CustomCallback>()
                val handler: suspend (CustomCallback) -> CustomCallbackResult = { callback ->
                    handlerInvocations.add(callback)
                    CustomCallbackResult.success()
                }

                When("a handler is registered for a paywall identifier") {
                    registry.register("paywall_123", handler)

                    Then("the handler can be retrieved") {
                        val retrieved = registry.getHandler("paywall_123")
                        assertNotNull(retrieved)
                    }
                }
            }
        }

    @Test
    fun getHandler_returns_null_for_unregistered_paywall() =
        runTest {
            Given("a CustomCallbackRegistry with no handlers") {
                When("getting a handler for an unregistered paywall") {
                    val result = registry.getHandler("unknown_paywall")

                    Then("it returns null") {
                        assertNull(result)
                    }
                }
            }
        }

    @Test
    fun unregister_removes_handler() =
        runTest {
            Given("a registry with a registered handler") {
                val handler: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.success()
                }
                registry.register("paywall_456", handler)

                When("the handler is unregistered") {
                    registry.unregister("paywall_456")

                    Then("getHandler returns null for that paywall") {
                        assertNull(registry.getHandler("paywall_456"))
                    }
                }
            }
        }

    @Test
    fun unregister_does_not_affect_other_handlers() =
        runTest {
            Given("a registry with multiple handlers") {
                val handler1: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.success(mapOf("source" to "handler1"))
                }
                val handler2: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.success(mapOf("source" to "handler2"))
                }
                registry.register("paywall_1", handler1)
                registry.register("paywall_2", handler2)

                When("one handler is unregistered") {
                    registry.unregister("paywall_1")

                    Then("the other handler is still available") {
                        assertNull(registry.getHandler("paywall_1"))
                        assertNotNull(registry.getHandler("paywall_2"))
                    }
                }
            }
        }

    @Test
    fun clear_removes_all_handlers() =
        runTest {
            Given("a registry with multiple handlers") {
                val handler: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.success()
                }
                registry.register("paywall_a", handler)
                registry.register("paywall_b", handler)
                registry.register("paywall_c", handler)

                When("clear is called") {
                    registry.clear()

                    Then("all handlers are removed") {
                        assertNull(registry.getHandler("paywall_a"))
                        assertNull(registry.getHandler("paywall_b"))
                        assertNull(registry.getHandler("paywall_c"))
                    }
                }
            }
        }

    @Test
    fun register_overwrites_existing_handler() =
        runTest {
            Given("a registry with an existing handler") {
                var firstHandlerCalled = false
                var secondHandlerCalled = false

                val firstHandler: suspend (CustomCallback) -> CustomCallbackResult = {
                    firstHandlerCalled = true
                    CustomCallbackResult.success(mapOf("handler" to "first"))
                }
                val secondHandler: suspend (CustomCallback) -> CustomCallbackResult = {
                    secondHandlerCalled = true
                    CustomCallbackResult.success(mapOf("handler" to "second"))
                }

                registry.register("paywall_x", firstHandler)

                When("a new handler is registered for the same paywall") {
                    registry.register("paywall_x", secondHandler)

                    Then("the new handler replaces the old one") {
                        val retrieved = registry.getHandler("paywall_x")
                        assertNotNull(retrieved)

                        val callback = CustomCallback(name = "test", variables = null)
                        val result = retrieved!!.invoke(callback)

                        assertEquals(false, firstHandlerCalled)
                        assertEquals(true, secondHandlerCalled)
                        assertEquals("second", result.data?.get("handler"))
                    }
                }
            }
        }

    @Test
    fun registered_handler_receives_correct_callback_data() =
        runTest {
            Given("a registry with a handler that captures callback data") {
                var capturedCallback: CustomCallback? = null

                val handler: suspend (CustomCallback) -> CustomCallbackResult = { callback ->
                    capturedCallback = callback
                    CustomCallbackResult.success()
                }
                registry.register("paywall_capture", handler)

                When("the handler is invoked with callback data") {
                    val callback =
                        CustomCallback(
                            name = "validate_email",
                            variables = mapOf("email" to "test@example.com", "count" to 42),
                        )

                    val retrieved = registry.getHandler("paywall_capture")
                    retrieved!!.invoke(callback)

                    Then("the handler receives the correct callback data") {
                        assertNotNull(capturedCallback)
                        assertEquals("validate_email", capturedCallback!!.name)
                        assertEquals("test@example.com", capturedCallback!!.variables?.get("email"))
                        assertEquals(42, capturedCallback!!.variables?.get("count"))
                    }
                }
            }
        }

    @Test
    fun handler_can_return_success_with_data() =
        runTest {
            Given("a handler that returns success with data") {
                val handler: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.success(
                        mapOf(
                            "validated" to true,
                            "score" to 100,
                        ),
                    )
                }
                registry.register("paywall_success", handler)

                When("the handler is invoked") {
                    val callback = CustomCallback(name = "test", variables = null)
                    val retrieved = registry.getHandler("paywall_success")
                    val result = retrieved!!.invoke(callback)

                    Then("it returns success status with the data") {
                        assertEquals(CustomCallbackResultStatus.SUCCESS, result.status)
                        assertEquals(true, result.data?.get("validated"))
                        assertEquals(100, result.data?.get("score"))
                    }
                }
            }
        }

    @Test
    fun handler_can_return_failure_with_data() =
        runTest {
            Given("a handler that returns failure with error data") {
                val handler: suspend (CustomCallback) -> CustomCallbackResult = {
                    CustomCallbackResult.failure(
                        mapOf("error" to "Invalid input"),
                    )
                }
                registry.register("paywall_failure", handler)

                When("the handler is invoked") {
                    val callback = CustomCallback(name = "test", variables = null)
                    val retrieved = registry.getHandler("paywall_failure")
                    val result = retrieved!!.invoke(callback)

                    Then("it returns failure status with error data") {
                        assertEquals(CustomCallbackResultStatus.FAILURE, result.status)
                        assertEquals("Invalid input", result.data?.get("error"))
                    }
                }
            }
        }
}
