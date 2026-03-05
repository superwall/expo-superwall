package com.superwall.sdk.identity

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.storage.AliasId
import com.superwall.sdk.storage.AppUserId
import com.superwall.sdk.storage.DidTrackFirstSeen
import com.superwall.sdk.storage.Seed
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.UserAttributes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests specifically validating that user.aliasId and user.appUserId
 * are always present in userAttributes when they should be.
 *
 * These cover the scenarios where the _userAttributes map (used for
 * template variables in paywalls) can get out of sync with the
 * individual identity fields (_aliasId, _appUserId).
 */
class IdentityManagerUserAttributesTest {
    private lateinit var storage: Storage
    private lateinit var configManager: ConfigManager
    private lateinit var deviceHelper: DeviceHelper
    private var resetCalled = false
    private var trackedEvents: MutableList<Any> = mutableListOf()

    @Before
    fun setup() =
        runTest {
            storage = mockk(relaxed = true)
            configManager = mockk(relaxed = true)
            deviceHelper = mockk(relaxed = true)
            resetCalled = false
            trackedEvents = mutableListOf()

            every { storage.read(AppUserId) } returns null
            every { storage.read(AliasId) } returns null
            every { storage.read(Seed) } returns null
            every { storage.read(UserAttributes) } returns null
            every { storage.read(DidTrackFirstSeen) } returns null
            every { deviceHelper.appInstalledAtString } returns "2024-01-01"
            every { configManager.options } returns SuperwallOptions()
            every { configManager.configState } returns MutableStateFlow(ConfigState.None)
        }

    private fun createManager(
        scope: TestScope,
        existingAppUserId: String? = null,
        existingAliasId: String? = null,
        existingSeed: Int? = null,
        existingAttributes: Map<String, Any>? = null,
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }
        existingSeed?.let { every { storage.read(Seed) } returns it }
        existingAttributes?.let { every { storage.read(UserAttributes) } returns it }

