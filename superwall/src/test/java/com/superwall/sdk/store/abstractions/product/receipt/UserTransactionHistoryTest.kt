package com.superwall.sdk.store.abstractions.product.receipt

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Tests for UserTransactionHistory - verifying merge logic and persistence behavior.
 */
class UserTransactionHistoryTest {
    // MARK: - Mock Transaction

    data class MockTransaction(
        override val productId: String,
        override val transactionId: String,
        override val purchaseDate: Date,
        override val originalPurchaseDate: Date,
        override val expirationDate: Date?,
        override val isRevoked: Boolean,
        override val productType: EntitlementTransactionType,
        override val willRenew: Boolean,
        override val renewedAt: Date?,
        override val isInGracePeriod: Boolean,
        override val isInBillingRetryPeriod: Boolean,
        override val isActive: Boolean,
    ) : EntitlementTransaction

    // MARK: - Helper Methods

    private fun createMockTransaction(
        productId: String = "test_product",
        transactionId: String = "txn_123",
        purchaseDate: Date = Date(),
        originalPurchaseDate: Date? = null,
        expirationDate: Date? = null,
        isRevoked: Boolean = false,
        productType: EntitlementTransactionType = EntitlementTransactionType.AUTO_RENEWABLE,
        willRenew: Boolean = true,
        renewedAt: Date? = null,
        isInGracePeriod: Boolean = false,
        isInBillingRetryPeriod: Boolean = false,
        isActive: Boolean = true,
    ): MockTransaction =
        MockTransaction(
            productId = productId,
            transactionId = transactionId,
            purchaseDate = purchaseDate,
            originalPurchaseDate = originalPurchaseDate ?: purchaseDate,
            expirationDate = expirationDate,
            isRevoked = isRevoked,
            productType = productType,
            willRenew = willRenew,
            renewedAt = renewedAt,
            isInGracePeriod = isInGracePeriod,
            isInBillingRetryPeriod = isInBillingRetryPeriod,
            isActive = isActive,
        )

    // MARK: - StoredEntitlementTransaction Tests

