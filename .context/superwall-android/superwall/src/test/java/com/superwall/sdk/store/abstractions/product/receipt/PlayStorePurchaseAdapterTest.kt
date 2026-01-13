package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PlayStorePurchaseAdapter, specifically covering the bug where
 * productId was set to the raw product ID instead of the full identifier.
 */
class PlayStorePurchaseAdapterTest {
    // MARK: - Helper Methods

    private fun createMockPurchase(
        products: List<String> = listOf("com.ui_tests.monthly"),
        orderId: String? = "GPA.3382-0986-5088-93164",
        purchaseToken: String = "test-token",
        purchaseTime: Long = System.currentTimeMillis(),
        purchaseState: Int = PurchaseState.PURCHASED,
        isAutoRenewing: Boolean = true,
    ): Purchase {
        val purchase = mockk<Purchase>()
        every { purchase.products } returns products
        every { purchase.orderId } returns orderId
        every { purchase.purchaseToken } returns purchaseToken
        every { purchase.purchaseTime } returns purchaseTime
        every { purchase.purchaseState } returns purchaseState
        every { purchase.isAutoRenewing } returns isAutoRenewing
        return purchase
    }

    private fun createMockStoreProduct(
        fullIdentifier: String = "com.ui_tests.monthly:monthly_plan:sw-auto",
        productIdentifier: String = "com.ui_tests.monthly",
        productType: String = ProductType.SUBS,
    ): StoreProduct {
        val productDetails = mockk<ProductDetails>()
        every { productDetails.productType } returns productType

        val rawStoreProduct = mockk<RawStoreProduct>()
        every { rawStoreProduct.fullIdentifier } returns fullIdentifier
        every { rawStoreProduct.productIdentifier } returns productIdentifier
        every { rawStoreProduct.underlyingProductDetails } returns productDetails
        every { rawStoreProduct.subscriptionPeriod } returns null

        val storeProduct = mockk<StoreProduct>()
        every { storeProduct.fullIdentifier } returns fullIdentifier
        every { storeProduct.productIdentifier } returns productIdentifier
        every { storeProduct.rawStoreProduct } returns rawStoreProduct

        return storeProduct
    }

    // MARK: - Bug Reproduction Tests

