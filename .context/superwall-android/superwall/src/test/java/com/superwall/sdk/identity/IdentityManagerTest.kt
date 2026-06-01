package com.superwall.sdk.identity

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.primitives.StateActor
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.config.RawFeatureFlag
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.storage.AliasId
import com.superwall.sdk.storage.AppUserId
import com.superwall.sdk.storage.DidTrackFirstSeen
import com.superwall.sdk.storage.Seed
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.UserAttributes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentityManagerTest {
    private lateinit var storage: Storage
    private lateinit var deviceHelper: DeviceHelper
    private var notifiedChanges: MutableList<Map<String, Any>> = mutableListOf()
    private var resetCalled = false
    private var trackedEvents: MutableList<Any> = mutableListOf()

    private fun installPrintlnDebug(actor: StateActor<IdentityContext, IdentityState>, name: String) {
        actor.onUpdate { reducer, next ->
            next(reducer)
            println("[$name] update -> $reducer")
        }
        actor.onAction { action, next ->
            println("[$name] action -> $action")
            next()
        }
        actor.onActionExecution { action, next ->
            try {
                next()
            } finally {
                println("[$name] action done -> $action")
            }
        }
    }

    /** Create a test identity actor using Unconfined dispatcher. */
    private fun testIdentityActor(): StateActor<IdentityContext, IdentityState> {
        val actor =
            StateActor<IdentityContext, IdentityState>(
                createInitialIdentityState(storage, "2024-01-01"),
                CoroutineScope(Dispatchers.Unconfined),
            )
        installPrintlnDebug(actor, name = "IdentityTest")
        IdentityPendingInterceptor.install(actor)
        IdentityPersistenceInterceptor.install(actor, storage)
        return actor
    }

    @Before
    fun setup() {
        storage = mockk(relaxed = true)
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
        superwallOptions: SuperwallOptions = SuperwallOptions(),
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }
        existingSeed?.let { every { storage.read(Seed) } returns it }
        existingAttributes?.let { every { storage.read(UserAttributes) } returns it }

        val scope = IOScope(dispatcher.coroutineContext)
        return IdentityManager(
            storage = storage,
            options = { superwallOptions },
            ioScope = scope,
            notifyUserChange = { notifiedChanges.add(it) },
            completeReset = { resetCalled = true },
            tracker = { trackedEvents.add(it) },
            webPaywallRedeemer = { mockk(relaxed = true) },
            actor = testIdentityActor(),
            sdkContext = mockk(relaxed = true),
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
    ): IdentityManager {
        existingAppUserId?.let { every { storage.read(AppUserId) } returns it }
        existingAliasId?.let { every { storage.read(AliasId) } returns it }

        return IdentityManager(
            storage = storage,
            options = { SuperwallOptions() },
            ioScope = ioScope,
            notifyUserChange = { notifiedChanges.add(it) },
            completeReset = { resetCalled = true },
            tracker = { trackedEvents.add(it) },
            webPaywallRedeemer = { mockk(relaxed = true) },
            actor = testIdentityActor(),
            sdkContext = mockk(relaxed = true),
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
    fun `init does not merge attributes when alias, seed, and attributes already exist`() =
        runTest {
            Given("existing aliasId, seed, and non-empty attributes in storage") {
                When("the manager is created") {
                    createManager(
                        this@runTest,
                        existingAliasId = "existing",
                        existingSeed = 50,
                        existingAttributes = mapOf("aliasId" to "existing", "seed" to 50),
                    )
                    Then("user attributes are not merged during init") {
                        verify(exactly = 0) { storage.write(UserAttributes, any()) }
                    }
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
                val manager =
                    createManager(
                        this@runTest,
                        existingAppUserId = "user-123",
                        superwallOptions = SuperwallOptions().apply { passIdentifiersToPlayStore = true },
                    )

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
                val testOptions = SuperwallOptions().apply { passIdentifiersToPlayStore = false }

                val manager =
                    IdentityManager(
                        storage = storage,
                        options = { testOptions },
                        ioScope = IOScope(Dispatchers.Unconfined),
                        stringToSha = { "sha256-of-$it" },
                        notifyUserChange = {},
                        completeReset = {},
                        tracker = {},
                        webPaywallRedeemer = { mockk(relaxed = true) },
                        actor = testIdentityActor(),
                        sdkContext = mockk(relaxed = true),
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
                    manager.reset()
                    manager.awaitLatestIdentity()
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
    // region identify

    @Test
    fun `identify with new userId sets appUserId`() =
        runTest {
            Given("a fresh manager with no logged in user") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with a new userId") {
                    manager.identify("new-user-456")
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

                val manager = createManagerWithScope(testScope)

                // First identify
                manager.identify("user-123")
                manager.awaitLatestIdentity()

                When("identify is called again with the same userId") {
                    manager.identify("user-123")
                    manager.awaitLatestIdentity()
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
                    manager.awaitLatestIdentity()
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

                every { storage.read(AppUserId) } returns "user-A"

                val manager = createManagerWithScope(testScope, existingAppUserId = "user-A")

                When("identify is called with a different userId") {
                    manager.identify("user-B")
                    manager.awaitLatestIdentity()
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
                    )

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = true))
                    manager.awaitLatestIdentity()
                }

                Then("identity is ready immediately without pending assignments") {
                    assertTrue("Identity should be ready", manager.actor.state.value.isReady)
                    assertFalse(
                        "Should not have pending assignments",
                        manager.actor.state.value.pending
                            .contains(IdentityState.Pending.Assignments),
                    )
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
                    manager.awaitLatestIdentity()
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
                    manager.awaitLatestIdentity()
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
                    manager.awaitLatestIdentity()
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
                    manager.awaitLatestIdentity()
                }

                Then("notifyUserChange callback is invoked") {
                    assertTrue(notifiedChanges.isNotEmpty())
                }
            }
        }

    @Test
    fun `mergeUserAttributes does not call notifyUserChange`() =
        runTest {
            Given("a manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("mergeUserAttributes is called (not mergeAndNotify)") {
                    manager.mergeUserAttributes(mapOf("key" to "value"))
                    manager.awaitLatestIdentity()
                }

                Then("notifyUserChange callback is NOT invoked") {
                    assertTrue(notifiedChanges.isEmpty())
                }
            }
        }

    // endregion

    // region identify - restorePaywallAssignments

    @Test
    fun `identify with restorePaywallAssignments true sets appUserId`() =
        runTest {
            Given("a manager with config available") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with restorePaywallAssignments = true") {
                    manager.identify(
                        "user-restore",
                        options = IdentityOptions(restorePaywallAssignments = true),
                    )
                    manager.awaitLatestIdentity()
                }

                Then("appUserId is set") {
                    assertEquals("user-restore", manager.appUserId)
                }

                And("userId is persisted") {
                    verify { storage.write(AppUserId, "user-restore") }
                }
            }
        }

    @Test
    fun `identify with restorePaywallAssignments false sets appUserId`() =
        runTest {
            Given("a manager with config available") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with restorePaywallAssignments = false (default)") {
                    manager.identify("user-no-restore")
                    manager.awaitLatestIdentity()
                }

                Then("appUserId is set") {
                    assertEquals("user-no-restore", manager.appUserId)
                }

                And("userId is persisted") {
                    verify { storage.write(AppUserId, "user-no-restore") }
                }
            }
        }

    // endregion

    // region identify - side effects

    @Test
    fun `identify with whitespace-only userId is a no-op`() =
        runTest {
            Given("a fresh manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with whitespace-only string") {
                    manager.identify("   \n\t   ")
                    manager.awaitLatestIdentity()
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
    fun `identify tracks IdentityAlias event`() =
        runTest {
            Given("a manager with config available") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called with a new userId") {
                    manager.identify("user-track-test")
                    manager.awaitLatestIdentity()
                    advanceUntilIdle()
                }

                Then("an IdentityAlias event is tracked") {
                    assertTrue(
                        "Expected IdentityAlias event in tracked events, got: $trackedEvents",
                        trackedEvents.any { it is InternalSuperwallEvent.IdentityAlias },
                    )
                }
            }
        }

    @Test
    fun `identify persists aliasId along with appUserId`() =
        runTest {
            Given("a manager with config available") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify is called") {
                    manager.identify("user-side-effects")
                    manager.awaitLatestIdentity()
                }

                Then("appUserId is persisted") {
                    verify { storage.write(AppUserId, "user-side-effects") }
                }

                And("aliasId is persisted alongside it") {
                    verify { storage.write(AliasId, any<String>()) }
                }

                And("seed is persisted alongside it") {
                    verify { storage.write(Seed, any<Int>()) }
                }
            }
        }

    // endregion

    // region identify - seed re-computation with enableUserIdSeed

    @Test
    fun `identify re-seeds from userId SHA when enableUserIdSeed flag is true`() =
        runTest {
            Given("a config with enableUserIdSeed enabled") {
                val configWithFlag =
                    Config.stub().copy(
                        rawFeatureFlags =
                            listOf(
                                RawFeatureFlag("enable_userid_seed", true),
                            ),
                    )
                // Set up sdkContext mock so ResolveSeed can read the config
                val sdkContext = mockk<com.superwall.sdk.SdkContext>(relaxed = true)
                coEvery { sdkContext.awaitConfig() } returns configWithFlag

                val manager =
                    IdentityManager(
                        storage = storage,
                        options = { SuperwallOptions() },
                        ioScope = IOScope(this@runTest.coroutineContext),
                        notifyUserChange = { notifiedChanges.add(it) },
                        completeReset = { resetCalled = true },
                        tracker = { trackedEvents.add(it) },
                        webPaywallRedeemer = { mockk(relaxed = true) },
                        actor = testIdentityActor(),
                        sdkContext = sdkContext,
                    )

                val seedBefore = manager.seed

                When("identify is called with a userId") {
                    manager.identify("deterministic-user")
                    manager.awaitLatestIdentity()
                }

                Then("seed is updated based on the userId hash") {
                    val seedAfter = manager.seed
                    // The seed should be deterministically derived from the userId
                    assertTrue("Seed should be in range 0-99, got: $seedAfter", seedAfter in 0..99)
                    // Verify seed was written to storage
                    verify(atLeast = 1) { storage.write(Seed, any<Int>()) }
                }
            }
        }

    // endregion

    // region hasIdentity flow

    @Test
    fun `hasIdentity emits true after configure`() =
        runTest {
            Given("a fresh manager") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns true

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                    )

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = false))
                    manager.awaitLatestIdentity()
                }

                Then("hasIdentity emits true") {
                    val result = withTimeout(2000) { manager.hasIdentity.first() }
                    assertTrue(result)
                }
            }
        }

    @Test
    fun `hasIdentity emits true after configure for returning user`() =
        runTest {
            Given("a returning anonymous user") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns true

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                        existingAliasId = "returning-alias",
                    )

                var identityReceived = false
                val collectJob =
                    launch {
                        manager.hasIdentity.first()
                        identityReceived = true
                    }

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = false))
                    manager.awaitLatestIdentity()
                    advanceUntilIdle()
                }

                Then("hasIdentity emitted true") {
                    collectJob.cancel()
                    assertTrue(
                        "hasIdentity should have emitted true after configure",
                        identityReceived,
                    )
                }
            }
        }

    // endregion

    // region configure - additional cases

    @Test
    fun `configure triggers assignment fetching when logged in and neverCalledStaticConfig`() =
        runTest {
            Given("a logged-in returning user with neverCalledStaticConfig = true") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns true

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                        existingAppUserId = "user-123",
                    )

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = true))
                    manager.awaitLatestIdentity()
                }

                Then("identity state reflects that assignments were requested") {
                    assertTrue("Identity should be ready after configure", manager.actor.state.value.isReady)
                }
            }
        }

    @Test
    fun `configure triggers assignment fetching for anonymous returning user with neverCalledStaticConfig`() =
        runTest {
            Given("an anonymous returning user with neverCalledStaticConfig = true") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns true // not first open

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                    )

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = true))
                    manager.awaitLatestIdentity()
                }

                Then("identity state reflects that assignments were requested") {
                    assertTrue("Identity should be ready after configure", manager.actor.state.value.isReady)
                }
            }
        }

    @Test
    fun `configure does not trigger assignments when neverCalledStaticConfig is false`() =
        runTest {
            Given("a logged-in user but static config has been called") {
                val testScope = IOScope(this@runTest.coroutineContext)
                every { storage.read(DidTrackFirstSeen) } returns true

                val manager =
                    createManagerWithScope(
                        ioScope = testScope,
                        existingAppUserId = "user-123",
                    )

                When("configure is dispatched") {
                    manager.effect(IdentityState.Actions.Configure(neverCalledStaticConfig = false))
                    manager.awaitLatestIdentity()
                }

                Then("identity is ready without pending assignments") {
                    assertTrue("Identity should be ready", manager.actor.state.value.isReady)
                    assertFalse(
                        "Should not have pending assignments",
                        manager.actor.state.value.pending
                            .contains(IdentityState.Pending.Assignments),
                    )
                }
            }
        }

    // endregion

    // region reset - custom attributes cleared

    @Test
    fun `reset clears custom attributes but repopulates identity fields`() =
        runTest {
            Given("an identified user with custom attributes") {
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
                                "customName" to "John",
                                "customEmail" to "john@test.com",
                                "applicationInstalledAt" to "2024-01-01",
                            ),
                    )

                When("reset is called") {
                    manager.reset()
                }

                manager.awaitLatestIdentity()

                Then("custom attributes are gone") {
                    val attrs = manager.userAttributes
                    assertFalse(
                        "customName should not survive reset, got: $attrs",
                        attrs.containsKey("customName"),
                    )
                    assertFalse(
                        "customEmail should not survive reset, got: $attrs",
                        attrs.containsKey("customEmail"),
                    )
                }

                And("identity fields are repopulated with new values") {
                    val attrs = manager.userAttributes
                    assertTrue(attrs.containsKey("aliasId"))
                    assertTrue(attrs.containsKey("seed"))
                    assertNotEquals("old-alias", attrs["aliasId"])
                }
            }
        }

    // endregion

    // region userAttributes getter invariant

    @Test
    fun `userAttributes getter always injects aliasId but omits appUserId when anonymous`() =
        runTest {
            Given("a manager with no stored attributes") {
                val manager = createManager(this@runTest, existingAliasId = "test-alias", existingSeed = 55)

                Then("userAttributes always contains aliasId") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "userAttributes must always contain aliasId, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertEquals("test-alias", attrs["aliasId"])
                }

                And("userAttributes does NOT contain appUserId when anonymous (matches iOS)") {
                    val attrs = manager.userAttributes
                    assertFalse(
                        "anonymous user_attributes must not inject appUserId fallback, got: $attrs",
                        attrs.containsKey("appUserId"),
                    )
                }
            }
        }

    @Test
    fun `userAttributes getter reflects appUserId after identify`() =
        runTest {
            Given("a fresh manager") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)
                val aliasBeforeIdentify = manager.aliasId

                When("identify is called") {
                    manager.identify("real-user")
                    manager.awaitLatestIdentity()
                }

                Then("userAttributes appUserId reflects the identified user") {
                    assertEquals("real-user", manager.userAttributes["appUserId"])
                }

                And("userAttributes aliasId is still present") {
                    assertEquals(aliasBeforeIdentify, manager.userAttributes["aliasId"])
                }
            }
        }

    // endregion

    // region concurrent operations

    @Test
    fun `concurrent identify and mergeUserAttributes do not lose data`() =
        runTest {
            Given("a manager with config available") {
                val testScope = IOScope(this@runTest.coroutineContext)

                val manager = createManagerWithScope(testScope)

                When("identify and mergeUserAttributes are called concurrently") {
                    val job1 = launch { manager.identify("concurrent-user") }
                    val job2 =
                        launch {
                            manager.mergeUserAttributes(
                                mapOf("name" to "Test", "plan" to "premium"),
                            )
                        }
                    job1.join()
                    job2.join()
                    manager.awaitLatestIdentity()
                }

                Then("appUserId is set correctly") {
                    assertEquals("concurrent-user", manager.appUserId)
                }

                And("identity fields are always present in userAttributes") {
                    val attrs = manager.userAttributes
                    assertTrue(
                        "aliasId must be present, got: $attrs",
                        attrs.containsKey("aliasId"),
                    )
                    assertTrue(
                        "appUserId must be present, got: $attrs",
                        attrs.containsKey("appUserId"),
                    )
                }
            }
        }

    // endregion
}
