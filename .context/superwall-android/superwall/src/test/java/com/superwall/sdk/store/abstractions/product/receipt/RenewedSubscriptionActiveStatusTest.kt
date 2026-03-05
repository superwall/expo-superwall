package com.superwall.sdk.store.abstractions.product.receipt

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for renewed subscription active status.
 *
 * PlayStorePurchaseAdapter.calculateExpirationDate() computes purchaseTime + 1 period,
 * which doesn't account for renewals. Google Play's purchaseTime is the original purchase
 * time, not the latest renewal. queryPurchasesAsync only returns active purchases, so
 * purchaseState=PURCHASED means the subscription IS active regardless of the naive
 * expiration calculation.
 *
 * From user logs:
 *   Transaction history: 1 current, 1 total stored, 0 active
 *   Found purchases: [<0. Products: settlemate_subscription ... state: 1 >]
 */
class RenewedSubscriptionActiveStatusTest {
    // region Helpers

    private fun createMockPurchase(
        products: List<String> = listOf("settlemate_subscription"),
        orderId: String = "GPA.3338-2692-7817-62930",
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
        fullIdentifier: String = "settlemate_subscription:initial-monthly",
        productIdentifier: String = "settlemate_subscription",
        productType: String = ProductType.SUBS,
        subscriptionPeriod: SubscriptionPeriod? = SubscriptionPeriod(1, SubscriptionPeriod.Unit.month),
    ): StoreProduct {
        val productDetails = mockk<ProductDetails>()
        every { productDetails.productType } returns productType

        val rawStoreProduct = mockk<RawStoreProduct>()
        every { rawStoreProduct.fullIdentifier } returns fullIdentifier
        every { rawStoreProduct.productIdentifier } returns productIdentifier
        every { rawStoreProduct.underlyingProductDetails } returns productDetails
        every { rawStoreProduct.subscriptionPeriod } returns subscriptionPeriod

        val storeProduct = mockk<StoreProduct>()
        every { storeProduct.fullIdentifier } returns fullIdentifier
        every { storeProduct.productIdentifier } returns productIdentifier
        every { storeProduct.rawStoreProduct } returns rawStoreProduct

        return storeProduct
    }

    // endregion

    // region Adapter isActive for renewed subscriptions

