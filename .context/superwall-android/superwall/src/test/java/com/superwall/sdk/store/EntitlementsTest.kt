package com.superwall.sdk.store

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StoredEntitlementsByProductId
import com.superwall.sdk.storage.StoredSubscriptionStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EntitlementsTest {
    private val storage: Storage =
        mockk(relaxUnitFun = true) {
            every { read(LatestRedemptionResponse) } returns null
        }

    private lateinit var entitlements: Entitlements

    @Test
    fun `test initialization with stored entitlement status`() =
        runTest {
            Given("storage contains entitlement status") {
                val storedEntitlements = setOf(Entitlement("test_entitlement"))
                val storedStatus = SubscriptionStatus.Active(storedEntitlements)
                every { storage.read(StoredSubscriptionStatus) } returns storedStatus
                every { storage.read(StoredEntitlementsByProductId) } returns
                    mapOf(
                        "test" to
                            setOf(
                                Entitlement("test_entitlement"),
                            ),
                    )
                entitlements = Entitlements(storage)

                When("Entitlements is initialized") {
                    val entitlements = Entitlements(storage)

                    Then("it should load the stored status") {
                        assertEquals(storedStatus, entitlements.status.value)
                        assertEquals(storedEntitlements, entitlements.active)
                        assertEquals(storedEntitlements, entitlements.all)
                        assertTrue(entitlements.inactive.isEmpty())
                    }
                }
            }
        }

    @Test
    fun `test setEntitlementStatus with active entitlements`() =
        runTest {
            Given("an Entitlements instance") {
                val activeEntitlements =
                    setOf(
                        Entitlement("entitlement1"),
                        Entitlement("entitlement2"),
                    )
                every {
                    storage.write(
                        StoredSubscriptionStatus,
                        SubscriptionStatus.Active(activeEntitlements),
                    )
                } just Runs
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null
                entitlements = Entitlements(storage, scope = backgroundScope)

                When("setting active entitlement status") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(activeEntitlements))
                    Then("it should update all collections correctly") {
                        assertEquals(activeEntitlements, entitlements.active)
                        assertEquals(activeEntitlements, entitlements.all)
                        assertTrue(entitlements.inactive.isEmpty())
                        assertTrue(entitlements.status.value is SubscriptionStatus.Active)
                    }
                    async(Dispatchers.Default) {
                        delay(1.seconds)
                    }.await()
                    And("it should store the status") {
                        verify {
                            storage.write(
                                StoredSubscriptionStatus,
                                SubscriptionStatus.Active(activeEntitlements),
                            )
                        }
                    }
                }
            }
        }

    @Test
    fun `test setEntitlementStatus with empty active entitlements`() =
        runTest {
            Given("an Entitlements instance") {
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null
                entitlements = Entitlements(storage)

                When("setting active entitlement status with empty set") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(emptySet()))

                    Then("it should convert to NoActiveEntitlements status") {
                        assertTrue(entitlements.status.value is SubscriptionStatus.Inactive)
                        assertTrue(entitlements.active.isEmpty())
                        assertTrue(entitlements.inactive.isEmpty())
                    }
                }
            }
        }

    @Test
    fun `test setEntitlementStatus with no active entitlements`() =
        runTest {
            Given("an Entitlements instance") {
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null
                entitlements = Entitlements(storage)
                When("setting NoActiveEntitlements status") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Inactive)

                    Then("it should clear all collections") {
                        assertTrue(entitlements.active.isEmpty())
                        assertTrue(entitlements.inactive.isEmpty())
                        assertTrue(entitlements.status.value is SubscriptionStatus.Inactive)
                    }
                }
            }
        }

    @Test
    fun `test setEntitlementStatus with unknown status`() =
        runTest {
            Given("an Entitlements instance") {
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null
                entitlements = Entitlements(storage)
                When("setting Unknown status") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Unknown)

                    Then("it should clear all collections") {
                        assertTrue(entitlements.active.isEmpty())
                        assertTrue(entitlements.inactive.isEmpty())
                        assertTrue(entitlements.all.isEmpty())
                        assertTrue(entitlements.status.value is SubscriptionStatus.Unknown)
                    }
                }
            }
        }

    @Test
    fun `test byProductId functionality`() =
        runTest {
            Given("storage contains entitlements by product ID") {
                val productEntitlements =
                    mapOf(
                        "product1" to setOf(Entitlement("entitlement1")),
                        "product2" to setOf(Entitlement("entitlement2")),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement("entitlement1"),
                            Entitlement("entitlement2"),
                        ),
                    )
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage)

                When("creating a new Entitlements instance") {
                    Then("it should return correct entitlements for each product") {
                        assertEquals(
                            productEntitlements["product1"],
                            entitlements.byProductId("product1"),
                        )
                        assertEquals(
                            productEntitlements["product2"],
                            entitlements.byProductId("product2"),
                        )
                    }

                    And("it should return empty set for unknown product") {
                        assertTrue(entitlements.byProductId("unknown_product").isEmpty())
                    }
                }
            }
        }
}
