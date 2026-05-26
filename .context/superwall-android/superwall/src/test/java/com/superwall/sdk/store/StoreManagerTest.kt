package com.superwall.sdk.store

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.billing.BillingError
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.CrossplatformProduct
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue as junitAssertTrue

class StoreManagerTest {
    private lateinit var purchaseController: InternalPurchaseController
    private lateinit var billing: Billing
    private lateinit var storeManager: StoreManager
    private val entitlementsBasic = setOf(Entitlement("entitlement1"))

    @Before
    fun setup() {
        purchaseController = mockk()
        billing = mockk()
        val receiptManager: com.superwall.sdk.store.abstractions.product.receipt.ReceiptManager = mockk(relaxed = true)
        storeManager =
            StoreManager(
                purchaseController = purchaseController,
                billing = billing,
                receiptManagerFactory = { receiptManager },
                track = {},
            )
    }

    @Test
    fun test_getProductVariables_with_successful_product_fetch() =
        runTest {
            Given("a paywall with product items") {
                val paywall =
                    Paywall.stub().copy(
                        productIds = listOf("product1", "product2"),
                        _productItemsV3 =
                            listOf(
                                CrossplatformProduct(
                                    compositeId = "product1:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item1",
                                ),
                                CrossplatformProduct(
                                    compositeId = "product2:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product2",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item2",
                                ),
                            ),
                    )
                val request = mockk<PaywallRequest>()
                val storeProducts =
                    setOf(
                        mockk<StoreProduct> {
                            every { fullIdentifier } returns "product1:basePlan1:sw-auto"
                            every { attributes } returns mapOf("attr1" to "value1")
                        },
                        mockk<StoreProduct> {
                            every { fullIdentifier } returns "product2:basePlan1:sw-auto"
                            every { attributes } returns mapOf("attr2" to "value2")
                        },
                    )

                coEvery { billing.awaitGetProducts(any()) } returns storeProducts

                When("getProductVariables is called") {
                    val result = storeManager.getProductVariables(paywall, request)

                    Then("it should return the correct product variables") {
                        assertEquals(2, result.size)
                        assertEquals("Item1", result[0].name)
                        assertEquals(mapOf("attr1" to "value1"), result[0].attributes)
                        assertEquals("Item2", result[1].name)
                        assertEquals(mapOf("attr2" to "value2"), result[1].attributes)
                    }
                }
            }
        }

    @Test
    fun test_getProducts_with_substitute_products() =
        runTest {
            Given("a paywall and substitute products") {
                val paywall =
                    Paywall.stub().copy(
                        productIds = listOf("product1", "product2"),
                        _productItemsV3 =
                            listOf(
                                CrossplatformProduct(
                                    compositeId = "product1:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item1",
                                ),
                                CrossplatformProduct(
                                    compositeId = "product2:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product2",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item2",
                                ),
                            ),
                    )
                val substituteProducts =
                    mapOf(
                        "Item1" to
                            mockk<StoreProduct> {
                                every { fullIdentifier } returns "substitute1"
                                every { attributes } returns mapOf("attr1" to "value1")
                            },
                    )

                coEvery { billing.awaitGetProducts(any()) } returns
                    setOf(
                        mockk {
                            every { fullIdentifier } returns "product2"
                            every { attributes } returns mapOf("attr2" to "value2")
                        },
                    )

                When("getProducts is called with substitute products") {
                    val result = storeManager.getProducts(substituteProducts, paywall, null)

                    Then("it should use the substitute product and fetch the remaining product") {
                        assertEquals(2, result.productsByFullId.size)
                        assertTrue(result.productsByFullId.containsKey("substitute1"))
                        assertTrue(result.productsByFullId.containsKey("product2"))
                    }

                    And("it should update the product items accordingly") {
                        assertEquals(2, result.productItems.size)
                        assertEquals("Item1", result.productItems[0].name)
                        assertEquals("Item2", result.productItems[1].name)
                    }
                }
            }
        }

    @Test
    fun `test getProducts with billing error`() =
        runTest {
            Given("a paywall and a billing error") {
                val paywall =
                    Paywall.stub().copy(
                        productIds = listOf("product1"),
                        _productItemsV3 =
                            listOf(
                                CrossplatformProduct(
                                    compositeId = "product1:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item1",
                                ),
                                CrossplatformProduct(
                                    compositeId = "product2:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product2",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item2",
                                ),
                            ),
                    )

                coEvery { billing.awaitGetProducts(any()) } throws
                    BillingError.BillingNotAvailable(
                        "Billing not available",
                    )

                When("getProducts is called") {
                    Then("it should throw a BillingNotAvailable error") {
                        assertThrows(BillingError.BillingNotAvailable::class.java) {
                            runBlocking { storeManager.getProducts(null, paywall, null) }
                        }
                    }
                }
            }
        }

