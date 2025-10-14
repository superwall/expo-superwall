package com.superwall.sdk.models.paywall

import com.superwall.sdk.models.product.ProductItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.Test

class PaywallProductTest {
    @Test
    fun `test parsing of config`() {
        val productString =
            """
            {
                "reference_name": "primary",
                "store_product": {
                    "store": "PLAY_STORE",
                    "product_identifier": "abc-def",
                    "base_plan_identifier": "ghi",
                    "offer": {
                        "type": "SPECIFIED",
                        "offer_identifier": "jkl"
                    }
                }
            }
            """.trimIndent()

        val json =
            Json {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            }
        val product = json.decodeFromString<ProductItem>(productString)
        assert(product != null)
        assert(product.fullProductId == "abc-def:ghi:jkl")
        assert(product.name == "primary")
        assert(product.entitlements.isEmpty())
    }
}
