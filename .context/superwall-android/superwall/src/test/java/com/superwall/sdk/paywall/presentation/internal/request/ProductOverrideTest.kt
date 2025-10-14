package com.superwall.sdk.paywall.presentation.internal.request

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductOverrideTest {
    @Test
    fun `test ProductOverride ById creation`() {
        Given("a product ID") {
            val productId = "test.product.id"

            When("creating ProductOverride.ById") {
                val override = ProductOverride.ById(productId)

                Then("it should store the product ID") {
                    assertEquals(productId, override.productId)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride ByProduct creation`() {
        Given("a store product") {
            val mockProduct = mockk<StoreProduct>()

            When("creating ProductOverride.ByProduct") {
                val override = ProductOverride.ByProduct(mockProduct)

                Then("it should store the product") {
                    assertEquals(mockProduct, override.product)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride ById equals`() {
        Given("two ProductOverride.ById instances with same product ID") {
            val productId = "test.product.id"
            val override1 = ProductOverride.ById(productId)
            val override2 = ProductOverride.ById(productId)
            val override3 = ProductOverride.ById("different.id")

            When("comparing them") {
                Then("instances with same ID should be equal") {
                    assertEquals(override1, override2)
                }

                Then("instances with different IDs should not be equal") {
                    assert(override1 != override3)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride ByProduct equals`() {
        Given("ProductOverride.ByProduct instances with same and different products") {
            val mockProduct1 = mockk<StoreProduct>()
            val mockProduct2 = mockk<StoreProduct>()
            val override1 = ProductOverride.ByProduct(mockProduct1)
            val override2 = ProductOverride.ByProduct(mockProduct1)
            val override3 = ProductOverride.ByProduct(mockProduct2)

            When("comparing them") {
                Then("instances with same product should be equal") {
                    assertEquals(override1, override2)
                }

                Then("instances with different products should not be equal") {
                    assert(override1 != override3)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride different types are not equal`() {
        Given("ProductOverride.ById and ProductOverride.ByProduct instances") {
            val productId = "test.product.id"
            val mockProduct = mockk<StoreProduct>()
            val byIdOverride = ProductOverride.ById(productId)
            val byProductOverride = ProductOverride.ByProduct(mockProduct)

            When("comparing different types") {
                Then("they should not be equal") {
                    assert(byIdOverride != byProductOverride)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride companion function byId`() {
        Given("a product ID") {
            val productId = "test.product.id"

            When("creating ProductOverride using companion function byId") {
                val override = ProductOverride.byId(productId)

                Then("it should create ProductOverride.ById instance") {
                    assert(override is ProductOverride.ById)
                    assertEquals(productId, (override as ProductOverride.ById).productId)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride companion function byProduct`() {
        Given("a store product") {
            val mockProduct = mockk<StoreProduct>()

            When("creating ProductOverride using companion function byProduct") {
                val override = ProductOverride.byProduct(mockProduct)

                Then("it should create ProductOverride.ByProduct instance") {
                    assert(override is ProductOverride.ByProduct)
                    assertEquals(mockProduct, (override as ProductOverride.ByProduct).product)
                }
            }
        }
    }
}
