package com.superwall.sdk.analytics.internal.trackable

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.products.mockPricingPhase
import com.superwall.sdk.products.mockProductDetails
import com.superwall.sdk.products.mockSubscriptionOfferDetails
import com.superwall.sdk.store.abstractions.product.BasePlanType
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProductType
import com.superwall.sdk.store.abstractions.transactions.GoogleBillingPurchaseTransaction
import com.superwall.sdk.store.abstractions.transactions.StorePayment
import com.superwall.sdk.store.abstractions.transactions.StoreTransactionState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Date

class TransactionCompleteStateTest {
    @Test
    fun `Complete enriches transaction with expiration date for monthly subscription`() {
        Given("a monthly subscription product and a Google Billing transaction") {
            val product = createSubscriptionProduct(basePlanId = "monthly", billingPeriod = "P1M")
            val purchaseDate = Date(1_700_000_000_000L)
            val transaction = createTransaction(transactionDate = purchaseDate)

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("expiration date is one month after purchase") {
                    val expected =
                        Calendar
                            .getInstance()
                            .apply {
                                time = purchaseDate
                                add(Calendar.MONTH, 1)
                            }.time
                    assertNotNull(state.transaction)
                    assertEquals(expected, (state.transaction as GoogleBillingPurchaseTransaction).expirationDate)
                }
            }
        }
    }

    @Test
    fun `Complete enriches transaction with expiration date for yearly subscription`() {
        Given("a yearly subscription product and a Google Billing transaction") {
            val product = createSubscriptionProduct(basePlanId = "yearly", billingPeriod = "P1Y")
            val purchaseDate = Date(1_700_000_000_000L)
            val transaction = createTransaction(transactionDate = purchaseDate)

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("expiration date is one year after purchase") {
                    val expected =
                        Calendar
                            .getInstance()
                            .apply {
                                time = purchaseDate
                                add(Calendar.YEAR, 1)
                            }.time
                    assertEquals(expected, (state.transaction as GoogleBillingPurchaseTransaction).expirationDate)
                }
            }
        }
    }

    @Test
    fun `Complete enriches transaction with expiration date for weekly subscription`() {
        Given("a weekly subscription product and a Google Billing transaction") {
            val product = createSubscriptionProduct(basePlanId = "weekly", billingPeriod = "P1W")
            val purchaseDate = Date(1_700_000_000_000L)
            val transaction = createTransaction(transactionDate = purchaseDate)

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("expiration date is one week after purchase") {
                    val expected =
                        Calendar
                            .getInstance()
                            .apply {
                                time = purchaseDate
                                add(Calendar.WEEK_OF_YEAR, 1)
                            }.time
                    assertEquals(expected, (state.transaction as GoogleBillingPurchaseTransaction).expirationDate)
                }
            }
        }
    }

    @Test
    fun `Complete sets subscriptionGroupId to basePlanId`() {
        Given("a subscription product with a specific base plan") {
            val product = createSubscriptionProduct(basePlanId = "premium-monthly")
            val transaction = createTransaction()

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("subscriptionGroupId equals the basePlanId") {
                    assertEquals(
                        "premium-monthly",
                        (state.transaction as GoogleBillingPurchaseTransaction).subscriptionGroupId,
                    )
                }
            }
        }
    }

    @Test
    fun `Complete sets offerId from product`() {
        Given("a subscription product with a specific offer") {
            val product =
                createSubscriptionProduct(
                    basePlanId = "monthly",
                    offerId = "intro-offer",
                    offerType = OfferType.Specific("intro-offer"),
                )
            val transaction = createTransaction()

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("offerId is set from the product") {
                    assertEquals(
                        "intro-offer",
                        (state.transaction as GoogleBillingPurchaseTransaction).offerId,
                    )
                }
            }
        }
    }

    @Test
    fun `Complete leaves expiration date null when product has no subscription period`() {
        Given("a product with no subscription period") {
            val product =
                mockk<StoreProductType>(relaxed = true) {
                    every { subscriptionPeriod } returns null
                }
            val transaction = createTransaction()

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("expiration date remains null") {
                    assertNull((state.transaction as GoogleBillingPurchaseTransaction).expirationDate)
                }
            }
        }
    }

    @Test
    fun `Complete leaves expiration date null when transaction date is null`() {
        Given("a subscription product but a transaction with no date") {
            val product = createSubscriptionProduct(basePlanId = "monthly", billingPeriod = "P1M")
            val transaction = createTransaction(transactionDate = null)

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("expiration date remains null") {
                    assertNull((state.transaction as GoogleBillingPurchaseTransaction).expirationDate)
                }
            }
        }
    }

    @Test
    fun `Complete passes through non-Google transactions unchanged`() {
        Given("a non-Google transaction") {
            val product = mockk<StoreProductType>(relaxed = true)
            val transaction = mockk<com.superwall.sdk.store.abstractions.transactions.StoreTransactionType>(relaxed = true)

            When("Complete state is created") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = transaction,
                    )

                Then("the transaction is returned as-is") {
                    assertEquals(transaction, state.transaction)
                }
            }
        }
    }

    @Test
    fun `Complete handles null transaction`() {
        Given("a null transaction") {
            val product = mockk<StoreProductType>(relaxed = true)

            When("Complete state is created with null") {
                val state =
                    InternalSuperwallEvent.Transaction.State.Complete(
                        product = product,
                        transaction = null,
                    )

                Then("the transaction is null") {
                    assertNull(state.transaction)
                }
            }
        }
    }

    // -- Helpers --

    private fun createSubscriptionProduct(
        basePlanId: String = "mock-base-plan-id",
        billingPeriod: String = "P1M",
        offerId: String = "mock-offer-id",
        offerType: OfferType = OfferType.Auto,
    ): RawStoreProduct {
        val pricingPhase = mockPricingPhase(billingPeriod = billingPeriod)
        val offerDetails =
            mockSubscriptionOfferDetails(
                basePlanId = basePlanId,
                offerId = offerId,
                pricingPhases = listOf(pricingPhase),
            )
        val productDetails =
            mockProductDetails(
                productId = "com.test.product:$basePlanId",
                subscriptionOfferDetails = listOf(offerDetails),
            )
        return RawStoreProduct(
            underlyingProductDetails = productDetails,
            fullIdentifier = "com.test.product:$basePlanId",
            basePlanType = BasePlanType.Specific(basePlanId),
            offerType = offerType,
        )
    }

    private fun createTransaction(transactionDate: Date? = Date(1_700_000_000_000L)): GoogleBillingPurchaseTransaction {
        val payment =
            StorePayment(
                productIdentifier = "com.test.product",
                quantity = 1,
                discountIdentifier = null,
            )
        return GoogleBillingPurchaseTransaction(
            transactionDate = transactionDate,
            originalTransactionIdentifier = "order-123",
            state = StoreTransactionState.Purchased,
            storeTransactionId = "order-123",
            originalTransactionDate = transactionDate,
            webOrderLineItemID = null,
            appBundleId = null,
            subscriptionGroupId = null,
            isUpgraded = null,
            expirationDate = null,
            offerId = null,
            revocationDate = null,
            appAccountToken = null,
            purchaseToken = "token-123",
            payment = payment,
            signature = null,
        )
    }
}
