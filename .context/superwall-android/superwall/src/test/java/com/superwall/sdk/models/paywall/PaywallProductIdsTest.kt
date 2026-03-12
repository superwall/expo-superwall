package com.superwall.sdk.models.paywall

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Paywall.productIds] initialization behavior.
 *
 * The issue: When the server sends only `products_v3` (not `products_v2`),
 * `productIds` should still be populated from `products_v3` data.
 * Otherwise, no products would be fetched from Google Play.
 */
class PaywallProductIdsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // Base paywall fields required for deserialization
    private fun basePaywallFields() =
        """
        "id": "test-id",
        "identifier": "test-paywall",
        "name": "Test Paywall",
        "url": "https://example.com",
        "paywalljs_event": "",
        "presentation_style_v2": null,
        "presentation_style_v3": null,
        "presentation_delay": 0,
        "presentation_condition": "CHECK_USER_SUBSCRIPTION",
        "background_color_hex": "#FFFFFF",
        "cache_key": "test-cache-key",
        "build_id": "test-build"
        """.trimIndent()

    @Test
    fun `productIds populated from products_v2 when both v2 and v3 exist`() {
        // Given: A paywall JSON with both products_v2 and products_v3
        val paywallJson =
            """
            {
                ${basePaywallFields()},
                "products_v2": [
                    {
                        "reference_name": "primary",
                        "sw_composite_product_id": "product1:plan1:offer1",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product1",
                            "base_plan_identifier": "plan1",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "offer1"}
                        }
                    }
                ],
                "products_v3": [
                    {
                        "sw_composite_product_id": "product1:plan1:offer1",
                        "reference_name": "primary",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product1",
                            "base_plan_identifier": "plan1",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "offer1"}
                        },
                        "entitlements": []
                    }
                ]
            }
            """.trimIndent()

        // When: Parsing the paywall
        val paywall = json.decodeFromString<Paywall>(paywallJson)

        // Then: productIds should be populated from products_v2
        assertEquals(1, paywall.productIds.size)
        assertEquals("product1:plan1:offer1", paywall.productIds[0])
    }

    @Test
    fun `productIds populated from products_v3 when products_v2 is empty`() {
        // Given: A paywall JSON with only products_v3 (products_v2 is empty)
        val paywallJson =
            """
            {
                ${basePaywallFields()},
                "products_v2": [],
                "products_v3": [
                    {
                        "sw_composite_product_id": "product1:plan1:offer1",
                        "reference_name": "primary",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product1",
                            "base_plan_identifier": "plan1",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "offer1"}
                        },
                        "entitlements": []
                    },
                    {
                        "sw_composite_product_id": "product1:plan2:offer2",
                        "reference_name": "secondary",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product1",
                            "base_plan_identifier": "plan2",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "offer2"}
                        },
                        "entitlements": []
                    }
                ]
            }
            """.trimIndent()

        // When: Parsing the paywall
        val paywall = json.decodeFromString<Paywall>(paywallJson)

        // Then: productIds should be populated from products_v3 (fallback)
        assertEquals(2, paywall.productIds.size)
        assertTrue(paywall.productIds.contains("product1:plan1:offer1"))
        assertTrue(paywall.productIds.contains("product1:plan2:offer2"))
    }

    @Test
    fun `productIds populated from products_v3 when products_v2 is missing`() {
        // Given: A paywall JSON without products_v2 field at all
        val paywallJson =
            """
            {
                ${basePaywallFields()},
                "products_v3": [
                    {
                        "sw_composite_product_id": "product1:monthly:trial",
                        "reference_name": "monthly",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product1",
                            "base_plan_identifier": "monthly",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "trial"}
                        },
                        "entitlements": []
                    }
                ]
            }
            """.trimIndent()

        // When: Parsing the paywall
        val paywall = json.decodeFromString<Paywall>(paywallJson)

        // Then: productIds should be populated from products_v3 (fallback)
        assertEquals(1, paywall.productIds.size)
        assertEquals("product1:monthly:trial", paywall.productIds[0])
    }

    @Test
    fun `productIds correctly populated for multiple products with same productId but different plans`() {
        // Given: A paywall with products that share the same Google productId but different plans
        // This is the scenario that was causing issues: "productid:plan:offer" vs "productid:plan2:offer"
        val paywallJson =
            """
            {
                ${basePaywallFields()},
                "products_v3": [
                    {
                        "sw_composite_product_id": "my_subscription:monthly:free_trial",
                        "reference_name": "Monthly with Trial",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "my_subscription",
                            "base_plan_identifier": "monthly",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "free_trial"}
                        },
                        "entitlements": []
                    },
                    {
                        "sw_composite_product_id": "my_subscription:annual:free_trial",
                        "reference_name": "Annual with Trial",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "my_subscription",
                            "base_plan_identifier": "annual",
                            "offer": {"type": "SPECIFIED", "offer_identifier": "free_trial"}
                        },
                        "entitlements": []
                    }
                ]
            }
            """.trimIndent()

        // When: Parsing the paywall
        val paywall = json.decodeFromString<Paywall>(paywallJson)

        // Then: Both product IDs should be present (different plans for same product)
        assertEquals(2, paywall.productIds.size)
        assertTrue(paywall.productIds.contains("my_subscription:monthly:free_trial"))
        assertTrue(paywall.productIds.contains("my_subscription:annual:free_trial"))

        // And: playStoreProducts should also have both
        assertEquals(2, paywall.playStoreProducts.size)
    }

    @Test
    fun `productIds uses fullProductId computed from store product details`() {
        // Given: A paywall where compositeId might differ from computed fullProductId
        // This tests that the fallback uses fullProductId (computed from store product details)
        val paywallJson =
            """
            {
                ${basePaywallFields()},
                "products_v3": [
                    {
                        "sw_composite_product_id": "product:plan:sw-auto",
                        "reference_name": "Auto Offer",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product",
                            "base_plan_identifier": "plan",
                            "offer": {"type": "AUTOMATIC"}
                        },
                        "entitlements": []
                    },
                    {
                        "sw_composite_product_id": "product:plan:sw-none",
                        "reference_name": "No Offer",
                        "store_product": {
                            "store": "PLAY_STORE",
                            "product_identifier": "product",
                            "base_plan_identifier": "plan",
                            "offer": {"type": "NO_OFFER"}
                        },
                        "entitlements": []
                    }
                ]
            }
            """.trimIndent()

        // When: Parsing the paywall
        val paywall = json.decodeFromString<Paywall>(paywallJson)

        // Then: productIds should contain the compositeIds (which are the fullProductIds)
        assertEquals(2, paywall.productIds.size)
        assertTrue(paywall.productIds.contains("product:plan:sw-auto"))
        assertTrue(paywall.productIds.contains("product:plan:sw-none"))
    }
}
