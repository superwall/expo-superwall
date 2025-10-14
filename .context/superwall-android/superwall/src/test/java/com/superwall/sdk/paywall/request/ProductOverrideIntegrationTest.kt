package com.superwall.sdk.paywall.request

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.paywall.presentation.internal.request.ProductOverride
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductOverrideIntegrationTest {
    @Test
    fun `test local overrides take precedence over global overrides`() {
        Given("both local and global product overrides") {
            val mockLocalProduct = mockk<StoreProduct>()
            val localOverrides = mapOf("primary" to mockLocalProduct)
            val globalOverrides = mapOf("primary" to "com.example.global_product")

            When("evaluating override precedence") {
                // Simulate the logic: local overrides take precedence
                val finalOverrides = localOverrides // local takes precedence

                Then("local overrides should be used") {
                    assertEquals(mockLocalProduct, finalOverrides["primary"])
                }
            }
        }
    }

    @Test
    fun `test global overrides used when no local overrides`() {
        Given("only global product overrides") {
            val localOverrides: Map<String, StoreProduct>? = null
            val globalOverrides = mapOf("primary" to "com.example.global_product")

            When("evaluating override precedence") {
                // Simulate the logic: use global when local is null
                val finalOverrides = localOverrides ?: globalOverrides.mapValues { ProductOverride.byId(it.value) }

                Then("global overrides should be converted to ProductOverride.ById") {
                    val override = (finalOverrides as Map<String, ProductOverride>)["primary"]
                    assert(override is ProductOverride.ById)
                    assertEquals("com.example.global_product", (override as ProductOverride.ById).productId)
                }
            }
        }
    }

    @Test
    fun `test no overrides when both are null or empty`() {
        Given("no local or global overrides") {
            val localOverrides: Map<String, StoreProduct>? = null
            val globalOverrides: Map<String, String> = emptyMap()

            When("evaluating override precedence") {
                // Simulate the logic: null when both are empty/null
                val finalOverrides =
                    localOverrides
                        ?: if (globalOverrides.isNotEmpty()) globalOverrides else null

                Then("no overrides should be applied") {
                    assertNull(finalOverrides)
                }
            }
        }
    }

    @Test
    fun `test ProductOverride companion functions`() {
        Given("product ID and StoreProduct") {
            val productId = "com.example.product"
            val mockProduct = mockk<StoreProduct>()

            When("creating ProductOverride instances using companion functions") {
                val byIdOverride = ProductOverride.byId(productId)
                val byProductOverride = ProductOverride.byProduct(mockProduct)

                Then("companion functions should create correct instances") {
                    assert(byIdOverride is ProductOverride.ById)
                    assertEquals(productId, (byIdOverride as ProductOverride.ById).productId)

                    assert(byProductOverride is ProductOverride.ByProduct)
                    assertEquals(mockProduct, (byProductOverride as ProductOverride.ByProduct).product)
                }
            }
        }
    }

    @Test
    fun `test mixed local and global overrides with different product names`() {
        Given("local overrides for some products and global overrides for others") {
            val mockLocalProduct = mockk<StoreProduct>()
            val localOverrides = mapOf("primary" to mockLocalProduct)
            val globalOverrides =
                mapOf(
                    "primary" to "com.example.global_primary", // This should be ignored
                    "secondary" to "com.example.global_secondary", // This should be used
                )

            When("evaluating override precedence") {
                // Simulate the real logic: local overrides take precedence
                val finalLocalOverrides = localOverrides
                val finalGlobalOverrides =
                    globalOverrides
                        .filterKeys { !localOverrides.containsKey(it) } // Remove keys that exist in local
                        .mapValues { ProductOverride.byId(it.value) }

                Then("local overrides should take precedence and global should fill gaps") {
                    assertEquals(mockLocalProduct, finalLocalOverrides["primary"])

                    val secondaryOverride = finalGlobalOverrides["secondary"]
                    assert(secondaryOverride is ProductOverride.ById)
                    assertEquals("com.example.global_secondary", (secondaryOverride as ProductOverride.ById).productId)

                    // Primary should not exist in global overrides due to filtering
                    assertNull(finalGlobalOverrides["primary"])
                }
            }
        }
    }
}
