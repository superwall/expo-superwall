package com.superwall.sdk.paywall.view.webview.messaging

import Given
import Then
import When
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Golden master tests for PaywallMessage parsing.
 * These tests verify the JSON parsing behavior of parseWrappedPaywallMessages.
 */
@RunWith(AndroidJUnit4::class)
class PaywallMessageParsingTest {
    // region OnReady (ping)

    @Test
    fun parse_ping_returns_OnReady_with_version() {
        Given("a JSON with ping event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "ping",
                                "version": "3.2.1"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OnReady with correct paywallJsVersion") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OnReady
                    assertEquals("3.2.1", message.paywallJsVersion)
                }
            }
        }
    }

    // endregion

    // region Close

    @Test
    fun parse_close_returns_Close() {
        Given("a JSON with close event") {
            val json =
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

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns Close message") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    assertTrue(messages[0] is PaywallMessage.Close)
                }
            }
        }
    }

    // endregion

    // region Restore

    @Test
    fun parse_restore_returns_Restore() {
        Given("a JSON with restore event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "restore"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns Restore message") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    assertTrue(messages[0] is PaywallMessage.Restore)
                }
            }
        }
    }

    // endregion

    // region OpenUrl

    @Test
    fun parse_open_url_without_browser_type_returns_OpenUrl_with_null_browserType() {
        Given("a JSON with open_url event without browser_type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_url",
                                "url": "https://example.com/page"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenUrl with null browserType") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenUrl
                    assertEquals("https://example.com/page", message.url.toString())
                    assertNull(message.browserType)
                }
            }
        }
    }

    @Test
    fun parse_open_url_with_null_browser_type_returns_OpenUrl_with_null_browserType() {
        Given("a JSON with open_url event with explicit null browser_type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_url",
                                "url": "https://example.com/page",
                                "browser_type": null
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenUrl with null browserType") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenUrl
                    assertEquals("https://example.com/page", message.url.toString())
                    assertNull(message.browserType)
                }
            }
        }
    }

    @Test
    fun parse_open_url_with_payment_sheet_browser_type_returns_OpenUrl_with_PAYMENT_SHEET() {
        Given("a JSON with open_url event with payment_sheet browser_type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_url",
                                "url": "https://payment.example.com",
                                "browser_type": "payment_sheet"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenUrl with PAYMENT_SHEET browserType") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenUrl
                    assertEquals("https://payment.example.com", message.url.toString())
                    assertEquals(PaywallMessage.OpenUrl.BrowserType.PAYMENT_SHEET, message.browserType)
                }
            }
        }
    }

    @Test
    fun parse_open_url_with_unknown_browser_type_returns_OpenUrl_with_null_browserType() {
        Given("a JSON with open_url event with unknown browser_type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_url",
                                "url": "https://example.com",
                                "browser_type": "unknown_type"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenUrl with null browserType") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenUrl
                    assertNull(message.browserType)
                }
            }
        }
    }

    // endregion

    // region OpenUrlInBrowser (open_url_external)

    @Test
    fun parse_open_url_external_returns_OpenUrlInBrowser() {
        Given("a JSON with open_url_external event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_url_external",
                                "url": "https://external.example.com/path?query=1"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenUrlInBrowser with correct URL") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenUrlInBrowser
                    assertEquals("https://external.example.com/path?query=1", message.url.toString())
                }
            }
        }
    }

    // endregion

    // region OpenDeepLink (open_deep_link)

    @Test
    fun parse_open_deep_link_returns_OpenDeepLink() {
        Given("a JSON with open_deep_link event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "open_deep_link",
                                "link": "myapp://settings/premium"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns OpenDeepLink with correct URL") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.OpenDeepLink
                    assertEquals("myapp://settings/premium", message.url.toString())
                }
            }
        }
    }

    // endregion

    // region Purchase

    @Test
    fun parse_purchase_returns_Purchase_with_product_info() {
        Given("a JSON with purchase event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "purchase",
                                "product": "primary",
                                "product_identifier": "com.app.subscription.monthly"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns Purchase with correct product and productId") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.Purchase
                    assertEquals("primary", message.product)
                    assertEquals("com.app.subscription.monthly", message.productId)
                }
            }
        }
    }

    // endregion

    // region Custom

    @Test
    fun parse_custom_returns_Custom_with_data() {
        Given("a JSON with custom event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "custom",
                                "data": "custom_action_data_string"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns Custom with correct data") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.Custom
                    assertEquals("custom_action_data_string", message.data)
                }
            }
        }
    }

    // endregion

    // region CustomPlacement

    @Test
    fun parse_custom_placement_returns_CustomPlacement_with_name_and_params() {
        Given("a JSON with custom_placement event") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "custom_placement",
                                "name": "checkout_upsell",
                                "params": {
                                    "product_id": "premium_yearly",
                                    "discount": 20,
                                    "is_trial": true
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns CustomPlacement with correct name and params") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.CustomPlacement
                    assertEquals("checkout_upsell", message.name)
                    assertEquals("premium_yearly", message.params["product_id"]?.jsonPrimitive?.content)
                    assertEquals(20, message.params["discount"]?.jsonPrimitive?.int)
                    assertEquals(true, message.params["is_trial"]?.jsonPrimitive?.boolean)
                }
            }
        }
    }

    @Test
    fun parse_custom_placement_with_nested_params() {
        Given("a JSON with custom_placement event with nested params") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "custom_placement",
                                "name": "complex_placement",
                                "params": {
                                    "nested": {
                                        "key": "value"
                                    },
                                    "array": [1, 2, 3]
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns CustomPlacement with nested params accessible") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.CustomPlacement
                    assertEquals("complex_placement", message.name)
                    val nested = message.params["nested"]?.jsonObject
                    assertNotNull(nested)
                    assertEquals("value", nested!!["key"]?.jsonPrimitive?.content)
                    val array = message.params["array"]?.jsonArray
                    assertNotNull(array)
                    assertEquals(3, array!!.size)
                }
            }
        }
    }

    // endregion

    // region RequestReview (request_store_review)

    @Test
    fun parse_request_store_review_inapp_returns_RequestReview_INAPP() {
        Given("a JSON with request_store_review event with in-app type") {
            val json =
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

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns RequestReview with INAPP type") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.RequestReview
                    assertEquals(PaywallMessage.RequestReview.Type.INAPP, message.type)
                }
            }
        }
    }

    @Test
    fun parse_request_store_review_external_returns_RequestReview_EXTERNAL() {
        Given("a JSON with request_store_review event with external type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "request_store_review",
                                "review_type": "external"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns RequestReview with EXTERNAL type") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.RequestReview
                    assertEquals(PaywallMessage.RequestReview.Type.EXTERNAL, message.type)
                }
            }
        }
    }

    @Test
    fun parse_request_store_review_unknown_type_defaults_to_INAPP() {
        Given("a JSON with request_store_review event with unknown type") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "request_store_review",
                                "review_type": "unknown_type"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns RequestReview with INAPP type as default") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.RequestReview
                    assertEquals(PaywallMessage.RequestReview.Type.INAPP, message.type)
                }
            }
        }
    }

    // endregion

    // region Version parsing

    @Test
    fun parse_with_explicit_version_returns_correct_version() {
        Given("a JSON with explicit version 2") {
            val json =
                """
                {
                    "version": 2,
                    "payload": {
                        "events": [
                            {
                                "event_name": "close"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns wrapper with version 2") {
                    assertTrue(result.isSuccess)
                    val wrapped = result.getOrThrow()
                    assertEquals(2, wrapped.version)
                }
            }
        }
    }

    @Test
    fun parse_without_version_defaults_to_1() {
        Given("a JSON without version field") {
            val json =
                """
                {
                    "payload": {
                        "events": [
                            {
                                "event_name": "close"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns wrapper with default version 1") {
                    assertTrue(result.isSuccess)
                    val wrapped = result.getOrThrow()
                    assertEquals(1, wrapped.version)
                }
            }
        }
    }

    // endregion

    // region Multiple events

    @Test
    fun parse_multiple_events_returns_all_messages_in_order() {
        Given("a JSON with multiple events") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "ping",
                                "version": "1.0.0"
                            },
                            {
                                "event_name": "restore"
                            },
                            {
                                "event_name": "purchase",
                                "product": "premium",
                                "product_identifier": "com.app.premium"
                            },
                            {
                                "event_name": "close"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns all messages in correct order") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(4, messages.size)
                    assertTrue(messages[0] is PaywallMessage.OnReady)
                    assertTrue(messages[1] is PaywallMessage.Restore)
                    assertTrue(messages[2] is PaywallMessage.Purchase)
                    assertTrue(messages[3] is PaywallMessage.Close)
                }
            }
        }
    }

    // endregion

    // region UserAttributesUpdated (user_attribute_updated)

    @Test
    fun parse_user_attribute_updated_returns_UserAttributesUpdated_with_data() {
        Given("a JSON with user_attribute_updated event") {
            // The attributes field is an array of {key, value} objects
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "user_attribute_updated",
                                "attributes": [
                                    {"key": "gender", "value": "male"},
                                    {"key": "age", "value": 25},
                                    {"key": "is_premium", "value": true}
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns UserAttributesUpdated with correct data map") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.UserAttributesUpdated
                    assertEquals("male", message.data["gender"])
                    assertEquals(25.0, message.data["age"])
                    assertEquals(true, message.data["is_premium"])
                }
            }
        }
    }

    @Test
    fun parse_user_attribute_updated_with_null_value() {
        Given("a JSON with user_attribute_updated event containing null value") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "user_attribute_updated",
                                "attributes": [
                                    {"key": "name", "value": "John"},
                                    {"key": "nickname", "value": null}
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns UserAttributesUpdated with empty string for null value") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(1, messages.size)
                    val message = messages[0] as PaywallMessage.UserAttributesUpdated
                    assertEquals("John", message.data["name"])
                    assertEquals("", message.data["nickname"])
                }
            }
        }
    }

    // endregion

    // region Error cases

    @Test
    fun parse_unknown_event_returns_failure() {
        Given("a JSON with unknown event name") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": [
                            {
                                "event_name": "unknown_event"
                            }
                        ]
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns failure") {
                    assertTrue(result.isFailure)
                    val exception = result.exceptionOrNull()
                    assertTrue(exception is IllegalArgumentException)
                    assertTrue(exception?.message?.contains("unknown_event") == true)
                }
            }
        }
    }

    @Test
    fun parse_invalid_json_returns_failure() {
        Given("invalid JSON") {
            val json = "{ invalid json }"

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns failure") {
                    assertTrue(result.isFailure)
                }
            }
        }
    }

    @Test
    fun parse_missing_payload_returns_failure() {
        Given("JSON missing payload") {
            val json =
                """
                {
                    "version": 1
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns failure") {
                    assertTrue(result.isFailure)
                }
            }
        }
    }

    @Test
    fun parse_missing_events_returns_failure() {
        Given("JSON missing events array") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {}
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns failure") {
                    assertTrue(result.isFailure)
                }
            }
        }
    }

    @Test
    fun parse_empty_events_returns_empty_messages() {
        Given("JSON with empty events array") {
            val json =
                """
                {
                    "version": 1,
                    "payload": {
                        "events": []
                    }
                }
                """.trimIndent()

            When("parsed") {
                val result = parseWrappedPaywallMessages(json)

                Then("it returns success with empty messages") {
                    assertTrue(result.isSuccess)
                    val messages = result.getOrThrow().payload.messages
                    assertEquals(0, messages.size)
                }
            }
        }
    }

    // endregion
}
