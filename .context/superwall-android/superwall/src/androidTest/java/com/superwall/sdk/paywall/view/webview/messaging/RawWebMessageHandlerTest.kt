package com.superwall.sdk.paywall.view.webview.messaging

import Given
import Then
import When
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.superwall.sdk.misc.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RawWebMessageHandlerTest {
    private class RecordingDelegate : WebEventDelegate {
        val receivedMessages = mutableListOf<PaywallMessage>()

        override suspend fun handle(message: PaywallMessage) {
            receivedMessages.add(message)
        }
    }

    @Test
    fun postMessage_handles_valid_wrapped_paywall_message_with_single_event() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a valid message with a single close event is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "close"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100) // Allow coroutines to process

                    Then("the delegate receives the close message") {
                        assertEquals(1, delegate.receivedMessages.size)
                        assertTrue(delegate.receivedMessages[0] is PaywallMessage.Close)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_multiple_events_in_single_message() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a valid message with multiple events is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "ping",
                                        "version": "2.0.0"
                                    },
                                    {
                                        "event_name": "restore"
                                    },
                                    {
                                        "event_name": "close"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives all three messages in order") {
                        assertEquals(3, delegate.receivedMessages.size)
                        assertTrue(delegate.receivedMessages[0] is PaywallMessage.OnReady)
                        assertTrue(delegate.receivedMessages[1] is PaywallMessage.Restore)
                        assertTrue(delegate.receivedMessages[2] is PaywallMessage.Close)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_purchase_event_with_product_details() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a purchase event message is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "purchase",
                                        "product": "premium",
                                        "product_identifier": "com.example.premium"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives the purchase message with correct product info") {
                        assertEquals(1, delegate.receivedMessages.size)
                        val purchaseMessage = delegate.receivedMessages[0] as PaywallMessage.Purchase
                        assertEquals("premium", purchaseMessage.product)
                        assertEquals("com.example.premium", purchaseMessage.productId)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_invalid_JSON_gracefully() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("an invalid JSON message is posted") {
                    val message = "{ invalid json }"

                    handler.postMessage(message)
                    delay(100)

                    Then("no messages are passed to the delegate") {
                        assertEquals(0, delegate.receivedMessages.size)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_missing_payload_field_gracefully() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a message without payload is posted") {
                    val message =
                        """
                        {
                            "version": 1
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("no messages are passed to the delegate") {
                        assertEquals(0, delegate.receivedMessages.size)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_custom_event_with_data() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a custom event message is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "custom",
                                        "data": "custom_data_string"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives the custom message with data") {
                        assertEquals(1, delegate.receivedMessages.size)
                        val customMessage = delegate.receivedMessages[0] as PaywallMessage.Custom
                        assertEquals("custom_data_string", customMessage.data)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_URL_events_correctly() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("URL-related events are posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "open_url",
                                        "url": "https://example.com"
                                    },
                                    {
                                        "event_name": "open_url_external",
                                        "url": "https://external.com"
                                    },
                                    {
                                        "event_name": "open_deep_link",
                                        "link": "myapp://path"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives all URL-related messages") {
                        assertEquals(3, delegate.receivedMessages.size)
                        assertTrue(delegate.receivedMessages[0] is PaywallMessage.OpenUrl)
                        assertTrue(delegate.receivedMessages[1] is PaywallMessage.OpenUrlInBrowser)
                        assertTrue(delegate.receivedMessages[2] is PaywallMessage.OpenDeepLink)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_request_review_event() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a request review event is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "request_store_review",
                                        "review_type": "in-app"
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives the request review message") {
                        assertEquals(1, delegate.receivedMessages.size)
                        val reviewMessage = delegate.receivedMessages[0] as PaywallMessage.RequestReview
                        assertEquals(PaywallMessage.RequestReview.Type.INAPP, reviewMessage.type)
                    }
                }
            }
        }

    @Test
    fun postMessage_handles_custom_placement_event() =
        runTest {
            Given("a RawWebMessageHandler with a recording delegate") {
                val delegate = RecordingDelegate()
                val handler = RawWebMessageHandler(delegate, MainScope(this@runTest.coroutineContext))

                When("a custom placement event is posted") {
                    val message =
                        """
                        {
                            "version": 1,
                            "payload": {
                                "events": [
                                    {
                                        "event_name": "custom_placement",
                                        "name": "my_placement",
                                        "params": {
                                            "key": "value"
                                        }
                                    }
                                ]
                            }
                        }
                        """.trimIndent()

                    handler.postMessage(message)
                    delay(100)

                    Then("the delegate receives the custom placement message") {
                        assertEquals(1, delegate.receivedMessages.size)
                        val placementMessage = delegate.receivedMessages[0] as PaywallMessage.CustomPlacement
                        assertEquals("my_placement", placementMessage.name)
                        assertEquals("value", placementMessage.params["key"]?.jsonPrimitive?.content)
                    }
                }
            }
        }
}
