package com.superwall.sdk.web

import com.superwall.sdk.models.entitlements.RedeemRequest
import com.superwall.sdk.models.entitlements.TransactionReceipt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

class AttributionPropsSerializationTest {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Test
    fun `show how attribution props are serialized`() {
        // Create sample attribution props with different data types
        val attributionProps: Map<String, JsonElement> =
            mapOf(
                "campaign" to JsonPrimitive("summer_sale"),
                "source" to JsonPrimitive("facebook"),
                "user_id" to JsonPrimitive(12345),
                "conversion_value" to JsonPrimitive(99.99),
                "is_organic" to JsonPrimitive(false),
                "custom_attributes" to
                    buildJsonObject {
                        put("region", JsonPrimitive("US"))
                        put("language", JsonPrimitive("en"))
                        put("app_version", JsonPrimitive("1.2.3"))
                    },
                "event_list" to
                    JsonArray(
                        listOf(
                            JsonPrimitive("app_open"),
                            JsonPrimitive("paywall_view"),
                            JsonPrimitive("purchase_attempt"),
                        ),
                    ),
            )

        // Create a sample RedeemRequest
        val redeemRequest =
            RedeemRequest(
                deviceId = "device_123",
                userId = "user_456",
                aliasId = "alias_789",
                codes = emptyList(), // Empty for simplicity
                receipts =
                    listOf(
                        TransactionReceipt(
                            purchaseToken = "purchase_token_abc123",
                            orderId = "order_id_def456",
                        ),
                    ),
                externalAccountId = "external_account_xyz",
                metadata = attributionProps,
            )

        // Serialize to JSON
        val jsonOutput = json.encodeToString(redeemRequest)

        // Print the result
        println("=== Attribution Props Serialization Example ===")
        println(jsonOutput)
    }

    @Test
    fun `show how empty attribution props are serialized`() {
        val redeemRequest =
            RedeemRequest(
                deviceId = "device_123",
                userId = "user_456",
                aliasId = "alias_789",
                codes = emptyList(),
                receipts =
                    listOf(
                        TransactionReceipt(
                            purchaseToken = "purchase_token_abc123",
                            orderId = "order_id_def456",
                        ),
                    ),
                externalAccountId = "external_account_xyz",
                metadata = null, // This is what happens with empty attribution props
            )

        val jsonOutput = json.encodeToString(redeemRequest)

        println("=== Empty Attribution Props Serialization ===")
        println(jsonOutput)
    }
}
