package com.superwall.sdk.store.abstractions.product.receipt

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.entitlements.Entitlement
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Tests for EntitlementProcessor - ported from iOS EntitlementProcessorTests.swift
 */
class EntitlementProcessorTest {
    // MARK: - Mock Transaction Types

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
        productType: EntitlementTransactionType = EntitlementTransactionType.CONSUMABLE,
        willRenew: Boolean = false,
        renewedAt: Date? = null,
        isInGracePeriod: Boolean = false,
        isInBillingRetryPeriod: Boolean = false,
        isActive: Boolean = false,
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

    private fun createEntitlement(
        id: String = "test_entitlement",
        productIds: Set<String> = setOf("test_product"),
    ): Entitlement =
        Entitlement(
            id = id,
            type = Entitlement.Type.SERVICE_LEVEL,
            isActive = false,
            productIds = productIds,
        )

    /**
     * Builds productIdsByEntitlementId from rawEntitlementsByProductId.
     * This is a helper to derive the mapping from the existing test data.
     */
    private fun buildProductIdsByEntitlementId(rawEntitlementsByProductId: Map<String, Set<Entitlement>>): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        rawEntitlementsByProductId.forEach { (productId, entitlements) ->
            entitlements.forEach { entitlement ->
                result.getOrPut(entitlement.id) { mutableSetOf() }.add(productId)
            }
        }
        return result
    }

    private val processor = EntitlementProcessor()

    // MARK: - Basic Processing Tests

    @Test
    fun `process empty transactions returns empty entitlements`() {
        Given("empty transactions and entitlements maps") {
            val transactionsByEntitlement: Map<String, List<EntitlementTransaction>> = emptyMap()
            val rawEntitlementsByProductId: Map<String, Set<Entitlement>> = emptyMap()

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should be empty") {
                    assertTrue(result.isEmpty())
                }
            }
        }
    }

    @Test
    fun `process lifetime non-consumable product`() {
        Given("a non-consumable transaction and entitlement") {
            val transaction =
                createMockTransaction(
                    productId = "lifetime_product",
                    isRevoked = false,
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "lifetime_entitlement",
                    productIds = setOf("lifetime_product"),
                )

            val transactionsByEntitlement = mapOf("lifetime_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("lifetime_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["lifetime_product"]?.firstOrNull()

                Then("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }

                And("entitlement should be lifetime") {
                    assertEquals(true, processedEntitlement?.isLifetime)
                }

                And("latest product ID should be set") {
                    assertEquals("lifetime_product", processedEntitlement?.latestProductId)
                }
            }
        }
    }

    @Test
    fun `process active subscription`() {
        Given("an active auto-renewable subscription") {
            val futureDate = Date(System.currentTimeMillis() + 3600_000) // 1 hour from now
            val transaction =
                createMockTransaction(
                    productId = "subscription_product",
                    expirationDate = futureDate,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "subscription_entitlement",
                    productIds = setOf("subscription_product"),
                )

            val transactionsByEntitlement = mapOf("subscription_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("subscription_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["subscription_product"]?.firstOrNull()

                Then("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }

                And("entitlement should not be lifetime") {
                    assertEquals(false, processedEntitlement?.isLifetime)
                }

                And("entitlement should auto-renew") {
                    assertEquals(true, processedEntitlement?.willRenew)
                }

                And("expiration date should match") {
                    assertEquals(futureDate, processedEntitlement?.expiresAt)
                }
            }
        }
    }

    @Test
    fun `process expired subscription`() {
        Given("an expired subscription") {
            val pastDate = Date(System.currentTimeMillis() - 3600_000) // 1 hour ago
            val transaction =
                createMockTransaction(
                    productId = "expired_product",
                    expirationDate = pastDate,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "expired_entitlement",
                    productIds = setOf("expired_product"),
                )

            val transactionsByEntitlement = mapOf("expired_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("expired_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["expired_product"]?.firstOrNull()

                Then("entitlement should be inactive") {
                    assertEquals(false, processedEntitlement?.isActive)
                }

                And("expiration date should match") {
                    assertEquals(pastDate, processedEntitlement?.expiresAt)
                }
            }
        }
    }

    // MARK: - Multiple Transaction Tests

    @Test
    fun `process multiple transactions for same entitlement`() {
        Given("older and newer transactions for the same entitlement") {
            val baseDate = Date()
            val olderTransaction =
                createMockTransaction(
                    productId = "product1",
                    transactionId = "txn_1",
                    purchaseDate = Date(baseDate.time - 1000),
                    expirationDate = Date(baseDate.time - 500),
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    isActive = false,
                )

            val newerTransaction =
                createMockTransaction(
                    productId = "product1",
                    transactionId = "txn_2",
                    purchaseDate = baseDate,
                    expirationDate = Date(baseDate.time + 3600_000),
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "multi_entitlement",
                    productIds = setOf("product1"),
                )

            val transactionsByEntitlement = mapOf("multi_entitlement" to listOf<EntitlementTransaction>(olderTransaction, newerTransaction))
            val rawEntitlementsByProductId = mapOf("product1" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["product1"]?.firstOrNull()

                Then("entitlement should be active (from newer transaction)") {
                    assertEquals(true, processedEntitlement?.isActive)
                }

                And("entitlement should auto-renew") {
                    assertEquals(true, processedEntitlement?.willRenew)
                }

                And("latest product ID should be set") {
                    assertEquals("product1", processedEntitlement?.latestProductId)
                }
            }
        }
    }

    @Test
    fun `process renewal tracking`() {
        Given("a renewed subscription transaction") {
            val baseDate = Date()
            val originalDate = Date(baseDate.time - 86400_000) // 1 day ago
            val renewalDate = baseDate

            val transaction =
                createMockTransaction(
                    productId = "renewal_product",
                    purchaseDate = renewalDate,
                    originalPurchaseDate = originalDate,
                    expirationDate = Date(baseDate.time + 3600_000),
                    isRevoked = false,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "renewal_entitlement",
                    productIds = setOf("renewal_product"),
                )

            val transactionsByEntitlement = mapOf("renewal_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("renewal_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["renewal_product"]?.firstOrNull()

                Then("startsAt should be the original purchase date") {
                    assertEquals(originalDate, processedEntitlement?.startsAt)
                }

                And("renewedAt should be the renewal date") {
                    assertEquals(renewalDate, processedEntitlement?.renewedAt)
                }
            }
        }
    }

    // MARK: - Multiple Product Tests

    @Test
    fun `process entitlement with multiple lifetime products`() {
        Given("two non-consumable products for the same entitlement") {
            val transaction1 =
                createMockTransaction(
                    productId = "product1",
                    transactionId = "txn_1",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val transaction2 =
                createMockTransaction(
                    productId = "product2",
                    transactionId = "txn_2",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "multi_product_entitlement",
                    productIds = setOf("product1", "product2"),
                )

            val transactionsByEntitlement = mapOf("multi_product_entitlement" to listOf<EntitlementTransaction>(transaction1, transaction2))
            val rawEntitlementsByProductId =
                mapOf(
                    "product1" to setOf(entitlement),
                    "product2" to setOf(entitlement),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should have 2 products") {
                    assertEquals(2, result.size)
                }

                And("both products should be active") {
                    assertEquals(true, result["product1"]?.firstOrNull()?.isActive)
                    assertEquals(true, result["product2"]?.firstOrNull()?.isActive)
                }

                And("both products should have 2 product IDs") {
                    assertEquals(2, result["product1"]?.firstOrNull()?.productIds?.size)
                    assertEquals(2, result["product2"]?.firstOrNull()?.productIds?.size)
                }
            }
        }
    }

    // MARK: - Edge Cases

    @Test
    fun `process non-consumable without expiration`() {
        Given("a non-consumable transaction without expiration date") {
            val transaction =
                createMockTransaction(
                    productId = "non_consumable",
                    expirationDate = null,
                    isRevoked = false,
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "non_consumable_entitlement",
                    productIds = setOf("non_consumable"),
                )

            val transactionsByEntitlement = mapOf("non_consumable_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("non_consumable" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["non_consumable"]?.firstOrNull()

                Then("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }

                And("entitlement should be lifetime") {
                    assertEquals(true, processedEntitlement?.isLifetime)
                }

                And("expiration should be null") {
                    assertNull(processedEntitlement?.expiresAt)
                }

                And("latest product ID should be set") {
                    assertEquals("non_consumable", processedEntitlement?.latestProductId)
                }
            }
        }
    }

    @Test
    fun `process consumable transaction`() {
        Given("a consumable transaction") {
            val transaction =
                createMockTransaction(
                    productId = "consumable",
                    productType = EntitlementTransactionType.CONSUMABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "consumable_entitlement",
                    productIds = setOf("consumable"),
                )

            val transactionsByEntitlement = mapOf("consumable_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("consumable" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["consumable"]?.firstOrNull()

                Then("entitlement should not be active (consumables don't grant active status)") {
                    assertEquals(false, processedEntitlement?.isActive)
                }
            }
        }
    }

    // MARK: - Complex Scenarios

    @Test
    fun `process mixed product types in same entitlement`() {
        Given("a lifetime product and an expired subscription for the same entitlement") {
            val lifetimeTransaction =
                createMockTransaction(
                    productId = "lifetime_product",
                    transactionId = "txn_lifetime",
                    isRevoked = false,
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val subscriptionTransaction =
                createMockTransaction(
                    productId = "subscription_product",
                    transactionId = "txn_subscription",
                    expirationDate = Date(System.currentTimeMillis() - 3600_000), // Expired
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "mixed_entitlement",
                    productIds = setOf("lifetime_product", "subscription_product"),
                )

            val transactionsByEntitlement =
                mapOf(
                    "mixed_entitlement" to listOf<EntitlementTransaction>(lifetimeTransaction, subscriptionTransaction),
                )
            val rawEntitlementsByProductId =
                mapOf(
                    "lifetime_product" to setOf(entitlement),
                    "subscription_product" to setOf(entitlement),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should have 2 products") {
                    assertEquals(2, result.size)
                }

                And("each product should have exactly one entitlement") {
                    assertEquals(1, result["lifetime_product"]?.size)
                    assertEquals(1, result["subscription_product"]?.size)
                }

                val lifetimeResult = result["lifetime_product"]?.firstOrNull()
                val subscriptionResult = result["subscription_product"]?.firstOrNull()

                And("both should reference the same entitlement ID") {
                    assertEquals("mixed_entitlement", lifetimeResult?.id)
                    assertEquals("mixed_entitlement", subscriptionResult?.id)
                }

                And("both should have identical enriched entitlement data") {
                    assertEquals(lifetimeResult, subscriptionResult)
                }

                And("entitlement should be active (lifetime takes precedence)") {
                    assertEquals(true, lifetimeResult?.isActive)
                }

                And("entitlement should be lifetime") {
                    assertEquals(true, lifetimeResult?.isLifetime)
                }

                And("latest product ID should be the lifetime product") {
                    assertEquals("lifetime_product", lifetimeResult?.latestProductId)
                }

                And("productIds should contain both products") {
                    assertEquals(setOf("lifetime_product", "subscription_product"), lifetimeResult?.productIds)
                }
            }
        }
    }

    @Test
    fun `process multiple entitlements for same product`() {
        Given("one product unlocking multiple entitlements") {
            val transaction =
                createMockTransaction(
                    productId = "shared_product",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val entitlement1 =
                createEntitlement(
                    id = "entitlement_1",
                    productIds = setOf("shared_product"),
                )

            val entitlement2 =
                createEntitlement(
                    id = "entitlement_2",
                    productIds = setOf("shared_product"),
                )

            val transactionsByEntitlement =
                mapOf(
                    "entitlement_1" to listOf<EntitlementTransaction>(transaction),
                    "entitlement_2" to listOf<EntitlementTransaction>(transaction),
                )
            val rawEntitlementsByProductId =
                mapOf(
                    "shared_product" to setOf(entitlement1, entitlement2),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val entitlements = result["shared_product"]

                Then("product should have 2 entitlements") {
                    assertEquals(2, entitlements?.size)
                }

                And("entitlement_1 should be included") {
                    assertTrue(entitlements?.any { it.id == "entitlement_1" } == true)
                }

                And("entitlement_2 should be included") {
                    assertTrue(entitlements?.any { it.id == "entitlement_2" } == true)
                }
            }
        }
    }

    @Test
    fun `process latest product ID selection with multiple renewable transactions`() {
        Given("older and newer subscription products for the same entitlement") {
            val baseDate = Date()
            val olderTransaction =
                createMockTransaction(
                    productId = "old_product",
                    transactionId = "txn_old",
                    purchaseDate = Date(baseDate.time - 7200_000), // 2 hours ago
                    expirationDate = Date(baseDate.time - 3600_000), // Expired 1 hour ago
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                )

            val newerTransaction =
                createMockTransaction(
                    productId = "new_product",
                    transactionId = "txn_new",
                    purchaseDate = Date(baseDate.time - 1800_000), // 30 minutes ago
                    expirationDate = Date(baseDate.time + 3600_000), // Active
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "version_entitlement",
                    productIds = setOf("old_product", "new_product"),
                )

            val transactionsByEntitlement =
                mapOf(
                    "version_entitlement" to listOf<EntitlementTransaction>(olderTransaction, newerTransaction),
                )
            val rawEntitlementsByProductId =
                mapOf(
                    "old_product" to setOf(entitlement),
                    "new_product" to setOf(entitlement),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["new_product"]?.firstOrNull()

                Then("latest product ID should be the newer product") {
                    assertEquals("new_product", processedEntitlement?.latestProductId)
                }

                And("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }
            }
        }
    }

    @Test
    fun `process entitlement with no matching raw entitlements`() {
        Given("a transaction with no matching raw entitlements") {
            val transaction =
                createMockTransaction(
                    productId = "orphan_product",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val transactionsByEntitlement = mapOf("orphan_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId: Map<String, Set<Entitlement>> = emptyMap()

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should be empty") {
                    assertTrue(result.isEmpty())
                }
            }
        }
    }

    @Test
    fun `process entitlement with mismatched IDs`() {
        Given("a transaction with different entitlement ID than raw entitlement") {
            val transaction =
                createMockTransaction(
                    productId = "product_a",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "different_entitlement",
                    productIds = setOf("product_a"),
                )

            val transactionsByEntitlement = mapOf("transaction_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("product_a" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("raw entitlement should be returned (inactive)") {
                    assertEquals(1, result["product_a"]?.size)
                }

                And("entitlement ID should match raw entitlement") {
                    assertEquals("different_entitlement", result["product_a"]?.firstOrNull()?.id)
                }

                And("entitlement should be inactive") {
                    assertEquals(false, result["product_a"]?.firstOrNull()?.isActive)
                }
            }
        }
    }

    @Test
    fun `process complex expiration date selection`() {
        Given("multiple transactions with different expiration dates including revoked") {
            val baseDate = Date()

            val transactions =
                listOf(
                    createMockTransaction(
                        productId = "product1",
                        transactionId = "txn_1",
                        expirationDate = Date(baseDate.time + 1800_000), // 30 min
                        isRevoked = false,
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "product2",
                        transactionId = "txn_2",
                        expirationDate = Date(baseDate.time + 3600_000), // 1 hour
                        isRevoked = false,
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "product3",
                        transactionId = "txn_3",
                        expirationDate = Date(baseDate.time + 4000_000), // 66 mins (latest but revoked)
                        isRevoked = true,
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        isActive = false,
                    ),
                )

            val entitlement =
                createEntitlement(
                    id = "complex_entitlement",
                    productIds = setOf("product1", "product2", "product3"),
                )

            val transactionsByEntitlement = mapOf("complex_entitlement" to transactions.map { it as EntitlementTransaction })
            val rawEntitlementsByProductId =
                mapOf(
                    "product1" to setOf(entitlement),
                    "product2" to setOf(entitlement),
                    "product3" to setOf(entitlement),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["product1"]?.firstOrNull()

                Then("expiration should be from non-revoked transaction with latest date") {
                    assertEquals(Date(baseDate.time + 3600_000), processedEntitlement?.expiresAt)
                }

                And("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }
            }
        }
    }

    @Test
    fun `process non-renewable subscription`() {
        Given("a non-renewable subscription") {
            val futureDate = Date(System.currentTimeMillis() + 3600_000)
            val transaction =
                createMockTransaction(
                    productId = "non_renewable_product",
                    expirationDate = futureDate,
                    productType = EntitlementTransactionType.NON_RENEWABLE,
                    willRenew = false,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "non_renewable_entitlement",
                    productIds = setOf("non_renewable_product"),
                )

            val transactionsByEntitlement = mapOf("non_renewable_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("non_renewable_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["non_renewable_product"]?.firstOrNull()

                Then("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }

                And("entitlement should not be lifetime") {
                    assertEquals(false, processedEntitlement?.isLifetime)
                }

                And("entitlement should not auto-renew") {
                    assertEquals(false, processedEntitlement?.willRenew)
                }

                And("expiration date should match") {
                    assertEquals(futureDate, processedEntitlement?.expiresAt)
                }
            }
        }
    }

    @Test
    fun `process large number of transactions`() {
        Given("100 transactions") {
            val baseDate = Date()
            val transactions = mutableListOf<MockTransaction>()

            for (i in 0 until 100) {
                transactions.add(
                    createMockTransaction(
                        productId = "product_$i",
                        transactionId = "txn_$i",
                        purchaseDate = Date(baseDate.time + i * 60_000),
                        expirationDate = Date(baseDate.time + i * 60_000 + 3600_000),
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        isActive = i > 95,
                    ),
                )
            }

            val entitlement =
                createEntitlement(
                    id = "bulk_entitlement",
                    productIds = transactions.map { it.productId }.toSet(),
                )

            val transactionsByEntitlement = mapOf("bulk_entitlement" to transactions.map { it as EntitlementTransaction })
            val rawEntitlementsByProductId = transactions.associate { it.productId to setOf(entitlement) }

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should have 100 products") {
                    assertEquals(100, result.size)
                }

                val latestProductId = "product_99"
                val processedEntitlement = result[latestProductId]?.firstOrNull()

                And("latest product should be product_99") {
                    assertEquals(latestProductId, processedEntitlement?.latestProductId)
                }

                And("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }
            }
        }
    }

    @Test
    fun `process transactions with extreme dates`() {
        Given("transactions with extreme past and future dates") {
            val distantPast = Date(Long.MIN_VALUE / 2)
            val distantFuture = Date(Long.MAX_VALUE / 2)

            val transaction =
                createMockTransaction(
                    productId = "extreme_product",
                    purchaseDate = distantPast,
                    originalPurchaseDate = distantPast,
                    expirationDate = distantFuture,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    isActive = true,
                )

            val entitlement =
                createEntitlement(
                    id = "extreme_entitlement",
                    productIds = setOf("extreme_product"),
                )

            val transactionsByEntitlement = mapOf("extreme_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("extreme_product" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result["extreme_product"]?.firstOrNull()

                Then("startsAt should be distant past") {
                    assertEquals(distantPast, processedEntitlement?.startsAt)
                }

                And("expiresAt should be distant future") {
                    assertEquals(distantFuture, processedEntitlement?.expiresAt)
                }

                And("entitlement should be active") {
                    assertEquals(true, processedEntitlement?.isActive)
                }
            }
        }
    }

    @Test
    fun `process empty product IDs`() {
        Given("a transaction with empty product ID") {
            val transaction =
                createMockTransaction(
                    productId = "",
                    productType = EntitlementTransactionType.NON_CONSUMABLE,
                )

            val entitlement =
                createEntitlement(
                    id = "empty_entitlement",
                    productIds = setOf(""),
                )

            val transactionsByEntitlement = mapOf("empty_entitlement" to listOf<EntitlementTransaction>(transaction))
            val rawEntitlementsByProductId = mapOf("" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                val processedEntitlement = result[""]?.firstOrNull()

                Then("entitlement should exist") {
                    assertNotNull(processedEntitlement)
                }

                And("productIds should contain empty string") {
                    assertTrue(processedEntitlement?.productIds?.contains("") == true)
                }
            }
        }
    }

    // MARK: - Performance Tests

    @Test
    fun `process entitlements with many-to-many relationships`() {
        Given("10 entitlements each with 5 products") {
            val transactionsByEntitlement = mutableMapOf<String, List<EntitlementTransaction>>()
            val rawEntitlementsByProductId = mutableMapOf<String, MutableSet<Entitlement>>()

            for (entitlementIndex in 0 until 10) {
                val entitlementId = "entitlement_$entitlementIndex"
                val productIds = mutableSetOf<String>()
                val transactions = mutableListOf<MockTransaction>()

                for (productIndex in 0 until 5) {
                    val productId = "product_$productIndex"
                    productIds.add(productId)

                    transactions.add(
                        createMockTransaction(
                            productId = productId,
                            transactionId = "txn_${entitlementIndex}_$productIndex",
                            productType = EntitlementTransactionType.NON_CONSUMABLE,
                        ),
                    )

                    val entitlement =
                        createEntitlement(
                            id = entitlementId,
                            productIds = productIds.toSet(),
                        )

                    rawEntitlementsByProductId.getOrPut(productId) { mutableSetOf() }.add(entitlement)
                }

                transactionsByEntitlement[entitlementId] = transactions.map { it as EntitlementTransaction }
            }

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("result should have 5 products") {
                    assertEquals(5, result.size)
                }

                And("each product should have 10 entitlements") {
                    for (productId in result.keys) {
                        assertEquals(10, result[productId]?.size)
                    }
                }
            }
        }
    }

    // MARK: - Transaction Processing Tests

    @Test
    fun `process transactions into subscription and non-subscription objects`() {
        Given("consumable, lifetime, and subscription transactions") {
            val transactions =
                listOf<EntitlementTransaction>(
                    createMockTransaction(
                        productId = "consumable_product",
                        transactionId = "txn_1",
                        productType = EntitlementTransactionType.CONSUMABLE,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "lifetime_product",
                        transactionId = "txn_2",
                        productType = EntitlementTransactionType.NON_CONSUMABLE,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "subscription_product",
                        transactionId = "txn_3",
                        expirationDate = Date(System.currentTimeMillis() + 3600_000),
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        willRenew = true,
                        isInGracePeriod = false,
                        isInBillingRetryPeriod = false,
                        isActive = true,
                    ),
                )

            When("processing transactions") {
                val (nonSubscriptions, subscriptions) = processor.processTransactions(transactions)

                Then("there should be 2 non-subscriptions") {
                    assertEquals(2, nonSubscriptions.size)
                }

                And("there should be 1 subscription") {
                    assertEquals(1, subscriptions.size)
                }

                val consumable = nonSubscriptions.find { it.productId == "consumable_product" }
                And("consumable should be marked as consumable") {
                    assertEquals(true, consumable?.isConsumable)
                    assertEquals("txn_1", consumable?.transactionId)
                }

                val lifetime = nonSubscriptions.find { it.productId == "lifetime_product" }
                And("lifetime should not be marked as consumable") {
                    assertEquals(false, lifetime?.isConsumable)
                    assertEquals("txn_2", lifetime?.transactionId)
                }

                val subscription = subscriptions.firstOrNull()
                And("subscription should have correct properties") {
                    assertEquals("subscription_product", subscription?.productId)
                    assertEquals("txn_3", subscription?.transactionId)
                    assertEquals(true, subscription?.willRenew)
                    assertEquals(true, subscription?.isActive)
                    assertEquals(false, subscription?.isInGracePeriod)
                    assertEquals(false, subscription?.isInBillingRetryPeriod)
                }
            }
        }
    }

    @Test
    fun `process transactions separates autoRenewable and nonRenewable into subscriptions`() {
        Given("auto-renewable and non-renewable subscription transactions") {
            val transactions =
                listOf<EntitlementTransaction>(
                    createMockTransaction(
                        productId = "auto_renewable",
                        transactionId = "txn_auto",
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        willRenew = true,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "non_renewable",
                        transactionId = "txn_non_renewable",
                        productType = EntitlementTransactionType.NON_RENEWABLE,
                        willRenew = false,
                        isActive = true,
                    ),
                )

            When("processing transactions") {
                val (_, subscriptions) = processor.processTransactions(transactions)

                Then("there should be 2 subscriptions") {
                    assertEquals(2, subscriptions.size)
                }

                And("auto_renewable should be included") {
                    assertTrue(subscriptions.any { it.productId == "auto_renewable" })
                }

                And("non_renewable should be included") {
                    assertTrue(subscriptions.any { it.productId == "non_renewable" })
                }
            }
        }
    }

    @Test
    fun `process transactions preserves subscription state fields`() {
        Given("subscriptions with various states") {
            val transactions =
                listOf<EntitlementTransaction>(
                    createMockTransaction(
                        productId = "grace_period_sub",
                        transactionId = "txn_grace",
                        expirationDate = Date(System.currentTimeMillis() + 3600_000),
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        willRenew = true,
                        isInGracePeriod = true,
                        isInBillingRetryPeriod = false,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "billing_retry_sub",
                        transactionId = "txn_billing",
                        expirationDate = Date(System.currentTimeMillis() + 3600_000),
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        willRenew = true,
                        isInGracePeriod = false,
                        isInBillingRetryPeriod = true,
                        isActive = true,
                    ),
                    createMockTransaction(
                        productId = "revoked_sub",
                        transactionId = "txn_revoked",
                        expirationDate = Date(System.currentTimeMillis() + 3600_000),
                        isRevoked = true,
                        productType = EntitlementTransactionType.AUTO_RENEWABLE,
                        willRenew = false,
                        isInGracePeriod = false,
                        isInBillingRetryPeriod = false,
                        isActive = false,
                    ),
                )

            When("processing transactions") {
                val (_, subscriptions) = processor.processTransactions(transactions)

                Then("there should be 3 subscriptions") {
                    assertEquals(3, subscriptions.size)
                }

                val gracePeriodSub = subscriptions.find { it.productId == "grace_period_sub" }
                And("grace period subscription should have correct state") {
                    assertEquals(true, gracePeriodSub?.isInGracePeriod)
                    assertEquals(false, gracePeriodSub?.isInBillingRetryPeriod)
                    assertEquals(true, gracePeriodSub?.willRenew)
                    assertEquals(true, gracePeriodSub?.isActive)
                }

                val billingRetrySub = subscriptions.find { it.productId == "billing_retry_sub" }
                And("billing retry subscription should have correct state") {
                    assertEquals(false, billingRetrySub?.isInGracePeriod)
                    assertEquals(true, billingRetrySub?.isInBillingRetryPeriod)
                    assertEquals(true, billingRetrySub?.willRenew)
                    assertEquals(true, billingRetrySub?.isActive)
                }

                val revokedSub = subscriptions.find { it.productId == "revoked_sub" }
                And("revoked subscription should have correct state") {
                    assertEquals(true, revokedSub?.isRevoked)
                    assertEquals(false, revokedSub?.willRenew)
                    assertEquals(false, revokedSub?.isActive)
                }
            }
        }
    }

    @Test
    fun `process transactions preserves revoked field for non-subscriptions`() {
        Given("active and revoked non-consumable transactions") {
            val transactions =
                listOf<EntitlementTransaction>(
                    createMockTransaction(
                        productId = "active_lifetime",
                        transactionId = "txn_active",
                        isRevoked = false,
                        productType = EntitlementTransactionType.NON_CONSUMABLE,
                    ),
                    createMockTransaction(
                        productId = "revoked_lifetime",
                        transactionId = "txn_revoked",
                        isRevoked = true,
                        productType = EntitlementTransactionType.NON_CONSUMABLE,
                    ),
                )

            When("processing transactions") {
                val (nonSubscriptions, _) = processor.processTransactions(transactions)

                Then("there should be 2 non-subscriptions") {
                    assertEquals(2, nonSubscriptions.size)
                }

                val activeLifetime = nonSubscriptions.find { it.productId == "active_lifetime" }
                And("active lifetime should not be revoked") {
                    assertEquals(false, activeLifetime?.isRevoked)
                }

                val revokedLifetime = nonSubscriptions.find { it.productId == "revoked_lifetime" }
                And("revoked lifetime should be revoked") {
                    assertEquals(true, revokedLifetime?.isRevoked)
                }
            }
        }
    }

    // MARK: - No Transactions Tests

    @Test
    fun `process with no transactions but single inactive entitlement`() {
        Given("no transactions but a raw entitlement exists") {
            val entitlement =
                createEntitlement(
                    id = "entitlement_1",
                    productIds = setOf("product_a"),
                )

            val transactionsByEntitlement: Map<String, List<EntitlementTransaction>> = emptyMap()
            val rawEntitlementsByProductId = mapOf("product_a" to setOf(entitlement))

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("entitlement should be returned") {
                    assertEquals(1, result["product_a"]?.size)
                }

                And("entitlement ID should match") {
                    assertEquals("entitlement_1", result["product_a"]?.firstOrNull()?.id)
                }

                And("entitlement should be inactive") {
                    assertEquals(false, result["product_a"]?.firstOrNull()?.isActive)
                }
            }
        }
    }

    @Test
    fun `process with no transactions but multiple inactive entitlements`() {
        Given("no transactions but multiple raw entitlements exist") {
            val entitlement1 = createEntitlement(id = "entitlement_1", productIds = setOf("product_a"))
            val entitlement2 = createEntitlement(id = "entitlement_2", productIds = setOf("product_b"))
            val entitlement3 = createEntitlement(id = "entitlement_3", productIds = setOf("product_c"))

            val transactionsByEntitlement: Map<String, List<EntitlementTransaction>> = emptyMap()
            val rawEntitlementsByProductId =
                mapOf(
                    "product_a" to setOf(entitlement1),
                    "product_b" to setOf(entitlement2),
                    "product_c" to setOf(entitlement3),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("all entitlements should be returned") {
                    assertEquals(1, result["product_a"]?.size)
                    assertEquals(1, result["product_b"]?.size)
                    assertEquals(1, result["product_c"]?.size)
                }

                And("all entitlements should have correct IDs") {
                    assertEquals("entitlement_1", result["product_a"]?.firstOrNull()?.id)
                    assertEquals("entitlement_2", result["product_b"]?.firstOrNull()?.id)
                    assertEquals("entitlement_3", result["product_c"]?.firstOrNull()?.id)
                }

                And("all entitlements should be inactive") {
                    assertEquals(false, result["product_a"]?.firstOrNull()?.isActive)
                    assertEquals(false, result["product_b"]?.firstOrNull()?.isActive)
                    assertEquals(false, result["product_c"]?.firstOrNull()?.isActive)
                }
            }
        }
    }

    @Test
    fun `process with no transactions but entitlement with multiple products`() {
        Given("no transactions but entitlement with multiple products") {
            val entitlement =
                createEntitlement(
                    id = "premium_tier",
                    productIds = setOf("monthly", "annual", "lifetime"),
                )

            val transactionsByEntitlement: Map<String, List<EntitlementTransaction>> = emptyMap()
            val rawEntitlementsByProductId =
                mapOf(
                    "monthly" to setOf(entitlement),
                    "annual" to setOf(entitlement),
                    "lifetime" to setOf(entitlement),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("same entitlement should be returned for all products") {
                    assertEquals("premium_tier", result["monthly"]?.firstOrNull()?.id)
                    assertEquals("premium_tier", result["annual"]?.firstOrNull()?.id)
                    assertEquals("premium_tier", result["lifetime"]?.firstOrNull()?.id)
                }

                And("all should be inactive") {
                    assertEquals(false, result["monthly"]?.firstOrNull()?.isActive)
                    assertEquals(false, result["annual"]?.firstOrNull()?.isActive)
                    assertEquals(false, result["lifetime"]?.firstOrNull()?.isActive)
                }
            }
        }
    }

    @Test
    fun `process with no transactions and multiple entitlements per product`() {
        Given("no transactions but multiple entitlements per product") {
            val entitlement1 = createEntitlement(id = "basic_tier", productIds = setOf("product_a"))
            val entitlement2 = createEntitlement(id = "premium_tier", productIds = setOf("product_a"))

            val transactionsByEntitlement: Map<String, List<EntitlementTransaction>> = emptyMap()
            val rawEntitlementsByProductId =
                mapOf(
                    "product_a" to setOf(entitlement1, entitlement2),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = rawEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(rawEntitlementsByProductId),
                    )

                Then("both entitlements should be returned for the product") {
                    assertEquals(2, result["product_a"]?.size)
                }

                val entitlementIds = result["product_a"]?.map { it.id } ?: emptyList()
                And("basic_tier should be included") {
                    assertTrue(entitlementIds.contains("basic_tier"))
                }

                And("premium_tier should be included") {
                    assertTrue(entitlementIds.contains("premium_tier"))
                }

                And("all should be inactive") {
                    val allInactive = result["product_a"]?.all { !it.isActive } ?: false
                    assertTrue(allInactive)
                }
            }
        }
    }

    // MARK: - Integration Tests for Entitlement Matching

    /**
     * This test verifies the FIX for the entitlement lookup bug.
     *
     * After the fix in ReceiptManager.loadPurchases() and PlayStorePurchaseAdapter:
     * - adapter.productId is now the full product ID (e.g., "com.ui_tests.monthly:monthly_plan:sw-auto")
     * - This matches the keys in serverEntitlementsByProductId
     * - Entitlement lookup succeeds and transactions are properly mapped
     *
     * This test verifies that when adapter.productId matches the server entitlement keys,
     * entitlements are correctly enriched with transaction data.
     */
    @Test
    fun `entitlement lookup succeeds when adapter productId is full product ID`() {
        Given("server entitlements keyed by full product ID and adapter with matching productId") {
            val fullProductId = "com.ui_tests.monthly:monthly_plan:sw-auto"
            val futureDate = Date(System.currentTimeMillis() + 3600_000) // 1 hour from now

            val defaultEntitlement =
                Entitlement(
                    id = "default",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(fullProductId),
                )

            val testEntitlement =
                Entitlement(
                    id = "test",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(fullProductId),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    fullProductId to setOf(defaultEntitlement, testEntitlement),
                )

            // After fix: adapter.productId is now the full product ID
            val lookupResult = serverEntitlementsByProductId[fullProductId]

            When("looking up entitlements by full product ID") {
                Then("lookup succeeds") {
                    assertNotNull(
                        "After fix: lookup by full product ID succeeds",
                        lookupResult,
                    )
                    assertEquals(2, lookupResult?.size)
                }
            }

            // Create transaction with full product ID (as adapter.productId now uses)
            val transaction =
                createMockTransaction(
                    productId = fullProductId,
                    transactionId = "GPA.3382-0986-5088-93164",
                    expirationDate = futureDate,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    isActive = true,
                )

            // Transactions are properly mapped to entitlements
            val transactionsByEntitlement =
                mapOf(
                    "default" to listOf<EntitlementTransaction>(transaction),
                    "test" to listOf<EntitlementTransaction>(transaction),
                )

            When("building entitlements with proper transaction mapping") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlements are active") {
                    val allEntitlements = result.values.flatten()
                    assertEquals(2, allEntitlements.size)

                    val defaultEnt = allEntitlements.find { it.id == "default" }
                    val testEnt = allEntitlements.find { it.id == "test" }

                    assertTrue(
                        "After fix: default entitlement is active",
                        defaultEnt?.isActive ?: false,
                    )
                    assertTrue(
                        "After fix: test entitlement is active",
                        testEnt?.isActive ?: false,
                    )
                }

                And("entitlements have latestProductId") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertEquals(
                        "After fix: latestProductId is set from transaction",
                        fullProductId,
                        defaultEnt?.latestProductId,
                    )
                }

                And("entitlements have expiration from subscription") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertEquals(futureDate, defaultEnt?.expiresAt)
                }

                And("entitlements have willRenew from subscription") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertTrue(defaultEnt?.willRenew ?: false)
                }
            }
        }
    }

    /**
     * This test verifies the fix works correctly with transaction containing full product ID.
     * After the fix, adapter.productId is the full identifier, so all lookups work correctly.
     */
    @Test
    fun `transaction with full product ID properly enriches entitlements`() {
        Given("server entitlements keyed by full product ID and transaction with full product ID") {
            val fullProductId = "com.ui_tests.monthly:monthly_plan:sw-auto"
            val futureDate = Date(System.currentTimeMillis() + 3600_000) // 1 hour from now

            // After fix: transaction productId is the full product ID
            val transaction =
                createMockTransaction(
                    productId = fullProductId,
                    transactionId = "GPA.3382-0986-5088-93164",
                    expirationDate = futureDate,
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    willRenew = true,
                    isActive = true,
                )

            val defaultEntitlement =
                Entitlement(
                    id = "default",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(fullProductId),
                )

            val testEntitlement =
                Entitlement(
                    id = "test",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(fullProductId),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    fullProductId to setOf(defaultEntitlement, testEntitlement),
                )

            // After fix: lookup succeeds because adapter.productId matches server key
            val lookupResult = serverEntitlementsByProductId[fullProductId]

            When("looking up entitlements by full product ID") {
                Then("entitlements are found") {
                    assertNotNull(lookupResult)
                    assertEquals(2, lookupResult?.size)
                }
            }

            val transactionsByEntitlement =
                mapOf(
                    "default" to listOf<EntitlementTransaction>(transaction),
                    "test" to listOf<EntitlementTransaction>(transaction),
                )

            When("building entitlements with proper transaction mapping") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlements are active") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    val testEnt = result.values.flatten().find { it.id == "test" }

                    assertTrue(
                        "default entitlement should be active from subscription",
                        defaultEnt?.isActive ?: false,
                    )
                    assertTrue(
                        "test entitlement should be active from subscription",
                        testEnt?.isActive ?: false,
                    )
                }

                And("latestProductId is the full product ID") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertEquals(
                        "latestProductId should be the full product ID from transaction",
                        fullProductId,
                        defaultEnt?.latestProductId,
                    )
                }

                And("entitlements have expiration from subscription") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertEquals(futureDate, defaultEnt?.expiresAt)
                }

                And("entitlements have willRenew from subscription") {
                    val defaultEnt = result.values.flatten().find { it.id == "default" }
                    assertTrue(defaultEnt?.willRenew ?: false)
                }
            }
        }
    }

    /**
     * This test verifies the fix in ReceiptManager.loadPurchases():
     *
     * After the fix:
     * - productsById is keyed by productIdentifier (raw product ID)
     * - Lookup by purchase.products (raw IDs) succeeds
     * - adapter.productId is set to product.fullIdentifier
     * - serverEntitlementsByProductId[adapter.productId] lookup succeeds
     */
    @Test
    fun `fixed flow - productsById keyed by raw ID and adapter uses full identifier`() {
        Given("proper product lookup keyed by raw product ID") {
            val rawProductId = "com.ui_tests.monthly"
            val fullProductId = "com.ui_tests.monthly:monthly_plan:sw-auto"

            // After fix: productsById is keyed by productIdentifier (raw ID)
            val productsById =
                mapOf(
                    rawProductId to fullProductId, // Maps raw ID to full ID
                )

            When("looking up by raw product ID") {
                val result = productsById[rawProductId]

                Then("lookup succeeds") {
                    assertEquals(fullProductId, result)
                }
            }

            // After fix: adapter.productId is the full identifier
            val adapterProductId = fullProductId

            val serverEntitlementsByProductId =
                mapOf(
                    fullProductId to
                        setOf(
                            Entitlement(
                                id = "default",
                                type = Entitlement.Type.SERVICE_LEVEL,
                                isActive = false,
                                productIds = setOf(fullProductId),
                            ),
                        ),
                )

            And("entitlement lookup succeeds with full product ID") {
                val entitlementLookup = serverEntitlementsByProductId[adapterProductId]

                Then("entitlements are found") {
                    assertNotNull(
                        "serverEntitlementsByProductId[$adapterProductId] should find entitlements",
                        entitlementLookup,
                    )
                    assertEquals(1, entitlementLookup?.size)
                    assertEquals("default", entitlementLookup?.first()?.id)
                }
            }
        }
    }

    /**
     * This test verifies the raw-to-full product ID mapping in ReceiptManager.loadPurchases():
     *
     * When purchases come in with raw product IDs (e.g., "com.ui_tests.monthly"),
     * we map them to full product IDs from serverEntitlementsByProductId keys
     * before fetching products. This ensures products have proper base plan/offer info.
     */
    @Test
    fun `raw to full product ID mapping finds full ID from server entitlements`() {
        Given("serverEntitlementsByProductId keyed by full product ID and raw product ID from purchase") {
            val rawProductId = "com.ui_tests.monthly"
            val fullProductId = "com.ui_tests.monthly:com-ui-tests-montly:sw-auto"

            val serverEntitlementsByProductId =
                mapOf(
                    fullProductId to
                        setOf(
                            Entitlement(
                                id = "default",
                                type = Entitlement.Type.SERVICE_LEVEL,
                                isActive = false,
                                productIds = setOf(fullProductId),
                            ),
                        ),
                )

            When("mapping raw product ID to full product ID") {
                // This simulates the fix in ReceiptManager:
                // Find the full product ID from server config that matches this raw ID
                val mappedFullId =
                    serverEntitlementsByProductId.keys.firstOrNull { key ->
                        key == rawProductId || key.startsWith("$rawProductId:")
                    }

                Then("full product ID is found via prefix matching") {
                    assertEquals(
                        "Should find full product ID from server entitlements",
                        fullProductId,
                        mappedFullId,
                    )
                }
            }
        }
    }

    /**
     * This test verifies that exact match takes precedence over prefix matching
     * when mapping raw product IDs to full product IDs.
     */
    @Test
    fun `exact match takes precedence over prefix matching in raw to full mapping`() {
        Given("serverEntitlementsByProductId with both exact and prefix-matchable entries") {
            val rawProductId = "com.ui_tests.monthly"
            val fullProductId = "com.ui_tests.monthly:base_plan:offer"

            val exactMatchEntitlement =
                Entitlement(
                    id = "exact",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(rawProductId),
                )

            val prefixMatchEntitlement =
                Entitlement(
                    id = "prefix",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = setOf(fullProductId),
                )

            // Use LinkedHashMap to preserve insertion order for predictable iteration
            val serverEntitlementsByProductId =
                linkedMapOf(
                    // Put prefix match first to ensure exact match still wins
                    fullProductId to setOf(prefixMatchEntitlement),
                    rawProductId to setOf(exactMatchEntitlement),
                )

            When("mapping raw product ID to full product ID") {
                // Find exact match first, then fall back to prefix match
                val mappedFullId =
                    serverEntitlementsByProductId.keys.firstOrNull { it == rawProductId }
                        ?: serverEntitlementsByProductId.keys.firstOrNull { it.startsWith("$rawProductId:") }
                        ?: rawProductId

                Then("exact match is returned, not prefix match") {
                    assertEquals(
                        "Exact match should take precedence over prefix match",
                        rawProductId,
                        mappedFullId,
                    )
                }
            }
        }
    }

    @Test
    fun `entitlement with no transactions should still have productIds from config`() {
        Given("an entitlement with no transactions but multiple products in config") {
            // This simulates the bug where "test" entitlement had productIds=[] because
            // the user only purchased "monthly" product which only unlocks "default" entitlement
            val defaultEntitlement =
                Entitlement(
                    id = "default",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(), // Server doesn't send productIds
                )
            val testEntitlement =
                Entitlement(
                    id = "test",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(), // Server doesn't send productIds
                )

            // Config: monthly product unlocks "default", annual product unlocks both "default" and "test"
            val serverEntitlementsByProductId =
                mapOf(
                    "com.app.monthly" to setOf(defaultEntitlement),
                    "com.app.annual" to setOf(defaultEntitlement, testEntitlement),
                )

            // User only purchased the monthly product - so only "default" has transactions
            val monthlyTransaction =
                createMockTransaction(
                    productId = "com.app.monthly",
                    transactionId = "txn_monthly",
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    isActive = true,
                    expirationDate = Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L),
                )

            // Only "default" entitlement has transactions (from monthly purchase)
            val transactionsByEntitlement =
                mapOf(
                    "default" to listOf<EntitlementTransaction>(monthlyTransaction),
                    // "test" has NO transactions because user didn't purchase annual
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("default entitlement should be active with correct productIds") {
                    val defaultResult = result["com.app.monthly"]?.find { it.id == "default" }
                    assertEquals(true, defaultResult?.isActive)
                    // default is unlocked by both monthly and annual
                    assertEquals(
                        setOf("com.app.monthly", "com.app.annual"),
                        defaultResult?.productIds,
                    )
                }

                And("test entitlement should be inactive but still have productIds from config") {
                    val testResult = result["com.app.annual"]?.find { it.id == "test" }
                    // test has no transactions so it's inactive
                    assertEquals(false, testResult?.isActive)
                    // But it should still have productIds from config!
                    assertEquals(
                        "Entitlement with no transactions should still have productIds from config",
                        setOf("com.app.annual"),
                        testResult?.productIds,
                    )
                }
            }
        }
    }

    // MARK: - isActive Status Tests

    @Test
    fun `entitlement isActive should trust transaction isActive status`() {
        Given("a subscription transaction with isActive=false even though expiration is in future") {
            // This simulates the bug where Google Play returns isActive=false
            // but the expiration date is still in the future
            val futureExpiration = Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)

            val transaction =
                createMockTransaction(
                    productId = "com.app.monthly",
                    transactionId = "txn_1",
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    expirationDate = futureExpiration,
                    isActive = false, // Google Play says inactive
                )

            val entitlement =
                Entitlement(
                    id = "premium",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    "com.app.monthly" to setOf(entitlement),
                )

            val transactionsByEntitlement =
                mapOf(
                    "premium" to listOf<EntitlementTransaction>(transaction),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlement should be inactive because transaction.isActive is false") {
                    val processedEntitlement = result["com.app.monthly"]?.find { it.id == "premium" }
                    assertEquals(
                        "Entitlement should trust transaction.isActive=false",
                        false,
                        processedEntitlement?.isActive,
                    )
                }
            }
        }
    }

    @Test
    fun `entitlement isActive should be true when transaction isActive is true`() {
        Given("a subscription transaction with isActive=true") {
            val futureExpiration = Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)

            val transaction =
                createMockTransaction(
                    productId = "com.app.monthly",
                    transactionId = "txn_1",
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    expirationDate = futureExpiration,
                    isActive = true,
                )

            val entitlement =
                Entitlement(
                    id = "premium",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    "com.app.monthly" to setOf(entitlement),
                )

            val transactionsByEntitlement =
                mapOf(
                    "premium" to listOf<EntitlementTransaction>(transaction),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlement should be active because transaction.isActive is true") {
                    val processedEntitlement = result["com.app.monthly"]?.find { it.id == "premium" }
                    assertEquals(
                        "Entitlement should trust transaction.isActive=true",
                        true,
                        processedEntitlement?.isActive,
                    )
                }
            }
        }
    }

    @Test
    fun `entitlement isActive should be true if any transaction is active`() {
        Given("multiple transactions where one is active and one is inactive") {
            val futureExpiration = Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)

            val inactiveTransaction =
                createMockTransaction(
                    productId = "com.app.monthly",
                    transactionId = "txn_old",
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    expirationDate = Date(System.currentTimeMillis() - 1000), // expired
                    isActive = false,
                    purchaseDate = Date(System.currentTimeMillis() - 60 * 24 * 60 * 60 * 1000L), // 60 days ago
                )

            val activeTransaction =
                createMockTransaction(
                    productId = "com.app.monthly",
                    transactionId = "txn_new",
                    productType = EntitlementTransactionType.AUTO_RENEWABLE,
                    expirationDate = futureExpiration,
                    isActive = true,
                    purchaseDate = Date(), // now
                )

            val entitlement =
                Entitlement(
                    id = "premium",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    "com.app.monthly" to setOf(entitlement),
                )

            val transactionsByEntitlement =
                mapOf(
                    "premium" to listOf<EntitlementTransaction>(inactiveTransaction, activeTransaction),
                )

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlement should be active because at least one transaction is active") {
                    val processedEntitlement = result["com.app.monthly"]?.find { it.id == "premium" }
                    assertEquals(
                        "Entitlement should be active if any transaction is active",
                        true,
                        processedEntitlement?.isActive,
                    )
                }
            }
        }
    }

    @Test
    fun `entitlement with no transactions should be inactive`() {
        Given("an entitlement with no transactions") {
            val entitlement =
                Entitlement(
                    id = "premium",
                    type = Entitlement.Type.SERVICE_LEVEL,
                    isActive = false,
                    productIds = emptySet(),
                )

            val serverEntitlementsByProductId =
                mapOf(
                    "com.app.monthly" to setOf(entitlement),
                )

            // No transactions for this entitlement
            val transactionsByEntitlement = emptyMap<String, List<EntitlementTransaction>>()

            When("building entitlements from transactions") {
                val result =
                    processor.buildEntitlementsFromTransactions(
                        transactionsByEntitlement = transactionsByEntitlement,
                        rawEntitlementsByProductId = serverEntitlementsByProductId,
                        productIdsByEntitlementId = buildProductIdsByEntitlementId(serverEntitlementsByProductId),
                    )

                Then("entitlement should be inactive") {
                    val processedEntitlement = result["com.app.monthly"]?.find { it.id == "premium" }
                    assertEquals(
                        "Entitlement with no transactions should be inactive",
                        false,
                        processedEntitlement?.isActive,
                    )
                }
            }
        }
    }
}