    @Test
    fun `test cached products are returned without re-fetching`() =
        runTest {
            Given("products that were previously fetched") {
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                coEvery { billing.awaitGetProducts(any()) } returns setOf(product)

                // First call fetches from billing
                storeManager.getProductsWithoutPaywall(listOf("product1"))

                When("the same products are requested again") {
                    val result = storeManager.getProductsWithoutPaywall(listOf("product1"))

                    Then("it should return cached products without calling billing again") {
                        assertEquals(product, result["product1"])
                        coVerify(exactly = 1) { billing.awaitGetProducts(any()) }
                    }
                }
            }
        }

    @Test
    fun `test concurrent callers await the same in-flight load`() =
        runTest {
            Given("a product that takes time to load") {
                val billingDeferred = CompletableDeferred<Set<StoreProduct>>()
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                coEvery { billing.awaitGetProducts(any()) } coAnswers { billingDeferred.await() }

                When("two callers request the same product concurrently") {
                    val first = async { storeManager.getProductsWithoutPaywall(listOf("product1")) }
                    val second = async { storeManager.getProductsWithoutPaywall(listOf("product1")) }

                    // Complete the billing call
                    billingDeferred.complete(setOf(product))

                    val result1 = first.await()
                    val result2 = second.await()

                    Then("both should get the product and billing should only be called once") {
                        assertEquals(product, result1["product1"])
                        assertEquals(product, result2["product1"])
                        coVerify(exactly = 1) { billing.awaitGetProducts(any()) }
                    }
                }
            }
        }

    @Test
    fun `test errored products are retried on next fetch`() =
        runTest {
            Given("a product that fails to load the first time") {
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                coEvery { billing.awaitGetProducts(any()) } throws RuntimeException("network error") andThen setOf(product)

                When("the first fetch fails") {
                    try {
                        storeManager.getProductsWithoutPaywall(listOf("product1"))
                    } catch (_: RuntimeException) {
                    }

                    Then("the product should not be cached") {
                        assertNull(storeManager.getProductFromCache("product1"))
                    }

                    And("a retry should fetch from billing again") {
                        val result = storeManager.getProductsWithoutPaywall(listOf("product1"))

                        assertEquals(product, result["product1"])
                        junitAssertTrue(storeManager.hasCached("product1"))
                        coVerify(exactly = 2) { billing.awaitGetProducts(any()) }
                    }
                }
            }
        }

