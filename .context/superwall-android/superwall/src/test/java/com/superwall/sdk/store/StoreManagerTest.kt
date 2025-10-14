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
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.models.product.PlayStoreProduct
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class StoreManagerTest {
    private lateinit var purchaseController: InternalPurchaseController
    private lateinit var billing: Billing
    private lateinit var storeManager: StoreManager
    private val entitlementsBasic = setOf(Entitlement("entitlement1"))

    @Before
    fun setup() {
        purchaseController = mockk()
        billing = mockk()
        storeManager =
            StoreManager(
                purchaseController,
                billing,
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
                        _productItems =
                            listOf(
                                ProductItem(
                                    "Item1",
                                    ProductItem.StoreProductType.PlayStore(
                                        PlayStoreProduct(
                                            store = Store.PLAY_STORE,
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    ),
                                    entitlements = entitlementsBasic.toSet(),
                                ),
                                ProductItem(
                                    "Item2",
                                    type =
                                        ProductItem.StoreProductType.PlayStore(
                                            PlayStoreProduct(
                                                store = Store.PLAY_STORE,
                                                productIdentifier = "product2",
                                                basePlanIdentifier = "basePlan1",
                                                offer = Offer.Automatic(),
                                            ),
                                        ),
                                    entitlements = entitlementsBasic.toSet(),
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
                        _productItems =
                            listOf(
                                ProductItem(
                                    "Item1",
                                    ProductItem.StoreProductType.PlayStore(
                                        PlayStoreProduct(
                                            store = Store.PLAY_STORE,
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    ),
                                    entitlements = entitlementsBasic.toSet(),
                                ),
                                ProductItem(
                                    "Item2",
                                    type =
                                        ProductItem.StoreProductType.PlayStore(
                                            PlayStoreProduct(
                                                store = Store.PLAY_STORE,
                                                productIdentifier = "product2",
                                                basePlanIdentifier = "basePlan1",
                                                offer = Offer.Automatic(),
                                            ),
                                        ),
                                    entitlements = entitlementsBasic.toSet(),
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
                        _productItems =
                            listOf(
                                ProductItem(
                                    "Item1",
                                    ProductItem.StoreProductType.PlayStore(
                                        PlayStoreProduct(
                                            store = Store.PLAY_STORE,
                                            productIdentifier = "product1",
                                            basePlanIdentifier = "basePlan1",
                                            offer = Offer.Automatic(),
                                        ),
                                    ),
                                    entitlements = entitlementsBasic.toSet(),
                                ),
                                ProductItem(
                                    "Item2",
                                    type =
                                        ProductItem.StoreProductType.PlayStore(
                                            PlayStoreProduct(
                                                store = Store.PLAY_STORE,
                                                productIdentifier = "product2",
                                                basePlanIdentifier = "basePlan1",
                                                offer = Offer.Automatic(),
                                            ),
                                        ),
                                    entitlements = entitlementsBasic.toSet(),
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
