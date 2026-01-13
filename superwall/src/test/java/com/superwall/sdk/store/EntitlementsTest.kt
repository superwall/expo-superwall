package com.superwall.sdk.store

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.models.product.Store
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

    @Test
    fun `test byProductId matches partial product IDs with contains logic`() =
        runTest {
            Given("storage contains entitlement with key subscription_monthly colon p1m-high") {
                val premiumEntitlement = Entitlement("premium_access")
                val wrongEntitlement = Entitlement("fake")
                val productEntitlements =
                    mapOf(
                        "subscription_monthly:p1m-high" to setOf(wrongEntitlement),
                        "subscription_monthly:p1m:freetrial" to setOf(premiumEntitlement),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage)

                When("querying with subscription_monthly colon p1m colon freetrial") {
                    val result = entitlements.byProductId("subscription_monthly:p1m:freetrial")

                    Then(
                        "it should match the entitlement because subscription_monthly colon p1m is contained in subscription_monthly colon p1m-high",
                    ) {
                        assertEquals(setOf(premiumEntitlement), result)
                    }
                }
            }
        }

    @Test
    fun `test activeDeviceEntitlements returns only active device entitlements not all`() =
        runTest {
            Given("entitlements with both active and inactive device entitlements") {
                val activeEntitlement = Entitlement("premium", isActive = true)
                val inactiveEntitlement = Entitlement("basic", isActive = false)
                val productEntitlements =
                    mapOf(
                        "product_premium" to setOf(activeEntitlement),
                        "product_basic" to setOf(inactiveEntitlement),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage)

                When("setting active device entitlements to only the active one") {
                    entitlements.activeDeviceEntitlements = setOf(activeEntitlement)

                    Then("activeDeviceEntitlements should only contain the active entitlement") {
                        assertEquals(setOf(activeEntitlement), entitlements.activeDeviceEntitlements)
                    }

                    And("all should contain both entitlements") {
                        assertTrue(entitlements.all.contains(activeEntitlement))
                        assertTrue(entitlements.all.contains(inactiveEntitlement))
                    }

                    And("activeDeviceEntitlements should NOT equal all") {
                        assertTrue(
                            "activeDeviceEntitlements should not return all entitlements - " +
                                "this would cause inactive entitlements to be treated as active",
                            entitlements.activeDeviceEntitlements != entitlements.all,
                        )
                    }
                }
            }
        }

    @Test
    fun `test activeDeviceEntitlements is empty when no active device purchases`() =
        runTest {
            Given("entitlements from config but no active device purchases") {
                val configEntitlement1 = Entitlement("default", isActive = false)
                val configEntitlement2 = Entitlement("test", isActive = false)
                val productEntitlements =
                    mapOf(
                        "product_monthly" to setOf(configEntitlement1),
                        "product_annual" to setOf(configEntitlement1, configEntitlement2),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage)

                When("no active device entitlements are set") {
                    // activeDeviceEntitlements not set, should be empty

                    Then("activeDeviceEntitlements should be empty") {
                        assertTrue(
                            "activeDeviceEntitlements should be empty when no device purchases are active",
                            entitlements.activeDeviceEntitlements.isEmpty(),
                        )
                    }

                    And("all should still contain config entitlements") {
                        assertTrue(entitlements.all.isNotEmpty())
                    }
                }
            }
        }

    @Test
    fun `test activeDeviceEntitlements cleared on subscription status change to inactive`() =
        runTest {
            Given("entitlements with active device entitlements") {
                val activeEntitlement = Entitlement("premium", isActive = true)
                val inactiveEntitlement = Entitlement("basic", isActive = false)
                val productEntitlements =
                    mapOf(
                        "product_premium" to setOf(activeEntitlement),
                        "product_basic" to setOf(inactiveEntitlement),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage, scope = backgroundScope)
                entitlements.activeDeviceEntitlements = setOf(activeEntitlement)

                When("subscription status is set to Inactive") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Inactive)

                    Then("activeDeviceEntitlements should be cleared") {
                        assertTrue(
                            "activeDeviceEntitlements should be cleared when status becomes Inactive",
                            entitlements.activeDeviceEntitlements.isEmpty(),
                        )
                    }

                    And("all should still contain config entitlements") {
                        assertTrue(entitlements.all.contains(activeEntitlement))
                        assertTrue(entitlements.all.contains(inactiveEntitlement))
                    }
                }
            }
        }

    @Test
    fun `test active getter combines backingActive and activeDeviceEntitlements correctly`() =
        runTest {
            Given("entitlements with both status-based and device-based active entitlements") {
                val statusActiveEntitlement = Entitlement("status_active", isActive = true)
                val deviceActiveEntitlement = Entitlement("device_active", isActive = true)
                val inactiveEntitlement = Entitlement("inactive", isActive = false)
                val productEntitlements =
                    mapOf(
                        "product_status" to setOf(statusActiveEntitlement),
                        "product_device" to setOf(deviceActiveEntitlement),
                        "product_inactive" to setOf(inactiveEntitlement),
                    )
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns productEntitlements
                entitlements = Entitlements(storage, scope = backgroundScope)

                When("setting both status and device entitlements") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(statusActiveEntitlement)))
                    entitlements.activeDeviceEntitlements = setOf(deviceActiveEntitlement)

                    Then("active should contain both status and device entitlements") {
                        assertTrue(entitlements.active.contains(statusActiveEntitlement))
                        assertTrue(entitlements.active.contains(deviceActiveEntitlement))
                    }

                    And("active should NOT contain inactive entitlements") {
                        assertTrue(
                            "active should not contain inactive entitlements from config",
                            !entitlements.active.contains(inactiveEntitlement),
                        )
                    }

                    And("activeDeviceEntitlements should only contain device entitlements") {
                        assertEquals(setOf(deviceActiveEntitlement), entitlements.activeDeviceEntitlements)
                    }
                }
            }
        }

    // ==========================================
    // Web Entitlements Tests
    // ==========================================

    @Test
    fun `test web property returns active entitlements from storage`() =
        runTest {
            Given("storage contains web entitlements in LatestRedemptionResponse") {
                val webEntitlement1 = Entitlement("web_pro", isActive = true, store = Store.STRIPE)
                val webEntitlement2 = Entitlement("web_basic", isActive = true, store = Store.STRIPE)
                val webEntitlement3 = Entitlement("web_inactive", isActive = false, store = Store.PADDLE)
                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement1, webEntitlement2, webEntitlement3),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("accessing the web property") {
                    entitlements = Entitlements(storage)

                    Then("it should return only active web entitlements") {
                        assertEquals(setOf(webEntitlement1, webEntitlement2), entitlements.web)
                    }
                }
            }
        }

    @Test
    fun `test web property filters out inactive entitlements`() =
        runTest {
            Given("storage contains mixed active and inactive web entitlements") {
                val activeWebEntitlement = Entitlement("web_active", isActive = true, store = Store.STRIPE)
                val inactiveWebEntitlement = Entitlement("web_inactive", isActive = false, store = Store.STRIPE)
                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(activeWebEntitlement, inactiveWebEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("accessing the web property") {
                    entitlements = Entitlements(storage)

                    Then("it should return only active web entitlements") {
                        assertEquals(setOf(activeWebEntitlement), entitlements.web)
                        assertTrue(!entitlements.web.contains(inactiveWebEntitlement))
                    }
                }
            }
        }

    @Test
    fun `test web property returns empty when no redemption response`() =
        runTest {
            Given("storage has no LatestRedemptionResponse") {
                every { storage.read(LatestRedemptionResponse) } returns null
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("accessing the web property") {
                    entitlements = Entitlements(storage)

                    Then("it should return empty set") {
                        assertTrue(entitlements.web.isEmpty())
                    }
                }
            }
        }

    // ==========================================
    // External Purchase Controller + Web Entitlements Tests
    // ==========================================

    @Test
    fun `test active property merges backingActive with web entitlements`() =
        runTest {
            Given("entitlements with both status-based and web entitlements") {
                val statusEntitlement = Entitlement("play_store_pro", isActive = true, store = Store.PLAY_STORE)
                val webEntitlement = Entitlement("web_feature", isActive = true, store = Store.STRIPE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("setting subscription status (simulating external PC)") {
                    entitlements = Entitlements(storage, scope = backgroundScope)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(statusEntitlement)))

                    Then("active should contain both status and web entitlements") {
                        assertTrue(entitlements.active.any { it.id == "play_store_pro" })
                        assertTrue(entitlements.active.any { it.id == "web_feature" })
                        assertEquals(2, entitlements.active.size)
                    }

                    And("web property should still return web entitlements independently") {
                        assertEquals(setOf(webEntitlement), entitlements.web)
                    }
                }
            }
        }

    @Test
    fun `test external PC scenario - web entitlements accessible after status set by external controller`() =
        runTest {
            Given("an external purchase controller scenario") {
                // Simulating RevenueCat setting entitlements (Play Store only)
                val rcEntitlement = Entitlement("rc_premium", isActive = true, store = Store.PLAY_STORE)

                // Web entitlements from Superwall backend
                val webEntitlement = Entitlement("stripe_addon", isActive = true, store = Store.STRIPE)
                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("external PC sets status with only its entitlements") {
                    entitlements = Entitlements(storage, scope = backgroundScope)
                    // External PC sets status (like RC does)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(rcEntitlement)))

                    Then("web entitlements should still be accessible via web property") {
                        assertEquals(setOf(webEntitlement), entitlements.web)
                    }

                    And("active should automatically include web entitlements via merge") {
                        val activeEntitlements = entitlements.active
                        assertTrue(activeEntitlements.any { it.id == "rc_premium" })
                        assertTrue(activeEntitlements.any { it.id == "stripe_addon" })
                    }
                }
            }
        }

    @Test
    fun `test external PC merges web entitlements manually (RC controller pattern)`() =
        runTest {
            Given("an external purchase controller that merges web entitlements") {
                val rcEntitlement = Entitlement("rc_premium", isActive = true, store = Store.PLAY_STORE)
                val webEntitlement = Entitlement("web_addon", isActive = true, store = Store.STRIPE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                entitlements = Entitlements(storage, scope = backgroundScope)

                When("external PC reads web entitlements and merges them into status") {
                    // This simulates what the updated RC controller does:
                    // 1. Get RC entitlements
                    // 2. Read web entitlements
                    // 3. Merge and set status
                    val webFromStorage = entitlements.web
                    val allEntitlements = setOf(rcEntitlement) + webFromStorage
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(allEntitlements))

                    Then("status should contain both RC and web entitlements") {
                        val status = entitlements.status.value
                        assertTrue(status is SubscriptionStatus.Active)
                        val activeStatus = status as SubscriptionStatus.Active
                        assertTrue(activeStatus.entitlements.any { it.id == "rc_premium" })
                        assertTrue(activeStatus.entitlements.any { it.id == "web_addon" })
                    }

                    And("active property should also contain both") {
                        assertTrue(entitlements.active.any { it.id == "rc_premium" })
                        assertTrue(entitlements.active.any { it.id == "web_addon" })
                    }
                }
            }
        }

    // ==========================================
    // Reset and Re-identify Flow Tests
    // ==========================================

    @Test
    fun `test reset flow - web entitlements persist in storage after status reset`() =
        runTest {
            Given("user has web entitlements and active status") {
                val webEntitlement = Entitlement("web_pro", isActive = true, store = Store.STRIPE)
                val playEntitlement = Entitlement("play_pro", isActive = true, store = Store.PLAY_STORE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "userA",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                entitlements = Entitlements(storage, scope = backgroundScope)
                entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(playEntitlement)))

                When("status is reset to Inactive (simulating sign out)") {
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Inactive)

                    Then("web entitlements should still be accessible from storage") {
                        // Storage still has web entitlements
                        assertEquals(setOf(webEntitlement), entitlements.web)
                    }

                    And("active should only contain web entitlements (since status is inactive but web persists)") {
                        // active = backingActive + activeDeviceEntitlements + web
                        // backingActive is cleared, activeDeviceEntitlements is cleared, but web still reads from storage
                        assertEquals(setOf(webEntitlement), entitlements.active)
                    }
                }
            }
        }

    @Test
    fun `test re-identify flow - web entitlements restored after reset and re-identify`() =
        runTest {
            Given("user A had web entitlements, reset, and re-identifies") {
                val webEntitlement = Entitlement("web_pro", isActive = true, store = Store.STRIPE)
                val newPlayEntitlement = Entitlement("new_play_pro", isActive = true, store = Store.PLAY_STORE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "userA",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                entitlements = Entitlements(storage, scope = backgroundScope)

                // Initial state
                entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("old_play"))))

                When("user resets and external PC sets new status after re-identify") {
                    // Reset
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Inactive)

                    // Re-identify - external PC fetches and sets new status
                    // Web entitlements are also re-fetched from backend (simulated by storage still having them)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(newPlayEntitlement)))

                    Then("active should contain both new play entitlement and web entitlement") {
                        assertTrue(entitlements.active.any { it.id == "new_play_pro" })
                        assertTrue(entitlements.active.any { it.id == "web_pro" })
                    }

                    And("web property should return web entitlements") {
                        assertEquals(setOf(webEntitlement), entitlements.web)
                    }
                }
            }
        }

    @Test
    fun `test reset with different user - simulates user switch scenario`() =
        runTest {
            Given("user A has entitlements, then user B identifies") {
                // User A's web entitlements
                val userAWebEntitlement = Entitlement("userA_web", isActive = true, store = Store.STRIPE)
                val userAWebInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "userA",
                        entitlements = listOf(userAWebEntitlement),
                        isPlaceholder = false,
                    )
                val userARedemption =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = userAWebInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns userARedemption
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                entitlements = Entitlements(storage, scope = backgroundScope)
                entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("userA_play"))))

                When("user B identifies and storage is updated with user B's web entitlements") {
                    // Reset for user switch
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Inactive)

                    // Storage is updated with user B's web entitlements (simulating backend fetch)
                    val userBWebEntitlement = Entitlement("userB_web", isActive = true, store = Store.STRIPE)
                    val userBWebInfo =
                        CustomerInfo(
                            subscriptions = emptyList(),
                            nonSubscriptions = emptyList(),
                            userId = "userB",
                            entitlements = listOf(userBWebEntitlement),
                            isPlaceholder = false,
                        )
                    val userBRedemption =
                        WebRedemptionResponse(
                            codes = emptyList(),
                            allCodes = emptyList(),
                            customerInfo = userBWebInfo,
                        )
                    every { storage.read(LatestRedemptionResponse) } returns userBRedemption

                    // User B's external PC sets status
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("userB_play"))))

                    Then("web property should return user B's web entitlements") {
                        assertEquals(setOf(userBWebEntitlement), entitlements.web)
                    }

                    And("active should contain user B's entitlements only") {
                        assertTrue(entitlements.active.any { it.id == "userB_play" })
                        assertTrue(entitlements.active.any { it.id == "userB_web" })
                        assertTrue(!entitlements.active.any { it.id == "userA_web" })
                        assertTrue(!entitlements.active.any { it.id == "userA_play" })
                    }
                }
            }
        }

    // ==========================================
    // All Three Sources Combined Tests
    // ==========================================

    @Test
    fun `test active merges all three sources - backingActive, deviceEntitlements, and web`() =
        runTest {
            Given("entitlements from all three sources") {
                val statusEntitlement = Entitlement("from_status", isActive = true)
                val deviceEntitlement = Entitlement("from_device", isActive = true)
                val webEntitlement = Entitlement("from_web", isActive = true, store = Store.STRIPE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("all three sources have different entitlements") {
                    entitlements = Entitlements(storage, scope = backgroundScope)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(statusEntitlement)))
                    entitlements.activeDeviceEntitlements = setOf(deviceEntitlement)

                    Then("active should contain all three entitlements") {
                        val active = entitlements.active
                        assertEquals(3, active.size)
                        assertTrue(active.any { it.id == "from_status" })
                        assertTrue(active.any { it.id == "from_device" })
                        assertTrue(active.any { it.id == "from_web" })
                    }
                }
            }
        }

    @Test
    fun `test duplicate entitlement IDs are deduplicated via merge`() =
        runTest {
            Given("same entitlement ID from multiple sources with different properties") {
                // Same ID "premium" from status and web, but different properties
                val statusPremium = Entitlement("premium", isActive = true, store = Store.PLAY_STORE)
                val webPremium = Entitlement("premium", isActive = true, store = Store.STRIPE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webPremium),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("both sources have entitlement with same ID") {
                    entitlements = Entitlements(storage, scope = backgroundScope)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Active(setOf(statusPremium)))

                    Then("active should deduplicate and contain only one premium entitlement") {
                        val premiumEntitlements = entitlements.active.filter { it.id == "premium" }
                        assertEquals(1, premiumEntitlements.size)
                    }
                }
            }
        }

    @Test
    fun `test web entitlements included in active even when status is Unknown`() =
        runTest {
            Given("status is Unknown but web entitlements exist") {
                val webEntitlement = Entitlement("web_pro", isActive = true, store = Store.STRIPE)

                val webCustomerInfo =
                    CustomerInfo(
                        subscriptions = emptyList(),
                        nonSubscriptions = emptyList(),
                        userId = "user123",
                        entitlements = listOf(webEntitlement),
                        isPlaceholder = false,
                    )
                val redemptionResponse =
                    WebRedemptionResponse(
                        codes = emptyList(),
                        allCodes = emptyList(),
                        customerInfo = webCustomerInfo,
                    )

                every { storage.read(LatestRedemptionResponse) } returns redemptionResponse
                every { storage.read(StoredSubscriptionStatus) } returns null
                every { storage.read(StoredEntitlementsByProductId) } returns null

                When("status is set to Unknown") {
                    entitlements = Entitlements(storage, scope = backgroundScope)
                    entitlements.setSubscriptionStatus(SubscriptionStatus.Unknown)

                    Then("web property should still return web entitlements") {
                        assertEquals(setOf(webEntitlement), entitlements.web)
                    }

                    And("active should include web entitlements despite Unknown status") {
                        // active = backingActive + activeDeviceEntitlements + web
                        // Unknown clears backingActive and activeDeviceEntitlements, but web still reads from storage
                        assertTrue(entitlements.active.contains(webEntitlement))
                    }
                }
            }
        }
}