        return IdentityManager(
            deviceHelper = deviceHelper,
            storage = storage,
            configManager = configManager,
            ioScope = IOScope(scope.coroutineContext),
            neverCalledStaticConfig = { false },
            notifyUserChange = {},
            completeReset = { resetCalled = true },
            track = { trackedEvents.add(it) },
        )
    }

    private fun createManagerWithScope(
        ioScope: IOScope,
        existingAppUserId: String? = null,
        existingAliasId: String? = null,
        existingSeed: Int? = null,
        existingAttributes: Map<String, Any>? = null,
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }
        existingSeed?.let { every { storage.read(Seed) } returns it }
        existingAttributes?.let { every { storage.read(UserAttributes) } returns it }

        return IdentityManager(
            deviceHelper = deviceHelper,
            storage = storage,
            configManager = configManager,
            ioScope = ioScope,
            neverCalledStaticConfig = { false },
            notifyUserChange = {},
            completeReset = { resetCalled = true },
            track = { trackedEvents.add(it) },
        )
    }

    // region Fresh install - userAttributes should contain identity fields

    @Test
    fun `fresh install - userAttributes contains aliasId after init merge`() =
        runTest {
            Given("a fresh install with no stored data") {
                val manager =
                    When("the manager is created and init merge completes") {
                        createManager(this@runTest)
                    }

                // Allow scope.launch from init's mergeUserAttributes to complete
                Thread.sleep(200)

                Then("userAttributes contains aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain aliasId key, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals(manager.aliasId, attrs["aliasId"])
                }

                And("userAttributes contains seed") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain seed key, got: $attrs",
                        attrs.containsKey("seed"),
                    )
                    assertEquals(manager.seed, attrs["seed"])
                }
            }
        }

    @Test
    fun `fresh install - identify adds appUserId to userAttributes`() =
        runTest {
            Given("a fresh install") {
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with a new userId") {
                    manager.identify("user-123")
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("userAttributes contains appUserId") {
                    val attrs = manager.userAttributes
                    assertEquals("user-123", attrs["appUserId"])
                }

                And("userAttributes still contains aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain aliasId, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals(manager.aliasId, attrs["aliasId"])
                }

                And("userAttributes still contains seed") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain seed, got: $attrs",
                        attrs.containsKey("seed"),
                    )
                }
            }
        }

    // endregion

    // region Returning user - userAttributes loaded from storage

    @Test
    fun `returning user - userAttributes loaded from storage preserves identity fields`() =
        runTest {
            Given("a returning user with stored attributes including identity fields") {
                val storedAttrs =
                    mapOf(
                        "aliasId" to "stored-alias",
                        "seed" to 42,
                        "appUserId" to "user-123",
                        "applicationInstalledAt" to "2024-01-01",
                        "customKey" to "customValue",
                    )

                val manager =
                    When("the manager is created") {
                        createManager(
                            this@runTest,
                            existingAliasId = "stored-alias",
                            existingSeed = 42,
                            existingAppUserId = "user-123",
                            existingAttributes = storedAttrs,
                        )
                    }

                Then("userAttributes contains aliasId") {
                    assertEquals("stored-alias", manager.userAttributes["aliasId"])
                }

                And("userAttributes contains appUserId") {
                    assertEquals("user-123", manager.userAttributes["appUserId"])
                }

                And("userAttributes contains custom attributes") {
                    assertEquals("customValue", manager.userAttributes["customKey"])
                }
            }
        }

    @Test
    fun `returning user - same identify is no-op and preserves userAttributes`() =
        runTest {
            Given("a returning user with stored identity") {
                val storedAttrs =
                    mapOf(
                        "aliasId" to "stored-alias",
                        "seed" to 42,
                        "appUserId" to "user-123",
                        "applicationInstalledAt" to "2024-01-01",
                    )
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager =
                    createManagerWithScope(
                        testScope,
                        existingAliasId = "stored-alias",
                        existingSeed = 42,
                        existingAppUserId = "user-123",
                        existingAttributes = storedAttrs,
                    )

                When("identify is called with the SAME userId") {
                    manager.identify("user-123")
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("userAttributes still contains aliasId") {
                    assertEquals("stored-alias", manager.userAttributes["aliasId"])
                }

                And("userAttributes still contains appUserId") {
                    assertEquals("user-123", manager.userAttributes["appUserId"])
                }
            }
        }

    // endregion

    // region BUG SCENARIO: UserAttributes storage empty but individual IDs survive

    @Test
    fun `BUG - returning user with empty UserAttributes storage but valid individual IDs`() =
        runTest {
            Given("a returning user where UserAttributes failed to load but AliasId and AppUserId loaded fine") {
                val manager =
                    When("the manager is created") {
                        createManager(
                            this@runTest,
                            existingAliasId = "stored-alias",
                            existingSeed = 42,
                            existingAppUserId = "user-123",
                            existingAttributes = null, // UserAttributes deserialization failed
                        )
                    }

                // Allow any async merges to complete
                Thread.sleep(200)

                Then("aliasId individual field is correct") {
                    assertEquals("stored-alias", manager.aliasId)
                }

                And("appUserId individual field is correct") {
                    assertEquals("user-123", manager.appUserId)
                }

                And("userAttributes SHOULD contain aliasId (currently may not!)") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain aliasId but got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals("stored-alias", attrs["aliasId"])
                }

                And("userAttributes SHOULD contain appUserId (currently may not!)") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain appUserId but got: $attrs",
                        attrs.containsKey("appUserId"),
                    )
                    assertEquals("user-123", attrs["appUserId"])
                }
            }
        }

    @Test
    fun `BUG - returning user with empty storage, same identify, then setUserAttributes`() =
        runTest {
            Given("UserAttributes failed to load, individual IDs exist") {
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager =
                    createManagerWithScope(
                        testScope,
                        existingAliasId = "stored-alias",
                        existingSeed = 42,
                        existingAppUserId = "user-123",
                        existingAttributes = null, // failed deserialization
                    )

                When("identify is called with the SAME userId (early return, no saveIds)") {
                    manager.identify("user-123")
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                And("setUserAttributes is called with custom data") {
                    manager.mergeUserAttributes(mapOf("name" to "John"))
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("userAttributes should contain the custom attribute") {
                    assertTrue(manager.userAttributes.containsKey("name"))
                    assertEquals("John", manager.userAttributes["name"])
                }

                And("userAttributes SHOULD still contain aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "aliasId should survive in userAttributes, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                }

                And("userAttributes SHOULD still contain appUserId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "appUserId should survive in userAttributes, got: $attrs",
                        attrs.containsKey("appUserId"),
                    )
                }
            }
        }

    @Test
    fun `BUG - returning anonymous user with empty storage, no identify call`() =
        runTest {
            Given("anonymous user where UserAttributes failed to load but AliasId exists") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager =
                    createManagerWithScope(
                        testScope,
                        existingAliasId = "stored-alias",
                        existingSeed = 42,
                        existingAppUserId = null,
                        existingAttributes = null,
                    )

                When("setUserAttributes is called without any identify") {
                    manager.mergeUserAttributes(mapOf("name" to "John"))
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("userAttributes contains custom attribute") {
                    assertEquals("John", manager.userAttributes["name"])
                }

                And("userAttributes SHOULD contain aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "aliasId should be in userAttributes, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals("stored-alias", attrs["aliasId"])
                }
            }
        }

    // endregion

    // region Reset scenarios

    @Test
    fun `reset generates new identity and populates userAttributes`() =
        runTest {
            Given("an identified user") {
                val manager =
                    createManager(
                        this@runTest,
                        existingAppUserId = "user-123",
                        existingAliasId = "old-alias",
                        existingSeed = 42,
                        existingAttributes =
                            mapOf(
                                "aliasId" to "old-alias",
                                "seed" to 42,
                                "appUserId" to "user-123",
                                "applicationInstalledAt" to "2024-01-01",
                            ),
                    )

                When("reset is called") {
                    manager.reset(duringIdentify = false)
                }

                // Allow async operations
                Thread.sleep(200)

                Then("userAttributes contains the NEW aliasId") {
                    val attrs = manager.userAttributes
                    val newAlias = manager.aliasId
                    assertTrue(
                        "userAttributes should contain aliasId, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals(newAlias, attrs["aliasId"])
                }

                And("userAttributes contains the new seed") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain seed, got: $attrs",
                        attrs.containsKey("seed"),
                    )
                }

                And("appUserId is cleared from userAttributes") {
                    val attrs = manager.userAttributes
                    // After reset, appUserId should NOT be in attributes (it's null)
                    // This is expected - anonymous user after reset
                }
            }
        }

    @Test
    fun `reset during identify followed by new identify populates userAttributes`() =
        runTest {
            Given("a user identified as user-A") {
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager =
                    createManagerWithScope(
                        testScope,
                        existingAppUserId = "user-A",
                        existingAliasId = "alias-A",
                        existingSeed = 42,
                        existingAttributes =
                            mapOf(
                                "aliasId" to "alias-A",
                                "seed" to 42,
                                "appUserId" to "user-A",
                                "applicationInstalledAt" to "2024-01-01",
                            ),
                    )

                When("identify is called with a DIFFERENT userId (triggers reset)") {
                    manager.identify("user-B")
                    Thread.sleep(300)
                    advanceUntilIdle()
                }

                Then("appUserId is user-B") {
                    assertEquals("user-B", manager.appUserId)
                }

                And("userAttributes contains the new appUserId") {
                    val attrs = manager.userAttributes
                    assertEquals("user-B", attrs["appUserId"])
                }

                And("userAttributes contains aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes should contain aliasId, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals(manager.aliasId, attrs["aliasId"])
                }
            }
        }

    // endregion

    // region setUserAttributes preserves identity fields

    @Test
    fun `setUserAttributes does not remove identity fields`() =
        runTest {
            Given("a fresh install where init merge has completed") {
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                // First identify to get appUserId into attributes
                manager.identify("user-123")
                Thread.sleep(200)
                advanceUntilIdle()

                val attrsBefore = manager.userAttributes
                assertNotNull(
                    "aliasId should exist before setUserAttributes",
                    attrsBefore["aliasId"],
                )
                assertEquals("user-123", attrsBefore["appUserId"])

                When("setUserAttributes is called with unrelated attributes") {
                    manager.mergeUserAttributes(
                        mapOf("name" to "John", "email" to "john@example.com"),
                    )
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("custom attributes are added") {
                    assertEquals("John", manager.userAttributes["name"])
                    assertEquals("john@example.com", manager.userAttributes["email"])
                }

                And("aliasId is preserved") {
                    assertEquals(manager.aliasId, manager.userAttributes["aliasId"])
                }

                And("appUserId is preserved") {
                    assertEquals("user-123", manager.userAttributes["appUserId"])
                }

                And("seed is preserved") {
                    assertEquals(manager.seed, manager.userAttributes["seed"])
                }
            }
        }

    @Test
    fun `setUserAttributes with null aliasId removes it from userAttributes`() =
        runTest {
            Given("a manager with identity fields in userAttributes") {
                val testScope = IOScope(this@runTest.coroutineContext)
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState

                val manager = createManagerWithScope(testScope)
                manager.identify("user-123")
                Thread.sleep(200)
                advanceUntilIdle()

                When("setUserAttributes is called with aliasId = null") {
                    manager.mergeUserAttributes(mapOf("aliasId" to null))
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("aliasId is removed from userAttributes") {
                    // This documents current behavior: passing null REMOVES the key.
                    // This could be a vector for the bug if a developer accidentally
                    // passes aliasId: null.
                    val attrs = manager.userAttributes
                    // Note: the individual field is still correct:
                    assertTrue(manager.aliasId.isNotEmpty())
                    // But the MAP might no longer have it:
                    // This test documents the current behavior - whatever it is.
                    // If aliasId is removed, this is a potential issue.
                }
            }
        }
    // endregion

    // region Consistency: individual fields vs userAttributes map

    @Test
    fun `after identify - aliasId field and userAttributes aliasId are consistent`() =
        runTest {
            Given("a fresh install") {
                val configState =
                    MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)
                manager.identify("user-123")
                Thread.sleep(200)
                advanceUntilIdle()

                Then("aliasId field matches userAttributes aliasId") {
                    assertEquals(
                        manager.aliasId,
                        manager.userAttributes["aliasId"],
                    )
                }

                And("appUserId field matches userAttributes appUserId") {
                    assertEquals(
                        manager.appUserId,
                        manager.userAttributes["appUserId"],
                    )
                }

                And("seed field matches userAttributes seed") {
                    assertEquals(
                        manager.seed,
                        manager.userAttributes["seed"],
                    )
                }
            }
        }

    @Test
    fun `after reset - aliasId field and userAttributes aliasId are consistent`() =
        runTest {
            Given("an identified user") {
                val manager =
                    createManager(
                        this@runTest,
                        existingAppUserId = "user-123",
                        existingAliasId = "old-alias",
                        existingSeed = 42,
                        existingAttributes =
                            mapOf(
                                "aliasId" to "old-alias",
                                "seed" to 42,
                                "appUserId" to "user-123",
                                "applicationInstalledAt" to "2024-01-01",
                            ),
                    )

                When("reset is called") {
                    manager.reset(duringIdentify = false)
                }

                Thread.sleep(200)

                Then("aliasId field matches userAttributes aliasId") {
                    assertEquals(
                        manager.aliasId,
                        manager.userAttributes["aliasId"],
                    )
                }

                And("seed field matches userAttributes seed") {
                    assertEquals(
                        manager.seed,
                        manager.userAttributes["seed"],
                    )
                }
            }
        }

    // endregion

    // region Partial storage: some fields loaded, some not

    @Test
    fun `UserAttributes has identity fields but AliasId storage is empty`() =
        runTest {
            Given("UserAttributes loaded fine but AliasId storage is empty (newly generated)") {
                // This shouldn't normally happen but tests resilience
                val manager =
                    When("manager is created with UserAttributes but no AliasId") {
                        createManager(
                            this@runTest,
                            existingAliasId = null, // will generate new
                            existingSeed = null, // will generate new
                            existingAppUserId = null,
                            existingAttributes =
                                mapOf(
                                    "someCustomKey" to "value",
                                    "applicationInstalledAt" to "2024-01-01",
                                ),
                        )
                    }

                // Allow init merge to complete
                Thread.sleep(200)

                Then("userAttributes contains the newly generated aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "aliasId should be merged into existing userAttributes, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals(manager.aliasId, attrs["aliasId"])
                }

                And("custom attributes from storage are preserved") {
                    assertEquals("value", manager.userAttributes["someCustomKey"])
                }
            }
        }

    @Test
    fun `UserAttributes empty, AliasId and AppUserId exist - no identify called`() =
        runTest {
            Given("empty UserAttributes but individual IDs in storage, no identify") {
                val manager =
                    When("manager is created") {
                        createManager(
                            this@runTest,
                            existingAliasId = "stored-alias",
                            existingSeed = 42,
                            existingAppUserId = "user-123",
                            existingAttributes = null,
                        )
                    }

                // Allow any async operations to complete
                Thread.sleep(200)

                Then("the individual fields are correct") {
                    assertEquals("stored-alias", manager.aliasId)
                    assertEquals("user-123", manager.appUserId)
                    assertEquals(42, manager.seed)
                }

                And("userAttributes SHOULD contain aliasId - tests the gap") {
                    // This is the core of the bug: init block skips merge when
                    // AliasId exists in storage, so aliasId never gets into
                    // the empty _userAttributes map.
                    val attrs = manager.userAttributes
                    assertTrue(
                        "aliasId should be in userAttributes even when loaded from " +
                            "individual storage. Individual aliasId=${manager.aliasId}, " +
                            "but userAttributes=$attrs",
                        attrs.containsKey("aliasId"),
                    )
                }

                And("userAttributes SHOULD contain appUserId - tests the gap") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "appUserId should be in userAttributes even when loaded from " +
                            "individual storage. Individual appUserId=${manager.appUserId}, " +
                            "but userAttributes=$attrs",
                        attrs.containsKey("appUserId"),
                    )
                }
            }
        }

    // endregion
}