    @Test
    fun `test preload failure then presentation succeeds`() =
        runTest {
            Given("a paywall whose products fail during preload due to service unavailable") {
                val paywall =
                    Paywall.stub().copy(
                        productIds = listOf("product1"),
                        _productItemsV3 =
                            listOf(
                                CrossplatformProduct(
                                    compositeId = "product1:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item1",
                                ),
                            ),
                    )
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1:basePlan1:sw-auto"
                        every { attributes } returns mapOf("attr1" to "value1")
                    }

                // First call simulates a transient billing error (SERVICE_UNAVAILABLE).
                // BillingNotAvailable is terminal and re-thrown by getProducts,
                // so we use a RuntimeException to simulate the transient case.
                coEvery { billing.awaitGetProducts(any()) } throws
                    RuntimeException("Service unavailable") andThen
                    setOf(product)

                When("preload fetches products and fails") {
                    val preloadResult = storeManager.getProducts(paywall = paywall)

                    Then("preload returns empty products since error is swallowed for non-BillingNotAvailable") {
                        junitAssertTrue(preloadResult.productsByFullId.isEmpty())
                    }

                    And("a later presentation retries and succeeds") {
                        val presentResult = storeManager.getProducts(paywall = paywall)

                        assertEquals(1, presentResult.productsByFullId.size)
                        assertEquals(product, presentResult.productsByFullId["product1:basePlan1:sw-auto"])
                        coVerify(exactly = 2) { billing.awaitGetProducts(any()) }
                    }
                }
            }
        }

    @Test
    fun `test failed load does not permanently block subsequent fetches`() =
        runTest {
            Given("a product that fails then succeeds on retry") {
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                coEvery { billing.awaitGetProducts(any()) } throws
                    RuntimeException("billing disconnected") andThen setOf(product)

                When("the first call fails") {
                    val result1 = runCatching { storeManager.getProductsWithoutPaywall(listOf("product1")) }

                    Then("it propagates the error") {
                        junitAssertTrue(result1.isFailure)
                        assertEquals("billing disconnected", result1.exceptionOrNull()?.message)
                    }

                    And("the product is in Error state, not permanently stuck in Loading") {
                        assertNull(storeManager.getProductFromCache("product1"))
                        junitAssertTrue(!storeManager.hasCached("product1"))
                    }

                    And("a subsequent call retries and succeeds") {
                        val result2 = storeManager.getProductsWithoutPaywall(listOf("product1"))

                        assertEquals(product, result2["product1"])
                        junitAssertTrue(storeManager.hasCached("product1"))
                        coVerify(exactly = 2) { billing.awaitGetProducts(any()) }
                    }
                }
            }
        }

    @Test
    fun `test partial product failure does not block successful products`() =
        runTest {
            Given("two products where billing only returns one") {
                val product1 =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                // Billing returns only product1, not product2
                coEvery { billing.awaitGetProducts(any()) } returns setOf(product1)

                When("both products are requested") {
                    val result = storeManager.getProductsWithoutPaywall(listOf("product1", "product2"))

                    Then("the found product is returned") {
                        assertEquals(product1, result["product1"])
                    }

                    And("the missing product is not in the result") {
                        assertNull(result["product2"])
                    }

                    And("the found product is cached as Loaded") {
                        junitAssertTrue(storeManager.hasCached("product1"))
                    }

                    And("the missing product is in Error state but not cached as Loaded") {
                        junitAssertTrue(!storeManager.hasCached("product2"))
                        assertNull(storeManager.getProductFromCache("product2"))
                    }
                }
            }
        }

    @Test
    fun `test concurrent waiters receive error when in-flight fetch fails`() =
        runTest {
            Given("a product whose fetch will fail") {
                val billingDeferred = CompletableDeferred<Set<StoreProduct>>()

                coEvery { billing.awaitGetProducts(any()) } coAnswers { billingDeferred.await() }

                When("two callers request the same product and the fetch fails") {
                    val first = async { runCatching { storeManager.getProductsWithoutPaywall(listOf("product1")) } }
                    val second = async { runCatching { storeManager.getProductsWithoutPaywall(listOf("product1")) } }

                    // Fail the billing call
                    billingDeferred.completeExceptionally(RuntimeException("billing error"))

                    val result1 = first.await()
                    val result2 = second.await()

                    Then("both callers should receive the error") {
                        junitAssertTrue(result1.isFailure)
                        junitAssertTrue(result2.isFailure)
                        assertEquals("billing error", result1.exceptionOrNull()?.message)
                        assertEquals("billing error", result2.exceptionOrNull()?.message)
                    }

                    And("the product is retryable on the next call") {
                        val product =
                            mockk<StoreProduct> {
                                every { fullIdentifier } returns "product1"
                            }
                        coEvery { billing.awaitGetProducts(any()) } returns setOf(product)

                        val result3 = storeManager.getProductsWithoutPaywall(listOf("product1"))
                        assertEquals(product, result3["product1"])
                    }
                }
            }
        }

    @Test
    fun `test getProducts sets failAt on failure and clears on success`() =
        runTest {
            Given("a paywall whose product fetch fails then succeeds") {
                val paywall =
                    Paywall.stub().copy(
                        productIds = listOf("product1:basePlan1:sw-auto"),
                        _productItemsV3 =
                            listOf(
                                CrossplatformProduct(
                                    compositeId = "product1:basePlan1:sw-auto",
                                    storeProduct =
                                        CrossplatformProduct.StoreProduct.PlayStore(
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    entitlements = entitlementsBasic.toList(),
                                    name = "Item1",
                                ),
                            ),
                    )
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1:basePlan1:sw-auto"
                        every { attributes } returns mapOf("attr1" to "value1")
                    }

                coEvery { billing.awaitGetProducts(any()) } throws
                    RuntimeException("Service unavailable") andThen setOf(product)

                When("the first fetch fails") {
                    storeManager.getProducts(paywall = paywall)

                    Then("failAt should be set") {
                        junitAssertTrue(paywall.productsLoadingInfo.failAt != null)
                    }

                    And("a retry succeeds and the result contains the product") {
                        val result = storeManager.getProducts(paywall = paywall)
                        assertEquals(1, result.productsByFullId.size)
                        assertEquals(product, result.productsByFullId["product1:basePlan1:sw-auto"])
                    }
                }
            }
        }

    @Test
    fun `test cacheProduct completes pending loading deferred`() =
        runTest {
            Given("a product that is currently loading") {
                val billingDeferred = CompletableDeferred<Set<StoreProduct>>()
                val product =
                    mockk<StoreProduct> {
                        every { fullIdentifier } returns "product1"
                    }

                coEvery { billing.awaitGetProducts(any()) } coAnswers { billingDeferred.await() }

                When("a caller starts loading and another caches the product externally") {
                    val loader = async { storeManager.getProductsWithoutPaywall(listOf("product1")) }

                    // Simulate an external source caching the product (e.g. from a purchase)
                    storeManager.cacheProduct("product1", product)

                    val result = loader.await()

                    Then("the loader receives the externally cached product") {
                        assertEquals(product, result["product1"])
                    }

                    And("billing call completes without error when we finish it") {
                        billingDeferred.complete(emptySet())
                    }
                }
            }
        }

    @Test
    fun `test products method`() =
        runTest {
            Given("a set of product identifiers") {
                val identifiers = setOf("product1", "product2")
                val expectedProducts =
                    setOf(
                        mockk<StoreProduct> { every { fullIdentifier } returns "product1" },
                        mockk<StoreProduct> { every { fullIdentifier } returns "product2" },
                    )

                coEvery { billing.awaitGetProducts(identifiers) } returns expectedProducts

                When("products method is called") {
                    val result = storeManager.products(identifiers)

                    Then("it should return the correct set of StoreProducts") {
                        assertEquals(expectedProducts, result)
                    }
                }
            }
        }
}