    @Test
    fun `StoredEntitlementTransaction from creates correct copy`() {
        Given("an EntitlementTransaction") {
            val purchaseDate = Date()
            val expirationDate = Date(purchaseDate.time + 30 * 24 * 60 * 60 * 1000L)
            val transaction =
                createMockTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_abc123",
                    purchaseDate = purchaseDate,
                    expirationDate = expirationDate,
                    isActive = true,
                    willRenew = true,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                )

            When("converting to StoredEntitlementTransaction") {
                val stored = StoredEntitlementTransaction.from(transaction)

                Then("all fields should match") {
                    assertEquals("com.test.monthly", stored.productId)
                    assertEquals("txn_abc123", stored.transactionId)
                    assertEquals(purchaseDate, stored.purchaseDate)
                    assertEquals(expirationDate, stored.expirationDate)
                    assertTrue(stored.isActive)
                    assertTrue(stored.willRenew)
                    assertEquals(EntitlementTransactionType.AUTO_RENEWABLE, stored.productType)
                }
            }
        }
    }

    @Test
    fun `StoredEntitlementTransaction withUpdatedActiveStatus marks inactive`() {
        Given("an active stored transaction") {
            val stored =
                StoredEntitlementTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_123",
                    purchaseDate = Date(),
                    originalPurchaseDate = Date(),
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )

            When("updating active status to false") {
                val updated =
                    StoredEntitlementTransaction.withUpdatedActiveStatus(
                        stored = stored,
                        isCurrentlyActive = false,
                    )

                Then("isActive should be false") {
                    assertFalse(updated.isActive)
                }

                And("willRenew should be false since it's inactive") {
                    assertFalse(updated.willRenew)
                }
            }
        }
    }

    @Test
    fun `StoredEntitlementTransaction withUpdatedActiveStatus preserves willRenew when active`() {
        Given("an active stored transaction with willRenew true") {
            val stored =
                StoredEntitlementTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_123",
                    purchaseDate = Date(),
                    originalPurchaseDate = Date(),
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )

            When("updating active status to true") {
                val updated =
                    StoredEntitlementTransaction.withUpdatedActiveStatus(
                        stored = stored,
                        isCurrentlyActive = true,
                    )

                Then("isActive should be true") {
                    assertTrue(updated.isActive)
                }

                And("willRenew should remain true") {
                    assertTrue(updated.willRenew)
                }
            }
        }
    }

    // MARK: - UserTransactionHistory Merge Tests

    @Test
    fun `mergeWith adds new transactions`() {
        Given("an empty history") {
            val history = UserTransactionHistory()

            When("merging with new transactions") {
                val transaction1 =
                    createMockTransaction(
                        productId = "com.test.monthly",
                        transactionId = "txn_001",
                    )
                val transaction2 =
                    createMockTransaction(
                        productId = "com.test.yearly",
                        transactionId = "txn_002",
                    )

                val updated = history.mergeWith(listOf(transaction1, transaction2))

                Then("history should contain both transactions") {
                    assertEquals(2, updated.transactions.size)
                    assertTrue(updated.transactions.containsKey("txn_001"))
                    assertTrue(updated.transactions.containsKey("txn_002"))
                }
            }
        }
    }

    @Test
    fun `mergeWith updates existing transactions`() {
        Given("a history with an existing transaction") {
            val oldDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
            val existingStored =
                StoredEntitlementTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_001",
                    purchaseDate = oldDate,
                    originalPurchaseDate = oldDate,
                    expirationDate = Date(oldDate.time + 30 * 24 * 60 * 60 * 1000L),
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )
            val history = UserTransactionHistory(transactions = mapOf("txn_001" to existingStored))

            When("merging with updated transaction data") {
                val newPurchaseDate = Date()
                val updatedTransaction =
                    createMockTransaction(
                        productId = "com.test.monthly",
                        transactionId = "txn_001",
                        purchaseDate = newPurchaseDate,
                        willRenew = false, // Changed
                        isInGracePeriod = true, // Changed
                    )

                val updated = history.mergeWith(listOf(updatedTransaction))

                Then("transaction should be updated with fresh data") {
                    assertEquals(1, updated.transactions.size)
                    val storedTxn = updated.transactions["txn_001"]!!
                    assertEquals(newPurchaseDate, storedTxn.purchaseDate)
                    assertFalse(storedTxn.willRenew)
                    assertTrue(storedTxn.isInGracePeriod)
                }
            }
        }
    }

    @Test
    fun `mergeWith marks missing transactions as inactive`() {
        Given("a history with transactions that are no longer returned by Google Play") {
            val txn1 =
                StoredEntitlementTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_001",
                    purchaseDate = Date(),
                    originalPurchaseDate = Date(),
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )
            val txn2 =
                StoredEntitlementTransaction(
                    productId = "com.test.yearly",
                    transactionId = "txn_002",
                    purchaseDate = Date(),
                    originalPurchaseDate = Date(),
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )
            val history =
                UserTransactionHistory(
                    transactions =
                        mapOf(
                            "txn_001" to txn1,
                            "txn_002" to txn2,
                        ),
                )

            When("merging with only one of the transactions (simulating expired purchase)") {
                val currentTransaction =
                    createMockTransaction(
                        productId = "com.test.monthly",
                        transactionId = "txn_001",
                        isActive = true,
                    )

                val updated = history.mergeWith(listOf(currentTransaction))

                Then("existing transaction should be updated") {
                    val txn001 = updated.transactions["txn_001"]!!
                    assertTrue(txn001.isActive)
                }

                And("missing transaction should be marked inactive") {
                    val txn002 = updated.transactions["txn_002"]!!
                    assertFalse(txn002.isActive)
                    assertFalse(txn002.willRenew)
                }

                And("both transactions should still exist in history") {
                    assertEquals(2, updated.transactions.size)
                }
            }
        }
    }

    @Test
    fun `mergeWith preserves expired transactions across multiple merges`() {
        Given("a history with an expired transaction") {
            val expiredTxn =
                StoredEntitlementTransaction(
                    productId = "com.test.monthly",
                    transactionId = "txn_expired",
                    purchaseDate = Date(System.currentTimeMillis() - 60 * 24 * 60 * 60 * 1000L),
                    originalPurchaseDate = Date(System.currentTimeMillis() - 60 * 24 * 60 * 60 * 1000L),
                    expirationDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L),
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = false,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = false,
                )
            val history = UserTransactionHistory(transactions = mapOf("txn_expired" to expiredTxn))

            When("merging with a new purchase (empty current purchases from Play)") {
                val newTransaction =
                    createMockTransaction(
                        productId = "com.test.yearly",
                        transactionId = "txn_new",
                        isActive = true,
                    )

                val updated = history.mergeWith(listOf(newTransaction))

                Then("expired transaction should still exist") {
                    assertTrue(updated.transactions.containsKey("txn_expired"))
                    assertFalse(updated.transactions["txn_expired"]!!.isActive)
                }

                And("new transaction should be added") {
                    assertTrue(updated.transactions.containsKey("txn_new"))
                    assertTrue(updated.transactions["txn_new"]!!.isActive)
                }

                And("total transactions should be 2") {
                    assertEquals(2, updated.transactions.size)
                }
            }
        }
    }

    // MARK: - Query Methods Tests

    @Test
    fun `allTransactions returns sorted by purchase date descending`() {
        Given("a history with transactions at different dates") {
            val oldDate = Date(System.currentTimeMillis() - 60 * 24 * 60 * 60 * 1000L)
            val midDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
            val newDate = Date()

            val txnOld =
                StoredEntitlementTransaction(
                    productId = "old",
                    transactionId = "txn_old",
                    purchaseDate = oldDate,
                    originalPurchaseDate = oldDate,
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = false,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = false,
                )
            val txnMid =
                StoredEntitlementTransaction(
                    productId = "mid",
                    transactionId = "txn_mid",
                    purchaseDate = midDate,
                    originalPurchaseDate = midDate,
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )
            val txnNew =
                StoredEntitlementTransaction(
                    productId = "new",
                    transactionId = "txn_new",
                    purchaseDate = newDate,
                    originalPurchaseDate = newDate,
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )

            val history =
                UserTransactionHistory(
                    transactions =
                        mapOf(
                            "txn_old" to txnOld,
                            "txn_mid" to txnMid,
                            "txn_new" to txnNew,
                        ),
                )

            When("getting all transactions") {
                val allTxns = history.allTransactions()

                Then("transactions should be sorted by purchase date descending") {
                    assertEquals(3, allTxns.size)
                    assertEquals("txn_new", allTxns[0].transactionId)
                    assertEquals("txn_mid", allTxns[1].transactionId)
                    assertEquals("txn_old", allTxns[2].transactionId)
                }
            }
        }
    }

    @Test
    fun `activeTransactions returns only active transactions sorted`() {
        Given("a history with mixed active and inactive transactions") {
            val date1 = Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L)
            val date2 = Date()

            val txnInactive =
                StoredEntitlementTransaction(
                    productId = "inactive",
                    transactionId = "txn_inactive",
                    purchaseDate = date1,
                    originalPurchaseDate = date1,
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = false,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = false,
                )
            val txnActive =
                StoredEntitlementTransaction(
                    productId = "active",
                    transactionId = "txn_active",
                    purchaseDate = date2,
                    originalPurchaseDate = date2,
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    renewedAt = null,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                )

            val history =
                UserTransactionHistory(
                    transactions =
                        mapOf(
                            "txn_inactive" to txnInactive,
                            "txn_active" to txnActive,
                        ),
                )

            When("getting active transactions") {
                val activeTxns = history.activeTransactions()

                Then("only active transactions should be returned") {
                    assertEquals(1, activeTxns.size)
                    assertEquals("txn_active", activeTxns[0].transactionId)
                }
            }
        }
    }

    @Test
    fun `empty history returns empty lists`() {
        Given("an empty history") {
            val history = UserTransactionHistory()

            When("querying transactions") {
                val all = history.allTransactions()
                val active = history.activeTransactions()

                Then("both should be empty") {
                    assertTrue(all.isEmpty())
                    assertTrue(active.isEmpty())
                }
            }
        }
    }
}
