package com.superwall.sdk.paywall.presentation.internal.request

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallOverridesTest {
    @Test
    fun `test productOverridesByName converts productsByName to ProductOverride ByProduct`() {
        Given("a PaywallOverrides with multiple products") {
            val mockProduct1 = mockk<StoreProduct>(relaxed = true)
            val mockProduct2 = mockk<StoreProduct>(relaxed = true)
            val productsByName =
                mapOf(
                    "primary" to mockProduct1,
                    "secondary" to mockProduct2,
                )
            val paywallOverrides = PaywallOverrides(productsByName = productsByName)

            When("accessing productOverridesByName") {
                val productOverrides = paywallOverrides.productOverridesByName

                Then("it should convert all products to ProductOverride.ByProduct") {
                    assertEquals(2, productOverrides.size)
                    assertTrue(productOverrides["primary"] is ProductOverride.ByProduct)
                    assertTrue(productOverrides["secondary"] is ProductOverride.ByProduct)
                    assertEquals(mockProduct1, (productOverrides["primary"] as ProductOverride.ByProduct).product)
                    assertEquals(mockProduct2, (productOverrides["secondary"] as ProductOverride.ByProduct).product)
                }
            }
        }
    }

    @Test
    fun `test productOverridesByName with empty productsByName`() {
        Given("a PaywallOverrides with empty productsByName") {
            val paywallOverrides = PaywallOverrides(productsByName = emptyMap())

            When("accessing productOverridesByName") {
                val productOverrides = paywallOverrides.productOverridesByName

                Then("it should return empty map") {
                    assertTrue(productOverrides.isEmpty())
                }
            }
        }
    }

    @Test
    fun `test productOverridesByName with single product`() {
        Given("a PaywallOverrides with single product") {
            val mockProduct = mockk<StoreProduct>(relaxed = true)
            val productsByName = mapOf("primary" to mockProduct)
            val paywallOverrides = PaywallOverrides(productsByName = productsByName)

            When("accessing productOverridesByName") {
                val productOverrides = paywallOverrides.productOverridesByName

                Then("it should convert the product to ProductOverride.ByProduct") {
                    assertEquals(1, productOverrides.size)
                    assertTrue(productOverrides["primary"] is ProductOverride.ByProduct)
                    assertEquals(mockProduct, (productOverrides["primary"] as ProductOverride.ByProduct).product)
                }
            }
        }
    }
}