    /**
     * A monthly subscription purchased 3 months ago that Google Play still returns
     * as PURCHASED (meaning it renewed) must be reported as active.
     */
    @Test
    fun `renewed monthly subscription is active when returned by Google Play as PURCHASED`() {
        Given("a monthly subscription purchased 3 months ago, still returned by Google Play as PURCHASED") {
            val threeMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            val purchase =
                createMockPurchase(
                    purchaseTime = threeMonthsAgo,
                    purchaseState = PurchaseState.PURCHASED,
                    isAutoRenewing = true,
                )
            val storeProduct =
                createMockStoreProduct(
                    subscriptionPeriod = SubscriptionPeriod(1, SubscriptionPeriod.Unit.month),
                )
            val productsById = mapOf("settlemate_subscription" to storeProduct)

            When("creating the adapter (ReceiptManager path)") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("adapter reports isActive=true since Google Play returned this purchase as PURCHASED") {
                    assertTrue(
                        "A PURCHASED subscription returned by queryPurchasesAsync must be active",
                        adapter.isActive,
                    )
                }

                And("both ReceiptManager and AutomaticPurchaseController agree on active status") {
                    // AutomaticPurchaseController path:
                    val apcActive = listOf(purchase).any { it.purchaseState == PurchaseState.PURCHASED }
                    assertTrue("APC sees active", apcActive)
                    // ReceiptManager path (adapter) should agree:
                    val adapter2 = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()
                    assertEquals(
                        "Both paths must agree on active status",
                        apcActive,
                        adapter2.isActive,
                    )
                }
            }
        }
    }

    /**
     * A yearly subscription purchased 13 months ago that Google Play still returns
     * as PURCHASED must be reported as active.
     */
    @Test
    fun `renewed yearly subscription is active when returned by Google Play as PURCHASED`() {
        Given("a yearly subscription purchased 13 months ago, still returned by Google Play") {
            val thirteenMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(395)
            val purchase =
                createMockPurchase(
                    purchaseTime = thirteenMonthsAgo,
                    purchaseState = PurchaseState.PURCHASED,
                    isAutoRenewing = true,
                )
            val storeProduct =
                createMockStoreProduct(
                    subscriptionPeriod = SubscriptionPeriod(1, SubscriptionPeriod.Unit.year),
                )
            val productsById = mapOf("settlemate_subscription" to storeProduct)

            When("creating the adapter") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("adapter reports isActive=true") {
                    assertTrue(
                        "A PURCHASED yearly subscription returned by Google Play must be active",
                        adapter.isActive,
                    )
                }
            }
        }
    }

    /**
     * A subscription within its first period is active (this already works).
     */
    @Test
    fun `subscription within first period is correctly active`() {
        Given("a monthly subscription purchased 15 days ago") {
            val fifteenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
            val purchase =
                createMockPurchase(
                    purchaseTime = fifteenDaysAgo,
                    purchaseState = PurchaseState.PURCHASED,
                    isAutoRenewing = true,
                )
            val storeProduct =
                createMockStoreProduct(
                    subscriptionPeriod = SubscriptionPeriod(1, SubscriptionPeriod.Unit.month),
                )
            val productsById = mapOf("settlemate_subscription" to storeProduct)

            When("creating the adapter") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("adapter correctly reports active") {
                    assertTrue("Should be active within first period", adapter.isActive)
                }
            }
        }
    }

    // endregion

    // region Transaction history

    /**
     * Transaction history must show 1 active for a renewed subscription that
     * Google Play returns as PURCHASED.
     */
    @Test
    fun `transaction history shows 1 active for renewed subscription`() {
        Given("a renewed monthly subscription (purchased 3 months ago, PURCHASED)") {
            val threeMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            val purchase =
                createMockPurchase(
                    purchaseTime = threeMonthsAgo,
                    purchaseState = PurchaseState.PURCHASED,
                    isAutoRenewing = true,
                )
            val storeProduct = createMockStoreProduct()
            val productsById = mapOf("settlemate_subscription" to storeProduct)

            val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()
            val currentTransactions: List<EntitlementTransaction> = listOf(adapter)

            When("merging into transaction history") {
                val history = UserTransactionHistory()
                val updatedHistory = history.mergeWith(currentTransactions)

                Then("1 current, 1 stored, 1 active") {
                    assertEquals(1, currentTransactions.size)
                    assertEquals(1, updatedHistory.transactions.size)
                    assertEquals(
                        "Active count must be 1 for a PURCHASED subscription",
                        1,
                        updatedHistory.activeTransactions().size,
                    )
                }
            }
        }
    }

    // endregion

    // region Entitlement enrichment

    /**
     * EntitlementProcessor must build an active entitlement from a renewed
     * subscription that Google Play returns as PURCHASED.
     */
    @Test
    fun `entitlement processor builds active entitlement from renewed subscription`() {
        Given("a renewed subscription returned by Google Play as PURCHASED") {
            val threeMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            val purchase =
                createMockPurchase(
                    purchaseTime = threeMonthsAgo,
                    purchaseState = PurchaseState.PURCHASED,
                    isAutoRenewing = true,
                )
            val storeProduct = createMockStoreProduct()
            val productsById = mapOf("settlemate_subscription" to storeProduct)
            val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

            val processor = EntitlementProcessor()
            val rawEntitlement = Entitlement(id = "pro", type = Entitlement.Type.SERVICE_LEVEL)
            val serverEntitlementsByProductId =
                mapOf(
                    "settlemate_subscription:initial-monthly" to setOf(rawEntitlement),
                )
            val productIdsByEntitlementId =
                mapOf(
                    "pro" to setOf("settlemate_subscription:initial-monthly"),
                )

            val stored = StoredEntitlementTransaction.from(adapter)
            val transactionsByEntitlement =
                mapOf(
                    "pro" to listOf<EntitlementTransaction>(stored),
                )

            When("EntitlementProcessor builds entitlements") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = productIdsByEntitlementId,
                    )

                Then("entitlement is active") {
                    val entitlements = result.values.flatten()
                    assertEquals(1, entitlements.size)

                    val entitlement = entitlements.first()
                    assertEquals("pro", entitlement.id)
                    assertTrue(
                        "Entitlement must be active for a PURCHASED subscription",
                        entitlement.isActive,
                    )
                }
            }
        }
    }

    // endregion

    // region Edge case: product not found

    /**
     * When the product is not found in productsById, expirationDate is null,
     * so isActive defaults to true for PURCHASED state. However, productType
     * falls back to CONSUMABLE which is incorrect for a subscription.
     */
    @Test
    fun `when product not found - isActive is true but productType is wrong`() {
        Given("a renewed subscription where product lookup fails") {
            val threeMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            val purchase =
                createMockPurchase(
                    purchaseTime = threeMonthsAgo,
                    purchaseState = PurchaseState.PURCHASED,
                )
            val productsById = emptyMap<String, StoreProduct>()

            When("creating the adapter without product") {
                val adapter = PlayStorePurchaseAdapter.fromPurchase(purchase, productsById).first()

                Then("isActive is true due to null expiration") {
                    assertNull(adapter.expirationDate)
                    assertTrue(
                        "Without product info, null expiration means isActive=true",
                        adapter.isActive,
                    )
                }

                And("productType falls back to CONSUMABLE") {
                    assertEquals(
                        EntitlementTransactionType.CONSUMABLE,
                        adapter.productType,
                    )
                }
            }
        }
    }

    // endregion
}
