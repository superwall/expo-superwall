@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode.models

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperwallProductSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun `deserializes full product response`() {
        Given("a JSON response with products") {
            val jsonString =
                """
                {
                  "data": [
                    {
                      "object": "product",
                      "identifier": "com.test.monthly",
                      "platform": "android",
                      "price": { "amount": 999, "currency": "USD" },
                      "subscription": { "period": "month", "period_count": 1, "trial_period_days": 7 },
                      "entitlements": [{ "identifier": "premium", "type": "entitlement" }]
                    }
                  ]
                }
                """.trimIndent()

            When("the response is deserialized") {
                val response = json.decodeFromString<SuperwallProductsResponse>(jsonString)

                Then("the products are parsed correctly") {
                    assertEquals(1, response.data.size)
                    val product = response.data[0]
                    assertEquals("product", product.objectType)
                    assertEquals("com.test.monthly", product.identifier)
                    assertEquals(SuperwallProductPlatform.ANDROID, product.platform)
                    assertEquals(999, product.price!!.amount)
                    assertEquals("USD", product.price!!.currency)
                    assertEquals(SuperwallSubscriptionPeriod.MONTH, product.subscription!!.period)
                    assertEquals(1, product.subscription!!.periodCount)
                    assertEquals(7, product.subscription!!.trialPeriodDays)
                    assertEquals(1, product.entitlements.size)
                    assertEquals("premium", product.entitlements[0].identifier)
                }
            }
        }
    }

    @Test
    fun `deserializes product without subscription`() {
        Given("a JSON product without subscription field") {
            val jsonString =
                """
                {
                  "identifier": "com.test.lifetime",
                  "platform": "android",
                  "price": { "amount": 4999, "currency": "EUR" }
                }
                """.trimIndent()

            When("the product is deserialized") {
                val product = json.decodeFromString<SuperwallProduct>(jsonString)

                Then("subscription is null and price is correct") {
                    assertEquals("com.test.lifetime", product.identifier)
                    assertNull(product.subscription)
                    assertEquals(4999, product.price!!.amount)
                    assertEquals("EUR", product.price!!.currency)
                }
            }
        }
    }

    @Test
    fun `deserializes product with all subscription periods`() {
        Given("products with each subscription period type") {
            val periods = listOf("day", "week", "month", "year")
            val expected =
                listOf(
                    SuperwallSubscriptionPeriod.DAY,
                    SuperwallSubscriptionPeriod.WEEK,
                    SuperwallSubscriptionPeriod.MONTH,
                    SuperwallSubscriptionPeriod.YEAR,
                )

            Then("each period deserializes correctly") {
                periods.forEachIndexed { index, periodStr ->
                    val jsonString =
                        """
                        {
                          "identifier": "prod-$periodStr",
                          "platform": "android",
                          "price": { "amount": 100, "currency": "USD" },
                          "subscription": { "period": "$periodStr" }
                        }
                        """.trimIndent()
                    val product = json.decodeFromString<SuperwallProduct>(jsonString)
                    assertEquals(expected[index], product.subscription!!.period)
                }
            }
        }
    }

    @Test
    fun `deserializes all platform types`() {
        Given("products with each platform") {
            val platforms =
                mapOf(
                    "ios" to SuperwallProductPlatform.IOS,
                    "android" to SuperwallProductPlatform.ANDROID,
                    "stripe" to SuperwallProductPlatform.STRIPE,
                    "paddle" to SuperwallProductPlatform.PADDLE,
                    "superwall" to SuperwallProductPlatform.SUPERWALL,
                )

            Then("each platform deserializes correctly") {
                platforms.forEach { (str, expected) ->
                    val jsonString =
                        """
                        {
                          "identifier": "prod",
                          "platform": "$str",
                          "price": { "amount": 100, "currency": "USD" }
                        }
                        """.trimIndent()
                    val product = json.decodeFromString<SuperwallProduct>(jsonString)
                    assertEquals(expected, product.platform)
                }
            }
        }
    }

    @Test
    fun `deserializes subscription with default period_count`() {
        Given("a subscription JSON without period_count") {
            val jsonString =
                """
                {
                  "identifier": "prod",
                  "platform": "android",
                  "price": { "amount": 100, "currency": "USD" },
                  "subscription": { "period": "month" }
                }
                """.trimIndent()

            When("the product is deserialized") {
                val product = json.decodeFromString<SuperwallProduct>(jsonString)

                Then("periodCount defaults to 1") {
                    assertEquals(1, product.subscription!!.periodCount)
                }
            }
        }
    }

    @Test
    fun `deserializes subscription without trial_period_days`() {
        Given("a subscription JSON without trial_period_days") {
            val jsonString =
                """
                {
                  "identifier": "prod",
                  "platform": "android",
                  "price": { "amount": 100, "currency": "USD" },
                  "subscription": { "period": "year", "period_count": 1 }
                }
                """.trimIndent()

            When("the product is deserialized") {
                val product = json.decodeFromString<SuperwallProduct>(jsonString)

                Then("trialPeriodDays is null") {
                    assertNull(product.subscription!!.trialPeriodDays)
                }
            }
        }
    }

    @Test
    fun `deserializes entitlements with optional type`() {
        Given("entitlements with and without type") {
            val jsonString =
                """
                {
                  "identifier": "prod",
                  "platform": "android",
                  "price": { "amount": 100, "currency": "USD" },
                  "entitlements": [
                    { "identifier": "premium", "type": "entitlement" },
                    { "identifier": "pro" }
                  ]
                }
                """.trimIndent()

            When("the product is deserialized") {
                val product = json.decodeFromString<SuperwallProduct>(jsonString)

                Then("entitlements have correct identifiers and types") {
                    assertEquals(2, product.entitlements.size)
                    assertEquals("premium", product.entitlements[0].identifier)
                    assertEquals("entitlement", product.entitlements[0].type)
                    assertEquals("pro", product.entitlements[1].identifier)
                    assertNull(product.entitlements[1].type)
                }
            }
        }
    }

    @Test
    fun `deserializes empty data array`() {
        Given("a response with empty data array") {
            val jsonString = """{ "data": [] }"""

            When("the response is deserialized") {
                val response = json.decodeFromString<SuperwallProductsResponse>(jsonString)

                Then("data list is empty") {
                    assertTrue(response.data.isEmpty())
                }
            }
        }
    }

    @Test
    fun `ignores unknown fields in product`() {
        Given("a JSON product with extra unknown fields") {
            val jsonString =
                """
                {
                  "identifier": "prod",
                  "platform": "android",
                  "price": { "amount": 100, "currency": "USD" },
                  "some_unknown_field": "value",
                  "another_one": 42
                }
                """.trimIndent()

            When("the product is deserialized") {
                val product = json.decodeFromString<SuperwallProduct>(jsonString)

                Then("product is parsed without error") {
                    assertEquals("prod", product.identifier)
                }
            }
        }
    }
}
