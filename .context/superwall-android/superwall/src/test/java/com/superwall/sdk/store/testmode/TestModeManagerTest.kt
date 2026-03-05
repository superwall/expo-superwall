@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.IsTestModeActiveSubscription
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallEntitlementRef
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.models.SuperwallProductPrice
import com.superwall.sdk.store.testmode.models.SuperwallProductSubscription
import com.superwall.sdk.store.testmode.models.SuperwallSubscriptionPeriod
import com.superwall.sdk.store.testmode.models.TestStoreUser
import com.superwall.sdk.store.testmode.models.TestStoreUserType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestModeManagerTest {
    private fun makeStorage(): Storage = mockk(relaxed = true)

    private fun makeManager(
        storage: Storage = makeStorage(),
        isTestEnvironment: Boolean = false,
    ): TestModeManager = TestModeManager(storage, isTestEnvironment)

    private fun makeConfig(
        bundleIdConfig: String? = null,
        testModeUserIds: List<TestStoreUser>? = null,
    ): Config {
        val config = mockk<Config>(relaxed = true)
        every { config.bundleIdConfig } returns bundleIdConfig
        every { config.testModeUserIds } returns testModeUserIds
        return config
    }

    private fun makeSuperwallProduct(
        identifier: String = "com.test.product",
        entitlements: List<SuperwallEntitlementRef> = emptyList(),
    ): SuperwallProduct =
        SuperwallProduct(
            objectType = "product",
            identifier = identifier,
            platform = SuperwallProductPlatform.ANDROID,
            price = SuperwallProductPrice(amount = 999, currency = "USD"),
            subscription =
                SuperwallProductSubscription(
                    period = SuperwallSubscriptionPeriod.MONTH,
                    periodCount = 1,
                ),
            entitlements = entitlements,
        )

    /** Activates test mode via ALWAYS behavior so session data can be written. */
    private fun activateTestMode(manager: TestModeManager) {
        manager.evaluateTestMode(
            makeConfig(),
            "com.app",
            null,
            null,
            testModeBehavior = TestModeBehavior.ALWAYS,
        )
    }

    // region evaluateTestMode (AUTOMATIC behavior, isTestEnvironment = false)

    @Test
    fun `evaluateTestMode activates for applicationId mismatch`() {
        Given("a TestModeManager with a config that has a different applicationId") {
            val manager = makeManager()
            val config = makeConfig(bundleIdConfig = "com.expected.app")

            When("evaluateTestMode is called with a different applicationId") {
                manager.evaluateTestMode(config, "com.actual.app", null, null)
            }

            Then("test mode is active with ApplicationIdMismatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ApplicationIdMismatch)
                val reason = manager.testModeReason as TestModeReason.ApplicationIdMismatch
                assertEquals("com.expected.app", reason.expected)
                assertEquals("com.actual.app", reason.actual)
            }
        }
    }

    @Test
    fun `evaluateTestMode does not activate for matching bundleId`() {
        Given("a TestModeManager with a config that has the same applicationId") {
            val manager = makeManager()
            val config = makeConfig(bundleIdConfig = "com.myapp")

            When("evaluateTestMode is called with the same applicationId") {
                manager.evaluateTestMode(config, "com.myapp", null, null)
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `evaluateTestMode does not activate for null bundleIdConfig`() {
        Given("a config with null bundleIdConfig") {
            val manager = makeManager()
            val config = makeConfig(bundleIdConfig = null)

            When("evaluateTestMode is called") {
                manager.evaluateTestMode(config, "com.any.app", null, null)
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `evaluateTestMode does not activate for empty bundleIdConfig`() {
        Given("a config with empty bundleIdConfig") {
            val manager = makeManager()
            val config = makeConfig(bundleIdConfig = "")

            When("evaluateTestMode is called") {
                manager.evaluateTestMode(config, "com.any.app", null, null)
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `evaluateTestMode activates for userId match`() {
        Given("a TestModeManager with a config containing a matching userId") {
            val manager = makeManager()
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "test-user-123"),
                        ),
                )

            When("evaluateTestMode is called with matching userId") {
                manager.evaluateTestMode(config, "com.app", "test-user-123", "some-alias")
            }

            Then("test mode is active with ConfigMatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ConfigMatch)
                val reason = manager.testModeReason as TestModeReason.ConfigMatch
                assertEquals("test-user-123", reason.matchedId)
            }
        }
    }

    @Test
    fun `evaluateTestMode activates for aliasId match`() {
        Given("a TestModeManager with a config containing a matching aliasId") {
            val manager = makeManager()
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.AliasId, "alias-456"),
                        ),
                )

            When("evaluateTestMode is called with matching aliasId") {
                manager.evaluateTestMode(config, "com.app", "some-user", "alias-456")
            }

            Then("test mode is active with ConfigMatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ConfigMatch)
                val reason = manager.testModeReason as TestModeReason.ConfigMatch
                assertEquals("alias-456", reason.matchedId)
            }
        }
    }

    @Test
    fun `evaluateTestMode matches first user when multiple configured`() {
        Given("a config with multiple test users") {
            val manager = makeManager()
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "user-A"),
                            TestStoreUser(TestStoreUserType.AliasId, "alias-B"),
                            TestStoreUser(TestStoreUserType.UserId, "user-C"),
                        ),
                )

            When("evaluateTestMode is called with the second user matching") {
                manager.evaluateTestMode(config, "com.app", "no-match", "alias-B")
            }

            Then("test mode is active with the matched aliasId") {
                assertTrue(manager.isTestMode)
                val reason = manager.testModeReason as TestModeReason.ConfigMatch
                assertEquals("alias-B", reason.matchedId)
            }
        }
    }

    @Test
    fun `evaluateTestMode does not activate when no conditions match`() {
        Given("a TestModeManager with a config with non-matching users") {
            val manager = makeManager()
            val config =
                makeConfig(
                    bundleIdConfig = "com.app",
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "other-user"),
                        ),
                )

            When("evaluateTestMode is called with non-matching ids") {
                manager.evaluateTestMode(config, "com.app", "my-user", "my-alias")
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `evaluateTestMode prioritizes config match over applicationId mismatch in AUTOMATIC`() {
        Given("a config with both applicationId mismatch and matching userId") {
            val manager = makeManager()
            val config =
                makeConfig(
                    bundleIdConfig = "com.expected.app",
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "matching-user"),
                        ),
                )

            When("evaluateTestMode is called") {
                manager.evaluateTestMode(config, "com.actual.app", "matching-user", null)
            }

            Then("reason is ConfigMatch because config match is checked first") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ConfigMatch)
            }
        }
    }

    // endregion

    // region TestModeBehavior

    @Test
    fun `testModeBehavior NEVER disables test mode even with matching config`() {
        Given("a manager with NEVER behavior and a matching config") {
            val manager = makeManager()
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "matching-user"),
                        ),
                )

            When("evaluateTestMode is called with NEVER behavior") {
                manager.evaluateTestMode(
                    config,
                    "com.app",
                    "matching-user",
                    null,
                    testModeBehavior = TestModeBehavior.NEVER,
                )
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `testModeBehavior ALWAYS enables test mode even with no match`() {
        Given("a manager with ALWAYS behavior and no matching config") {
            val manager = makeManager()
            val config = makeConfig()

            When("evaluateTestMode is called with ALWAYS behavior") {
                manager.evaluateTestMode(
                    config,
                    "com.app",
                    null,
                    null,
                    testModeBehavior = TestModeBehavior.ALWAYS,
                )
            }

            Then("test mode is active with TestModeOption reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.TestModeOption)
            }
        }
    }

    @Test
    fun `testModeBehavior WHEN_ENABLED_FOR_USER activates on config match`() {
        Given("a manager with WHEN_ENABLED_FOR_USER behavior and matching userId") {
            val manager = makeManager()
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "test-user"),
                        ),
                )

            When("evaluateTestMode is called with WHEN_ENABLED_FOR_USER") {
                manager.evaluateTestMode(
                    config,
                    "com.app",
                    "test-user",
                    null,
                    testModeBehavior = TestModeBehavior.WHEN_ENABLED_FOR_USER,
                )
            }

            Then("test mode is active with ConfigMatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ConfigMatch)
            }
        }
    }

    @Test
    fun `testModeBehavior WHEN_ENABLED_FOR_USER does NOT activate on package name mismatch`() {
        Given("a manager with WHEN_ENABLED_FOR_USER behavior and package name mismatch") {
            val manager = makeManager()
            val config = makeConfig(bundleIdConfig = "com.expected.app")

            When("evaluateTestMode is called with WHEN_ENABLED_FOR_USER") {
                manager.evaluateTestMode(
                    config,
                    "com.actual.app",
                    null,
                    null,
                    testModeBehavior = TestModeBehavior.WHEN_ENABLED_FOR_USER,
                )
            }

            Then("test mode is not active") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `testModeBehavior AUTOMATIC skips in test environment`() {
        Given("a manager in a test environment with matching config") {
            val manager = makeManager(isTestEnvironment = true)
            val config =
                makeConfig(
                    bundleIdConfig = "com.expected.app",
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "matching-user"),
                        ),
                )

            When("evaluateTestMode is called with AUTOMATIC in test env") {
                manager.evaluateTestMode(
                    config,
                    "com.actual.app",
                    "matching-user",
                    null,
                    testModeBehavior = TestModeBehavior.AUTOMATIC,
                )
            }

            Then("test mode is not active because test environment is detected") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `testModeBehavior AUTOMATIC activates on config match when not in test env`() {
        Given("a manager not in test env with matching userId") {
            val manager = makeManager(isTestEnvironment = false)
            val config =
                makeConfig(
                    testModeUserIds =
                        listOf(
                            TestStoreUser(TestStoreUserType.UserId, "test-user"),
                        ),
                )

            When("evaluateTestMode is called with AUTOMATIC") {
                manager.evaluateTestMode(
                    config,
                    "com.app",
                    "test-user",
                    null,
                    testModeBehavior = TestModeBehavior.AUTOMATIC,
                )
            }

            Then("test mode is active with ConfigMatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ConfigMatch)
            }
        }
    }

    @Test
    fun `testModeBehavior AUTOMATIC activates on package name mismatch when not in test env`() {
        Given("a manager not in test env with package name mismatch") {
            val manager = makeManager(isTestEnvironment = false)
            val config = makeConfig(bundleIdConfig = "com.expected.app")

            When("evaluateTestMode is called with AUTOMATIC") {
                manager.evaluateTestMode(
                    config,
                    "com.actual.app",
                    null,
                    null,
                    testModeBehavior = TestModeBehavior.AUTOMATIC,
                )
            }

            Then("test mode is active with ApplicationIdMismatch reason") {
                assertTrue(manager.isTestMode)
                assertTrue(manager.testModeReason is TestModeReason.ApplicationIdMismatch)
            }
        }
    }

    @Test
    fun `package name prefix is treated as extension and does NOT activate test mode`() {
        Given("a manager with a config packageName and actual is a prefix extension") {
            val manager = makeManager(isTestEnvironment = false)
            val config = makeConfig(bundleIdConfig = "com.myapp")

            When("evaluateTestMode is called with a package name that starts with expected + dot") {
                manager.evaluateTestMode(
                    config,
                    "com.myapp.debug",
                    null,
                    null,
                    testModeBehavior = TestModeBehavior.AUTOMATIC,
                )
            }

            Then("test mode is not active because it is treated as an extension") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    // endregion

    // region shouldShowFreeTrial

    @Test
    fun `shouldShowFreeTrial applies UseDefault correctly`() {
        Given("a TestModeManager with UseDefault override") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.setFreeTrialOverride(FreeTrialOverride.UseDefault)

            Then("it returns the original value") {
                assertTrue(manager.shouldShowFreeTrial(true))
                assertFalse(manager.shouldShowFreeTrial(false))
            }
        }
    }

    @Test
    fun `shouldShowFreeTrial applies ForceAvailable correctly`() {
        Given("a TestModeManager with ForceAvailable override") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.setFreeTrialOverride(FreeTrialOverride.ForceAvailable)

            Then("it always returns true") {
                assertTrue(manager.shouldShowFreeTrial(true))
                assertTrue(manager.shouldShowFreeTrial(false))
            }
        }
    }

    @Test
    fun `shouldShowFreeTrial applies ForceUnavailable correctly`() {
        Given("a TestModeManager with ForceUnavailable override") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.setFreeTrialOverride(FreeTrialOverride.ForceUnavailable)

            Then("it always returns false") {
                assertFalse(manager.shouldShowFreeTrial(true))
                assertFalse(manager.shouldShowFreeTrial(false))
            }
        }
    }

    @Test
    fun `shouldShowFreeTrial returns default when inactive`() {
        Given("a TestModeManager that is inactive") {
            val manager = makeManager()

            Then("it returns the original value regardless") {
                assertTrue(manager.shouldShowFreeTrial(true))
                assertFalse(manager.shouldShowFreeTrial(false))
            }
        }
    }

    // endregion

    // region fakePurchase

    @Test
    fun `fakePurchase adds entitlement IDs`() {
        Given("a TestModeManager with active test mode") {
            val storage = makeStorage()
            val manager = makeManager(storage)
            activateTestMode(manager)

            When("fakePurchase is called with entitlements") {
                manager.fakePurchase(
                    listOf(
                        SuperwallEntitlementRef("premium", null),
                        SuperwallEntitlementRef("pro", null),
                    ),
                )
            }

            Then("entitlements are added and persisted") {
                assertTrue(manager.testEntitlementIds.contains("premium"))
                assertTrue(manager.testEntitlementIds.contains("pro"))
                verify { storage.write(IsTestModeActiveSubscription, true) }
            }
        }
    }

    @Test
    fun `fakePurchase accumulates entitlements across calls`() {
        Given("a TestModeManager with existing entitlements") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.fakePurchase(listOf(SuperwallEntitlementRef("premium", null)))

            When("fakePurchase is called again with different entitlements") {
                manager.fakePurchase(listOf(SuperwallEntitlementRef("pro", null)))
            }

            Then("all entitlements are present") {
                assertEquals(setOf("premium", "pro"), manager.testEntitlementIds)
            }
        }
    }

    // endregion

    // region setEntitlements / resetEntitlements

    @Test
    fun `setEntitlements replaces existing entitlements`() {
        Given("a TestModeManager with existing entitlements") {
            val storage = makeStorage()
            val manager = makeManager(storage)
            activateTestMode(manager)
            manager.setEntitlements(setOf("old-a", "old-b"))

            When("setEntitlements is called with new set") {
                manager.setEntitlements(setOf("new-x"))
            }

            Then("only new entitlements remain") {
                assertEquals(setOf("new-x"), manager.testEntitlementIds)
                verify { storage.write(IsTestModeActiveSubscription, true) }
            }
        }
    }

    @Test
    fun `setEntitlements with empty set writes false`() {
        Given("a TestModeManager with active test mode") {
            val storage = makeStorage()
            val manager = makeManager(storage)
            activateTestMode(manager)

            When("setEntitlements is called with an empty set") {
                manager.setEntitlements(emptySet())
            }

            Then("storage is updated with false") {
                assertTrue(manager.testEntitlementIds.isEmpty())
                verify { storage.write(IsTestModeActiveSubscription, false) }
            }
        }
    }

    @Test
    fun `resetEntitlements clears all and writes false`() {
        Given("a TestModeManager with entitlements") {
            val storage = makeStorage()
            val manager = makeManager(storage)
            activateTestMode(manager)
            manager.setEntitlements(setOf("premium", "pro"))

            When("resetEntitlements is called") {
                manager.resetEntitlements()
            }

            Then("entitlements are empty and storage is updated") {
                assertTrue(manager.testEntitlementIds.isEmpty())
                verify { storage.write(IsTestModeActiveSubscription, false) }
            }
        }
    }

    // endregion

    // region buildSubscriptionStatus

    @Test
    fun `buildSubscriptionStatus returns Active with entitlements`() {
        Given("a TestModeManager with entitlements set") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.setEntitlements(setOf("premium", "pro"))

            When("buildSubscriptionStatus is called") {
                val status = manager.buildSubscriptionStatus()

                Then("it returns Active with correct entitlements") {
                    assertTrue(status is SubscriptionStatus.Active)
                    val active = status as SubscriptionStatus.Active
                    assertTrue(active.entitlements.any { it.id == "premium" && it.isActive })
                    assertTrue(active.entitlements.any { it.id == "pro" && it.isActive })
                    assertEquals(2, active.entitlements.size)
                }
            }
        }
    }

    @Test
    fun `buildSubscriptionStatus returns Inactive when empty`() {
        Given("a TestModeManager with no entitlements") {
            val manager = makeManager()

            When("buildSubscriptionStatus is called") {
                val status = manager.buildSubscriptionStatus()

                Then("it returns Inactive") {
                    assertTrue(status is SubscriptionStatus.Inactive)
                }
            }
        }
    }

    // endregion

    // region setProducts / setTestProducts

    @Test
    fun `setProducts stores products`() {
        Given("a TestModeManager with active test mode") {
            val manager = makeManager()
            activateTestMode(manager)
            val products =
                listOf(
                    makeSuperwallProduct("prod-1"),
                    makeSuperwallProduct("prod-2"),
                )

            When("setProducts is called") {
                manager.setProducts(products)
            }

            Then("products are accessible") {
                assertEquals(2, manager.products.size)
                assertEquals("prod-1", manager.products[0].identifier)
                assertEquals("prod-2", manager.products[1].identifier)
            }
        }
    }

    @Test
    fun `setTestProducts stores test product map`() {
        Given("a TestModeManager with active test mode") {
            val manager = makeManager()
            activateTestMode(manager)
            val sp = mockk<StoreProduct>(relaxed = true)
            val map = mapOf("prod-1" to sp)

            When("setTestProducts is called") {
                manager.setTestProducts(map)
            }

            Then("testProductsByFullId is populated") {
                assertEquals(1, manager.testProductsByFullId.size)
                assertEquals(sp, manager.testProductsByFullId["prod-1"])
            }
        }
    }

    // endregion

    // region allEntitlements / entitlementsForProduct

    @Test
    fun `allEntitlements aggregates from all products`() {
        Given("a TestModeManager with products that have entitlements") {
            val manager = makeManager()
            activateTestMode(manager)
            manager.setProducts(
                listOf(
                    makeSuperwallProduct(
                        "prod-1",
                        entitlements =
                            listOf(
                                SuperwallEntitlementRef("premium", null),
                                SuperwallEntitlementRef("ads_free", null),
                            ),
                    ),
                    makeSuperwallProduct(
                        "prod-2",
                        entitlements =
                            listOf(
                                SuperwallEntitlementRef("premium", null),
                                SuperwallEntitlementRef("pro", null),
                            ),
                    ),
                ),
            )

            When("allEntitlements is called") {
                val all = manager.allEntitlements()

                Then("it returns the union of all entitlements") {
                    assertEquals(setOf("premium", "ads_free", "pro"), all)
                }
            }
        }
    }

    @Test
    fun `allEntitlements returns empty when no products`() {
        Given("a TestModeManager with no products") {
            val manager = makeManager()

            Then("allEntitlements returns empty set") {
                assertTrue(manager.allEntitlements().isEmpty())
            }
        }
    }

    @Test
    fun `entitlementsForProduct returns entitlements for given product`() {
        Given("a TestModeManager with a product that has entitlements") {
            val manager = makeManager()
            val product =
                makeSuperwallProduct(
                    "prod-1",
                    entitlements =
                        listOf(
                            SuperwallEntitlementRef("premium", null),
                            SuperwallEntitlementRef("pro", "type1"),
                        ),
                )

            When("entitlementsForProduct is called") {
                val refs = manager.entitlementsForProduct(product)

                Then("it returns the product's entitlements") {
                    assertEquals(2, refs.size)
                    assertEquals("premium", refs[0].identifier)
                    assertEquals("pro", refs[1].identifier)
                }
            }
        }
    }

    // endregion

    // region setOverriddenSubscriptionStatus

    @Test
    fun `setOverriddenSubscriptionStatus stores and clears status`() {
        Given("a TestModeManager with active test mode") {
            val manager = makeManager()
            activateTestMode(manager)

            When("a status is set") {
                val status = SubscriptionStatus.Active(setOf(Entitlement("premium")))
                manager.setOverriddenSubscriptionStatus(status)

                Then("it is retrievable") {
                    assertTrue(manager.overriddenSubscriptionStatus is SubscriptionStatus.Active)
                }
            }

            When("status is set to null") {
                manager.setOverriddenSubscriptionStatus(null)

                Then("it is cleared") {
                    assertNull(manager.overriddenSubscriptionStatus)
                }
            }
        }
    }

    // endregion

    // region clearTestModeState

    @Test
    fun `clearTestModeState resets all fields`() {
        Given("a TestModeManager with active test mode") {
            val storage = makeStorage()
            val manager = makeManager(storage)
            val config =
                makeConfig(
                    testModeUserIds = listOf(TestStoreUser(TestStoreUserType.UserId, "user")),
                )
            manager.evaluateTestMode(config, "com.app", "user", null)
            manager.setEntitlements(setOf("premium"))
            manager.setProducts(listOf(makeSuperwallProduct("prod-1")))
            manager.setTestProducts(mapOf("prod-1" to mockk(relaxed = true)))
            manager.setFreeTrialOverride(FreeTrialOverride.ForceAvailable)
            manager.setOverriddenSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("premium"))))

            When("clearTestModeState is called") {
                manager.clearTestModeState()
            }

            Then("all fields are reset") {
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
                assertTrue(manager.products.isEmpty())
                assertTrue(manager.testProductsByFullId.isEmpty())
                assertTrue(manager.testEntitlementIds.isEmpty())
                assertTrue(manager.testEntitlementSelections.isEmpty())
                assertEquals(FreeTrialOverride.UseDefault, manager.freeTrialOverride)
                assertNull(manager.overriddenSubscriptionStatus)
                verify { storage.delete(IsTestModeActiveSubscription) }
            }
        }
    }

    // endregion

    // region state transitions

    @Test
    fun `state transitions from Inactive to Active and back`() {
        Given("a fresh TestModeManager") {
            val manager = makeManager()

            Then("initial state is Inactive") {
                assertTrue(manager.state is TestModeState.Inactive)
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }

            When("test mode is activated via ALWAYS") {
                manager.evaluateTestMode(
                    makeConfig(),
                    "com.app",
                    null,
                    null,
                    testModeBehavior = TestModeBehavior.ALWAYS,
                )
            }

            Then("state is Active with TestModeOption reason") {
                val state = manager.state
                assertTrue(state is TestModeState.Active)
                assertEquals(TestModeReason.TestModeOption, (state as TestModeState.Active).reason)
                assertTrue(manager.isTestMode)
                assertEquals(TestModeReason.TestModeOption, manager.testModeReason)
            }

            When("clearTestModeState is called") {
                manager.clearTestModeState()
            }

            Then("state is back to Inactive") {
                assertTrue(manager.state is TestModeState.Inactive)
                assertFalse(manager.isTestMode)
                assertNull(manager.testModeReason)
            }
        }
    }

    @Test
    fun `session data is only accessible when active`() {
        Given("a fresh TestModeManager") {
            val manager = makeManager()

            Then("session data returns defaults when inactive") {
                assertTrue(manager.products.isEmpty())
                assertTrue(manager.testProductsByFullId.isEmpty())
                assertTrue(manager.testEntitlementIds.isEmpty())
                assertTrue(manager.testEntitlementSelections.isEmpty())
                assertEquals(FreeTrialOverride.UseDefault, manager.freeTrialOverride)
                assertNull(manager.overriddenSubscriptionStatus)
            }

            When("test mode is activated and session data is set") {
                activateTestMode(manager)
                manager.setProducts(listOf(makeSuperwallProduct("prod-1")))
                manager.setEntitlements(setOf("premium"))
                manager.setFreeTrialOverride(FreeTrialOverride.ForceAvailable)
            }

            Then("session data is accessible") {
                assertEquals(1, manager.products.size)
                assertEquals(setOf("premium"), manager.testEntitlementIds)
                assertEquals(FreeTrialOverride.ForceAvailable, manager.freeTrialOverride)
            }

            When("test mode is cleared") {
                manager.clearTestModeState()
            }

            Then("session data returns defaults again") {
                assertTrue(manager.products.isEmpty())
                assertTrue(manager.testEntitlementIds.isEmpty())
                assertEquals(FreeTrialOverride.UseDefault, manager.freeTrialOverride)
            }
        }
    }

    // endregion
}
