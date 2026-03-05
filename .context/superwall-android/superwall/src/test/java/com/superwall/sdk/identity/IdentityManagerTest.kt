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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentityManagerTest {
    private lateinit var storage: Storage
    private lateinit var configManager: ConfigManager
    private lateinit var deviceHelper: DeviceHelper
    private var notifiedChanges: MutableList<Map<String, Any>> = mutableListOf()
    private var resetCalled = false
    private var trackedEvents: MutableList<Any> = mutableListOf()

    @Before
    fun setup() {
        storage = mockk(relaxed = true)
        configManager = mockk(relaxed = true)
        deviceHelper = mockk(relaxed = true)
        notifiedChanges = mutableListOf()
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

    /**
     * Creates an IdentityManager using Dispatchers.Unconfined for ioScope.
     * Use for synchronous (non-coroutine) tests only.
     */
    private fun createManager(
        dispatcher: TestScope,
        existingAppUserId: String? = null,
        existingAliasId: String? = null,
        existingSeed: Int? = null,
        existingAttributes: Map<String, Any>? = null,
        neverCalledStaticConfig: Boolean = false,
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }
        existingSeed?.let { every { storage.read(Seed) } returns it }
        existingAttributes?.let { every { storage.read(UserAttributes) } returns it }

        return IdentityManager(
            deviceHelper = deviceHelper,
            storage = storage,
            configManager = configManager,
            ioScope = IOScope(dispatcher.coroutineContext),
            neverCalledStaticConfig = { neverCalledStaticConfig },
            notifyUserChange = { notifiedChanges.add(it) },
            completeReset = { resetCalled = true },
            track = { trackedEvents.add(it) },
        )
    }

    /**
     * Creates an IdentityManager using the provided IOScope.
     * Use for coroutine tests where the ioScope should be shared with runTest.
     */
    private fun createManagerWithScope(
        ioScope: IOScope,
        existingAppUserId: String? = null,
        existingAliasId: String? = null,
        neverCalledStaticConfig: Boolean = false,
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }

        return IdentityManager(
            deviceHelper = deviceHelper,
            storage = storage,
            configManager = configManager,
            ioScope = ioScope,
            neverCalledStaticConfig = { neverCalledStaticConfig },
            notifyUserChange = { notifiedChanges.add(it) },
            completeReset = { resetCalled = true },
            track = { trackedEvents.add(it) },
        )
    }

    // region Init

    @Test
    fun `init generates and persists aliasId when none stored`() =
        runTest {
            Given("no existing aliasId in storage") {
                val manager =
                    When("the manager is created") {
                        createManager(this@runTest)
                    }

                Then("a new aliasId is generated and written") {
                    verify { storage.write(AliasId, any<String>()) }
                    assertTrue(manager.aliasId.isNotEmpty())
                }
            }
        }

    @Test
    fun `init generates and persists seed when none stored`() =
        runTest {
            Given("no existing seed in storage") {
                val manager =
                    When("the manager is created") {
                        createManager(this@runTest)
                    }

                Then("a new seed is generated and written") {
                    verify { storage.write(Seed, any<Int>()) }
                    assertTrue(manager.seed in 0..99)
                }
            }
        }

    @Test
    fun `init uses existing aliasId from storage`() =
        runTest {
            Given("an existing aliasId in storage") {
                val existingAlias = "existing-alias-123"

                val manager =
                    When("the manager is created") {
                        createManager(this@runTest, existingAliasId = existingAlias)
                    }

                Then("it uses the stored aliasId") {
                    assertEquals(existingAlias, manager.aliasId)
                }
            }
        }

    @Test
    fun `init uses existing seed from storage`() =
        runTest {
            Given("an existing seed in storage") {
                val existingSeed = 42

                val manager =
                    When("the manager is created") {
                        createManager(this@runTest, existingSeed = existingSeed)
                    }

                Then("it uses the stored seed") {
                    assertEquals(existingSeed, manager.seed)
                }
            }
        }

    @Test
    fun `init merges generated alias and seed into user attributes`() =
        runTest {
            Given("no existing aliasId or seed in storage") {
                When("the manager is created") {
                    createManager(this@runTest)
                }

                Then("user attributes are written to storage") {
                    verify { storage.write(UserAttributes, any()) }
                }
            }
        }

    @Test
    fun `init does not merge attributes when alias and seed already exist`() =
        runTest {
            Given("existing aliasId and seed in storage") {
                When("the manager is created") {
                    createManager(this@runTest, existingAliasId = "existing", existingSeed = 50)
                }

                Then("user attributes are not merged during init") {
                    verify(exactly = 0) { storage.write(UserAttributes, any()) }
                }
            }
        }

    // endregion

    // region userId

    @Test
    fun `userId returns aliasId when no appUserId set`() =
        runTest {
            Given("no logged in user") {
                val manager = createManager(this@runTest, existingAliasId = "alias-abc")

                val userId =
                    When("userId is accessed") {
                        manager.userId
                    }

                Then("it returns the aliasId") {
                    assertEquals("alias-abc", userId)
                }
            }
        }

    @Test
    fun `userId returns appUserId when logged in`() =
        runTest {
            Given("a logged in user") {
                val manager =
                    createManager(
                        this@runTest,
                        existingAppUserId = "user-123",
                        existingAliasId = "alias-abc",
                    )

                val userId =
                    When("userId is accessed") {
                        manager.userId
                    }

                Then("it returns the appUserId") {
                    assertEquals("user-123", userId)
                }
            }
        }

    @Test
    fun `isLoggedIn is false when no appUserId`() =
        runTest {
            Given("no appUserId stored") {
                val manager = createManager(this@runTest)

                Then("isLoggedIn is false") {
                    assertFalse(manager.isLoggedIn)
                }
            }
        }

    @Test
    fun `isLoggedIn is true when appUserId exists`() =
        runTest {
            Given("an appUserId stored") {
                val manager = createManager(this@runTest, existingAppUserId = "user-123")

                Then("isLoggedIn is true") {
                    assertTrue(manager.isLoggedIn)
                }
            }
        }

    // endregion

    // region externalAccountId

    @Test
    fun `externalAccountId returns userId directly when passIdentifiersToPlayStore is true`() =
        runTest {
            Given("passIdentifiersToPlayStore is enabled") {
                val options = SuperwallOptions().apply { passIdentifiersToPlayStore = true }
                every { configManager.options } returns options

                val manager = createManager(this@runTest, existingAppUserId = "user-123")

                val externalId =
                    When("externalAccountId is accessed") {
                        manager.externalAccountId
                    }

                Then("it returns the raw userId") {
                    assertEquals("user-123", externalId)
                }
            }
        }

    @Test
    fun `externalAccountId returns sha of userId when passIdentifiersToPlayStore is false`() =
        runTest {
            Given("passIdentifiersToPlayStore is disabled") {
                val options = SuperwallOptions().apply { passIdentifiersToPlayStore = false }
                every { configManager.options } returns options

                val manager =
                    IdentityManager(
                        deviceHelper = deviceHelper,
                        storage = storage,
                        configManager = configManager,
                        ioScope = IOScope(Dispatchers.Unconfined),
                        neverCalledStaticConfig = { false },
                        stringToSha = { "sha256-of-$it" },
                        notifyUserChange = {},
                        completeReset = {},
                        track = {},
                    )

                val externalId =
                    When("externalAccountId is accessed") {
                        manager.externalAccountId
                    }

                Then("it returns the sha of the userId") {
                    assertTrue(externalId.startsWith("sha256-of-"))
                }
            }
        }

    // endregion

    // region reset

    @Test
    fun `reset clears appUserId and generates new alias and seed`() =
        runTest {
            Given("a logged in user") {
                val manager =
                    createManager(
                        this@runTest,
                        existingAppUserId = "user-123",
                        existingAliasId = "old-alias",
                        existingSeed = 42,
                    )
                val oldAlias = manager.aliasId

                When("reset is called not during identify") {
                    manager.reset(duringIdentify = false)
                }

                Then("appUserId is cleared") {
                    assertNull(manager.appUserId)
                }

                And("a new aliasId is generated") {
                    assertNotEquals(oldAlias, manager.aliasId)
                }

                And("appUserId deletion is persisted") {
                    verify { storage.delete(AppUserId) }
                }
            }
        }

    @Test
    fun `reset during identify does not emit identity`() =
        runTest {
            Given("a logged in user") {
                val manager = createManager(this@runTest, existingAppUserId = "user-123")

                When("reset is called during identify") {
                    manager.reset(duringIdentify = true)
                }

                Then("appUserId is cleared") {
                    assertNull(manager.appUserId)
                }

                And("new alias and seed are persisted") {
                    verify(atLeast = 2) { storage.write(AliasId, any<String>()) }
                    verify(atLeast = 2) { storage.write(Seed, any<Int>()) }
                }
            }
        }

    // endregion

    // region identify

    @Test
    fun `identify with new userId sets appUserId`() =
        runTest {
            Given("a fresh manager with no logged in user") {
                val testScope = IOScope(this@runTest.coroutineContext)
                val configState = MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState

                val manager = createManagerWithScope(testScope)

                When("identify is called with a new userId") {
                    manager.identify("new-user-456")
                    // Internal queue dispatches asynchronously
                    Thread.sleep(200)
                }

                Then("appUserId is set") {
                    assertEquals("new-user-456", manager.appUserId)
                }

                And("the userId is persisted") {
                    verify { storage.write(AppUserId, "new-user-456") }
                }

                And("completeReset is not called for first identification") {
                    assertFalse(resetCalled)
                }
            }
        }

    @Test
    fun `identify with same userId is a no-op`() =
        runTest {
            Given("a manager with an existing userId") {
                val testScope = IOScope(this@runTest.coroutineContext)
                val configState = MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState

                val manager = createManagerWithScope(testScope)

                // First identify
                manager.identify("user-123")
                Thread.sleep(200)
                advanceUntilIdle()

                When("identify is called again with the same userId") {
                    manager.identify("user-123")
                    Thread.sleep(200)
                }

                Then("completeReset is not called") {
                    assertFalse(resetCalled)
                }
            }
        }

    @Test
    fun `identify with empty string is a no-op`() =
        runTest {
            Given("a fresh manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with an empty string") {
                    manager.identify("")
                    Thread.sleep(200)
                }

                Then("appUserId remains null") {
                    assertNull(manager.appUserId)
                }

                And("completeReset is not called") {
                    assertFalse(resetCalled)
                }
            }
        }

    @Test
    fun `identify with different userId triggers reset`() =
        runTest {
            Given("a manager already identified with user-A") {
                val testScope = IOScope(this@runTest.coroutineContext)
                val configState = MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { configManager.configState } returns configState
                every { storage.read(AppUserId) } returns "user-A"

                val manager = createManagerWithScope(testScope, existingAppUserId = "user-A")

                When("identify is called with a different userId") {
                    manager.identify("user-B")
                    Thread.sleep(200)
                }

                Then("completeReset is called") {
                    assertTrue(resetCalled)
                }

                And("appUserId is updated to the new user") {
                    assertEquals("user-B", manager.appUserId)
                }
            }
        }

    // endregion

    // region configure

    @Test
    fun `configure does not call getAssignments on first app open when not logged in`() =
        runTest {
            Given("a first app open with no static config called") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns null

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                        neverCalledStaticConfig = true,
                    )

                When("configure is called") {
                    manager.configure()
                    advanceUntilIdle()
                }

                Then("getAssignments is not called") {
                    coVerify(exactly = 0) { configManager.getAssignments() }
                }
            }
        }

    // endregion

    // region mergeUserAttributes

    @Test
    fun `mergeUserAttributes persists merged attributes`() =
        runTest {
            Given("a manager with no existing attributes") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("mergeUserAttributes is called with new attributes") {
                    manager.mergeUserAttributes(mapOf("name" to "Test User"))
                    Thread.sleep(200)
                }

                Then("merged attributes are written to storage") {
                    verify(timeout = 500) {
                        storage.write(UserAttributes, match { it.containsKey("name") })
                    }
                }
            }
        }

    @Test
    fun `mergeUserAttributes tracks event when shouldTrackMerge is true`() =
        runTest {
            Given("a manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("mergeUserAttributes is called with tracking enabled") {
                    manager.mergeUserAttributes(
                        mapOf("key" to "value"),
                        shouldTrackMerge = true,
                    )
                    Thread.sleep(200)
                    advanceUntilIdle()
                }

                Then("an Attributes event is tracked") {
                    assertTrue(trackedEvents.isNotEmpty())
                }
            }
        }

    @Test
    fun `mergeUserAttributes does not track when shouldTrackMerge is false`() =
        runTest {
            Given("a manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("mergeUserAttributes is called with tracking disabled") {
                    manager.mergeUserAttributes(
                        mapOf("key" to "value"),
                        shouldTrackMerge = false,
                    )
                    Thread.sleep(200)
                }

                Then("no event is tracked") {
                    assertTrue(trackedEvents.isEmpty())
                }
            }
        }

    @Test
    fun `mergeAndNotify calls notifyUserChange`() =
        runTest {
            Given("a manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("mergeAndNotify is called") {
                    manager.mergeAndNotify(mapOf("key" to "value"))
                    Thread.sleep(200)
                }

                Then("notifyUserChange callback is invoked") {
                    assertTrue(notifiedChanges.isNotEmpty())
                }
            }
        }

    // endregion
}