    /**
     * This test verifies the FIX works correctly:
     * 1. productsById is keyed by raw productIdentifier
     * 2. Lookup succeeds using raw productId from purchase.products
     * 3. adapter.productId is set to fullIdentifier for entitlement matching
     */
    @Test
    fun `fromPurchase finds product and uses full identifier for entitlement matching`() {
        Given("a purchase with raw product ID and products map keyed by raw product ID") {
            val rawProductId = "com.ui_tests.monthly"
            val fullProductId = "com.ui_tests.monthly:monthly_plan:sw-auto"

            val purchase = createMockPurchase(products = listOf(rawProductId))
            val storeProduct =
                createMockStoreProduct(
                    fullIdentifier = fullProductId,
                    productIdentifier = rawProductId,
                )

            // After fix: productsById is keyed by raw productIdentifier
            val productsById = mapOf(rawProductId to storeProduct)

            When("creating adapter with fromPurchase") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)

                Then("adapter is created") {
                    assertEquals(1, adapters.size)
                }

                And("product is found successfully") {
                    val adapter = adapters.first()
                    // Product lookup succeeds, so productType should not be CONSUMABLE
                    assertEquals(
                        "Product should be found, productType should be AUTO_RENEWABLE",
                        EntitlementTransactionType.AUTO_RENEWABLE,
                        adapter.productType,
                    )
                }

                And("productId is full identifier for entitlement matching") {
                    val adapter = adapters.first()
                    assertEquals(
                        "adapter.productId should be fullIdentifier for entitlement matching",
                        fullProductId,
                        adapter.productId,
                    )
                }
            }
        }
    }

    /**
     * This test shows what happens when the product is NOT found in productsById:
     * - productType defaults to CONSUMABLE (because product is null)
     * - expirationDate is null (because product is null)
     * - adapter.productId falls back to raw productId
     */
    @Test
    fun `when product not found - productId falls back to raw ID and productType defaults to CONSUMABLE`() {
        Given("a subscription purchase where product is not in productsById") {
            val rawProductId = "com.ui_tests.monthly"

            val purchase =
                createMockPurchase(
                    products = listOf(rawProductId),
                    isAutoRenewing = true,
                )

            // Empty products map - product will not be found
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter with fromPurchase") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("productId falls back to raw productId") {
                    assertEquals(
                        "When product not found, productId should fall back to raw productId",
                        rawProductId,
                        adapter.productId,
                    )
                }

                And("productType is CONSUMABLE (default when product is null)") {
                    assertEquals(
                        "productType defaults to CONSUMABLE when product is null",
                        EntitlementTransactionType.CONSUMABLE,
                        adapter.productType,
                    )
                }

                And("expirationDate is null") {
                    assertNull(
                        "expirationDate is null because product is null",
                        adapter.expirationDate,
                    )
                }
            }
        }
    }

    // MARK: - Full Integration Tests

    /**
     * This test verifies that one-time purchases (INAPP) work correctly.
     * For INAPP products, fullIdentifier == productIdentifier == raw product ID.
     */
    @Test
    fun `one-time purchase - fullIdentifier equals productIdentifier`() {
        Given("a one-time purchase where fullIdentifier equals productIdentifier") {
            val productId = "com.test.lifetime"

            val purchase =
                createMockPurchase(
                    products = listOf(productId),
                    isAutoRenewing = false,
                )
            val storeProduct =
                createMockStoreProduct(
                    // For INAPP, fullIdentifier == productIdentifier
                    fullIdentifier = productId,
                    productIdentifier = productId,
                    productType = ProductType.INAPP,
                )

            // productsById keyed by productIdentifier
            val productsById = mapOf(productId to storeProduct)

            // serverEntitlementsByProductId keyed by fullIdentifier (same as productIdentifier for INAPP)
            val serverEntitlementsByProductId =
                mapOf(
                    productId to setOf("lifetime_access"),
                )

            When("creating adapter and looking up entitlements") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("product is found") {
                    assertEquals(
                        "Product should be found, productType should be NON_CONSUMABLE",
                        EntitlementTransactionType.NON_CONSUMABLE,
                        adapter.productType,
                    )
                }

                And("adapter.productId equals productIdentifier") {
                    assertEquals(productId, adapter.productId)
                }

                And("entitlement lookup succeeds") {
                    val entitlements = serverEntitlementsByProductId[adapter.productId]
                    assertNotNull(entitlements)
                    assertTrue(entitlements?.contains("lifetime_access") ?: false)
                }
            }
        }
    }

    /**
     * This test verifies the complete flow works for entitlement matching:
     * 1. productsById keyed by raw productIdentifier
     * 2. Product lookup succeeds
     * 3. adapter.productId is fullIdentifier
     * 4. Entitlement lookup by adapter.productId succeeds
     */
    @Test
    fun `full integration - adapter productId matches entitlement lookup key`() {
        Given("a purchase with products map keyed by raw product ID") {
            val rawProductId = "com.ui_tests.monthly"
            val fullProductId = "com.ui_tests.monthly:monthly_plan:sw-auto"

            val purchase = createMockPurchase(products = listOf(rawProductId))
            val storeProduct =
                createMockStoreProduct(
                    fullIdentifier = fullProductId,
                    productIdentifier = rawProductId,
                    productType = ProductType.SUBS,
                )

            // productsById keyed by raw product ID (what purchase.products returns)
            val productsById = mapOf(rawProductId to storeProduct)

            // serverEntitlementsByProductId keyed by full product ID (from server config)
            val serverEntitlementsByProductId =
                mapOf(
                    fullProductId to setOf("default", "pro"),
                )

            When("creating adapter and looking up entitlements") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("product is found") {
                    assertEquals(
                        "Product should be found, so productType should be AUTO_RENEWABLE",
                        EntitlementTransactionType.AUTO_RENEWABLE,
                        adapter.productType,
                    )
                }

                And("adapter.productId is fullIdentifier") {
                    assertEquals(fullProductId, adapter.productId)
                }

                And("entitlement lookup succeeds using adapter.productId") {
                    val entitlements = serverEntitlementsByProductId[adapter.productId]
                    assertNotNull(
                        "Entitlement lookup should succeed using adapter.productId",
                        entitlements,
                    )
                    assertEquals(2, entitlements?.size)
                    assertTrue(entitlements?.contains("default") ?: false)
                    assertTrue(entitlements?.contains("pro") ?: false)
                }
            }
        }
    }

    /**
     * Tests that when product is found, productType is correctly determined.
     */
    @Test
    fun `productType is AUTO_RENEWABLE when product is subscription and autoRenewing`() {
        Given("a subscription purchase with product found") {
            val rawProductId = "com.ui_tests.monthly"

            val purchase =
                createMockPurchase(
                    products = listOf(rawProductId),
                    isAutoRenewing = true,
                )
            val storeProduct =
                createMockStoreProduct(
                    fullIdentifier = rawProductId, // Same as raw for this test
                    productIdentifier = rawProductId,
                    productType = ProductType.SUBS,
                )

            val productsById = mapOf(rawProductId to storeProduct)

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("productType is AUTO_RENEWABLE") {
                    assertEquals(EntitlementTransactionType.AUTO_RENEWABLE, adapter.productType)
                }
            }
        }
    }

    /**
     * Tests that when product is found but not autoRenewing, productType is NON_RENEWABLE.
     */
    @Test
    fun `productType is NON_RENEWABLE when product is subscription but not autoRenewing`() {
        Given("a subscription purchase that is not auto-renewing") {
            val rawProductId = "com.ui_tests.monthly"

            val purchase =
                createMockPurchase(
                    products = listOf(rawProductId),
                    isAutoRenewing = false,
                )
            val storeProduct =
                createMockStoreProduct(
                    fullIdentifier = rawProductId,
                    productIdentifier = rawProductId,
                    productType = ProductType.SUBS,
                )

            val productsById = mapOf(rawProductId to storeProduct)

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("productType is NON_RENEWABLE") {
                    assertEquals(EntitlementTransactionType.NON_RENEWABLE, adapter.productType)
                }
            }
        }
    }

    /**
     * Tests that INAPP products are treated as NON_CONSUMABLE.
     */
    @Test
    fun `productType is NON_CONSUMABLE when product is INAPP`() {
        Given("an in-app purchase") {
            val rawProductId = "com.ui_tests.lifetime"

            val purchase = createMockPurchase(products = listOf(rawProductId))
            val storeProduct =
                createMockStoreProduct(
                    fullIdentifier = rawProductId,
                    productIdentifier = rawProductId,
                    productType = ProductType.INAPP,
                )

            val productsById = mapOf(rawProductId to storeProduct)

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("productType is NON_CONSUMABLE") {
                    assertEquals(EntitlementTransactionType.NON_CONSUMABLE, adapter.productType)
                }
            }
        }
    }

    // MARK: - Multiple Products Tests

    /**
     * Tests that multiple products in a single purchase create multiple adapters.
     */
    @Test
    fun `fromPurchase creates adapter for each product in purchase`() {
        Given("a purchase with multiple products") {
            val productIds = listOf("com.test.product1", "com.test.product2", "com.test.product3")
            val purchase = createMockPurchase(products = productIds)

            val productsById =
                productIds.associateWith { id ->
                    createMockStoreProduct(fullIdentifier = id, productIdentifier = id)
                }

            When("creating adapters") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)

                Then("one adapter is created for each product") {
                    assertEquals(3, adapters.size)
                }

                And("each adapter has correct productId") {
                    productIds.forEachIndexed { index, productId ->
                        assertEquals(productId, adapters[index].productId)
                    }
                }
            }
        }
    }

    // MARK: - Transaction Properties Tests

    /**
     * Tests that transactionId uses orderId when available.
     */
    @Test
    fun `transactionId uses orderId when available`() {
        Given("a purchase with orderId") {
            val orderId = "GPA.1234-5678-9012-34567"
            val purchase = createMockPurchase(orderId = orderId)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("transactionId is the orderId") {
                    assertEquals(orderId, adapter.transactionId)
                }
            }
        }
    }

    /**
     * Tests that transactionId falls back to purchaseToken when orderId is null.
     */
    @Test
    fun `transactionId uses purchaseToken when orderId is null`() {
        Given("a purchase without orderId") {
            val purchaseToken = "test-purchase-token-abc123"
            val purchase = createMockPurchase(orderId = null, purchaseToken = purchaseToken)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("transactionId is the purchaseToken") {
                    assertEquals(purchaseToken, adapter.transactionId)
                }
            }
        }
    }

    /**
     * Tests isActive calculation for purchased state.
     */
    @Test
    fun `isActive is true when purchaseState is PURCHASED and not expired`() {
        Given("a purchased subscription") {
            val purchase = createMockPurchase(purchaseState = PurchaseState.PURCHASED)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("isActive is true") {
                    // When product is null, expirationDate is null, so isActive should be true
                    assertTrue(adapter.isActive)
                }
            }
        }
    }

    /**
     * Tests isActive is false when purchase is pending.
     */
    @Test
    fun `isActive is false when purchaseState is PENDING`() {
        Given("a pending purchase") {
            val purchase = createMockPurchase(purchaseState = PurchaseState.PENDING)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapters = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById)
                val adapter = adapters.first()

                Then("isActive is false") {
                    assertFalse(adapter.isActive)
                }
            }
        }
    }

    /**
     * Tests willRenew matches purchase.isAutoRenewing.
     */
    @Test
    fun `willRenew matches purchase isAutoRenewing`() {
        Given("an auto-renewing purchase") {
            val purchase = createMockPurchase(isAutoRenewing = true)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("willRenew is true") {
                    assertTrue(adapter.willRenew)
                }
            }
        }

        Given("a non-auto-renewing purchase") {
            val purchase = createMockPurchase(isAutoRenewing = false)
            val productsById = emptyMap<String, StoreProduct>()

            When("creating adapter") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("willRenew is false") {
                    assertFalse(adapter.willRenew)
                }
            }
        }
    }
}
