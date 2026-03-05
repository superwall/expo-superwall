@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.testmode.models.SuperwallEntitlementRef
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.models.SuperwallProductPrice
import com.superwall.sdk.store.testmode.models.SuperwallProductSubscription
import com.superwall.sdk.store.testmode.models.SuperwallSubscriptionPeriod
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestModeTransactionHandlerTest {
    private fun makeTestModeManager(): TestModeManager {
        val manager = TestModeManager(mockk(relaxed = true))
        // Activate test mode so session data can be written
        val config = mockk<com.superwall.sdk.models.config.Config>(relaxed = true)
        manager.evaluateTestMode(config, "com.app", null, null, TestModeBehavior.ALWAYS)
        return manager
    }

    private fun makeProduct(
        identifier: String = "com.test.monthly",
        entitlements: List<SuperwallEntitlementRef> = listOf(SuperwallEntitlementRef("premium", null)),
    ): SuperwallProduct =
        SuperwallProduct(
            objectType = "product",
            identifier = identifier,
            platform = SuperwallProductPlatform.ANDROID,
            price = SuperwallProductPrice(amount = 999, currency = "USD"),
            subscription =
                SuperwallProductSubscription(
                    period = SuperwallSubscriptionPeriod.MONTH,
                    periodCount = 1,
                ),
            entitlements = entitlements,
        )

    @Test
    fun `findSuperwallProductForId returns matching product`() {
        Given("a TestModeTransactionHandler with products") {
            val manager = makeTestModeManager()
            manager.setProducts(
                listOf(
                    makeProduct("com.test.monthly"),
                    makeProduct("com.test.yearly"),
                ),
            )
            val handler = TestModeTransactionHandler(manager, mockk(relaxed = true))

            When("findSuperwallProductForId is called with a known id") {
                val product = handler.findSuperwallProductForId("com.test.yearly")

                Then("the matching product is returned") {
                    assertNotNull(product)
                    assertEquals("com.test.yearly", product!!.identifier)
                }
            }
        }
    }

    @Test
    fun `findSuperwallProductForId returns null for unknown id`() {
        Given("a TestModeTransactionHandler with products") {
            val manager = makeTestModeManager()
            manager.setProducts(listOf(makeProduct("com.test.monthly")))
            val handler = TestModeTransactionHandler(manager, mockk(relaxed = true))

            When("findSuperwallProductForId is called with an unknown id") {
                val product = handler.findSuperwallProductForId("com.unknown.product")

                Then("null is returned") {
                    assertNull(product)
                }
            }
        }
    }

    @Test
    fun `entitlementsForProduct returns entitlement set`() {
        Given("a handler with products that have entitlements") {
            val manager = makeTestModeManager()
            manager.setProducts(
                listOf(
                    makeProduct(
                        "com.test.pro",
                        entitlements =
                            listOf(
                                SuperwallEntitlementRef("premium", null),
                                SuperwallEntitlementRef("ads_free", null),
                            ),
                    ),
                ),
            )
            val handler = TestModeTransactionHandler(manager, mockk(relaxed = true))

            When("entitlementsForProduct is called") {
                val entitlements = handler.entitlementsForProduct("com.test.pro")

                Then("it returns Entitlement set with correct identifiers") {
                    assertEquals(2, entitlements.size)
                    assertTrue(entitlements.contains(Entitlement("premium")))
                    assertTrue(entitlements.contains(Entitlement("ads_free")))
                }
            }
        }
    }

    @Test
    fun `entitlementsForProduct returns empty set for unknown product`() {
        Given("a handler with products") {
            val manager = makeTestModeManager()
            manager.setProducts(listOf(makeProduct("com.test.monthly")))
            val handler = TestModeTransactionHandler(manager, mockk(relaxed = true))

            When("entitlementsForProduct is called with unknown id") {
                val entitlements = handler.entitlementsForProduct("com.unknown")

                Then("empty set is returned") {
                    assertTrue(entitlements.isEmpty())
                }
            }
        }
    }

    @Test
    fun `entitlementsForProduct returns empty set for product without entitlements`() {
        Given("a handler with a product that has no entitlements") {
            val manager = makeTestModeManager()
            manager.setProducts(
                listOf(
                    makeProduct("com.test.basic", entitlements = emptyList()),
                ),
            )
            val handler = TestModeTransactionHandler(manager, mockk(relaxed = true))

            When("entitlementsForProduct is called") {
                val entitlements = handler.entitlementsForProduct("com.test.basic")

                Then("empty set is returned") {
                    assertTrue(entitlements.isEmpty())
                }
            }
        }
    }
}
