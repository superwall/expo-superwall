package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.analytics.Tier
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.primitives.SequentialActor
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.config.RawFeatureFlag
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerPreloadBehavior
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.storage.LatestConfig
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.testmode.TestMode
import com.superwall.sdk.store.testmode.TestModeBehavior
import com.superwall.sdk.web.WebPaywallRedeemer
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.storage.DisableVerboseEvents
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ConfigManagerTest {
    private fun config(
        buildId: String = "stub",
        triggers: Set<Trigger> = emptySet(),
        enableRefresh: Boolean = false,
    ): Config =
        Config.stub().copy(
            buildId = buildId,
            triggers = triggers,
            rawFeatureFlags =
                if (enableRefresh) {
                    listOf(RawFeatureFlag("enable_config_refresh_v2", true))
                } else {
                    emptyList()
                },
        )

    private data class Setup(
        val manager: ConfigManagerForTest,
        val network: SuperwallAPI,
        val storage: Storage,
        val preload: PaywallPreload,
        val storeManager: StoreManager,
        val webRedeemer: WebPaywallRedeemer,
        val deviceHelper: DeviceHelper,
        val testMode: TestMode?,
        val tracked: CopyOnWriteArrayList<TrackableSuperwallEvent>,
        val statuses: MutableList<SubscriptionStatus>,
        val activateCalls: AtomicInteger,
    )

    @Suppress("LongParameterList")
    private fun setup(
        scope: CoroutineScope,
        cachedConfig: Config? = null,
        cachedEnrichment: Enrichment? = null,
        networkConfig: Either<Config, NetworkError> = Either.Success(Config.stub()),
        networkConfigAnswer: (suspend (suspend () -> Unit) -> Either<Config, NetworkError>)? = null,
        deviceTier: Tier = Tier.MID,
        shouldPreload: Boolean = false,
        preloadDeviceOverrides: Map<Tier, Boolean> = emptyMap(),
        testModeBehavior: TestModeBehavior = TestModeBehavior.AUTOMATIC,
        injectedTestMode: TestMode? = null,
        assignments: Assignments = mockk(relaxed = true),
        identityManager: IdentityManager? = null,
        storeManagerOverride: StoreManager? = null,
        entitlementsOverride: Entitlements? = null,
    ): Setup {
        val context = mockk<Context>(relaxed = true)
        val storage =
            mockk<Storage>(relaxed = true) {
                every { read(LatestConfig) } returns cachedConfig
                every { read(LatestEnrichment) } returns cachedEnrichment
                every { write(any(), any<Any>()) } just Runs
            }
        val network =
            mockk<SuperwallAPI> {
                if (networkConfigAnswer != null) {
                    coEvery { getConfig(any()) } coAnswers {
                        networkConfigAnswer.invoke(firstArg())
                    }
                } else {
                    coEvery { getConfig(any()) } returns networkConfig
                }
                coEvery { getEnrichment(any(), any(), any()) } returns
                    Either.Success(Enrichment.stub())
            }
        val deviceHelper =
            mockk<DeviceHelper>(relaxed = true) {
                every { appVersion } returns "1.0"
                every { locale } returns "en-US"
                every { this@mockk.deviceTier } returns deviceTier
                every { bundleId } returns "com.test"
                every { setEnrichment(any()) } just Runs
                coEvery { getTemplateDevice() } returns emptyMap()
                coEvery { getEnrichment(any(), any()) } returns Either.Success(Enrichment.stub())
                coEvery { preloadFingerprint() } returns "stub-fingerprint"
            }
        val storeManager =
            storeManagerOverride
                ?: mockk<StoreManager>(relaxed = true) {
                    coEvery { products(any()) } returns emptySet()
                    coEvery { loadPurchasedProducts(any()) } just Runs
                }
        val preload =
            mockk<PaywallPreload> {
                coEvery { preloadAllPaywalls(any(), any(), any()) } just Runs
                coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                every { lastFingerprint } returns java.util.concurrent.atomic.AtomicReference(null)
            }
        val paywallManager = mockk<PaywallManager>(relaxed = true)
        val webRedeemer = mockk<WebPaywallRedeemer>(relaxed = true)
        val factory =
            mockk<ConfigManager.Factory>(relaxed = true) {
                coEvery { makeSessionDeviceAttributes() } returns HashMap()
                every { makeHasExternalPurchaseController() } returns false
            }
        val entitlements =
            entitlementsOverride ?: mockk<Entitlements>(relaxed = true).also {
                every { it.status } returns
                    kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                every { it.entitlementsByProductId } returns emptyMap()
            }

        val tracked = CopyOnWriteArrayList<TrackableSuperwallEvent>()
        val statuses = mutableListOf<SubscriptionStatus>()
        val activateCalls = AtomicInteger(0)

        val options =
            SuperwallOptions().apply {
                paywalls.shouldPreload = shouldPreload
                paywalls.preloadDeviceOverrides = preloadDeviceOverrides
                this.testModeBehavior = testModeBehavior
            }

        val manager =
            ConfigManagerForTest(
                context = context,
                storage = storage,
                network = network,
                deviceHelper = deviceHelper,
                paywallManager = paywallManager,
                storeManager = storeManager,
                preload = preload,
                webRedeemer = webRedeemer,
                factory = factory,
                entitlements = entitlements,
                assignments = assignments,
                options = options,
                ioScope = scope,
                testMode = injectedTestMode,
                tracker = { tracked.add(it) },
                setSubscriptionStatus = { statuses.add(it) },
                activateTestMode = { _, justActivated ->
                    if (justActivated) activateCalls.incrementAndGet()
                },
                identityManager = identityManager?.let { im -> { im } },
            )
        return Setup(
            manager,
            network,
            storage,
            preload,
            storeManager,
            webRedeemer,
            deviceHelper,
            injectedTestMode,
            tracked,
            statuses,
            activateCalls,
        )
    }

    @Test
    fun `autoRetryCount resets after a successful apply`() =
        runTest(timeout = 30.seconds) {
            val calls = AtomicInteger(0)
            val s =
                setup(
                    backgroundScope,
                    networkConfigAnswer = {
                        when (calls.incrementAndGet()) {
                            1, 2 -> Either.Failure(NetworkError.Unknown())
                            3 -> Either.Success(Config.stub())
                            else -> Either.Failure(NetworkError.Unknown())
                        }
                    },
                )

            s.manager.fetchConfiguration()
            advanceUntilIdle()
            assertEquals("Cold-start = initial + 1 retry", 2, calls.get())
            assertTrue(s.manager.configState.value is ConfigState.Failed)

            s.manager.fetchConfiguration()
            advanceUntilIdle()
            assertEquals(3, calls.get())
            assertTrue(s.manager.configState.value is ConfigState.Retrieved)

            s.manager.fetchConfiguration()
            advanceUntilIdle()
            assertEquals("Counter must reset on Retrieved — fresh budget", 5, calls.get())
        }

    @Test
    fun `reevaluateTestMode deactivates when user no longer qualifies`() =
        runTest(timeout = 30.seconds) {
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storageForTm, isTestEnvironment = false))
            testMode.evaluateTestMode(
                Config.stub(),
                "com.test",
                null,
                null,
                testModeBehavior = TestModeBehavior.ALWAYS,
            )
            assertTrue(testMode.isTestMode)

            val s = setup(backgroundScope, injectedTestMode = testMode)
            s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "no-match")

            assertFalse(testMode.isTestMode)
            verify(atLeast = 1) { testMode.clearTestModeState() }
            assertTrue(
                "Expected SubscriptionStatus.Inactive on deactivation",
                s.statuses.any { it is SubscriptionStatus.Inactive },
            )
        }

    @Test
    fun `reevaluateTestMode activates when user now qualifies`() =
        runTest(timeout = 30.seconds) {
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = TestMode(storage = storageForTm, isTestEnvironment = false)
            assertFalse(testMode.isTestMode)

            val s =
                setup(
                    backgroundScope,
                    testModeBehavior = TestModeBehavior.ALWAYS,
                    injectedTestMode = testMode,
                )
            s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "anyone")
            advanceUntilIdle()

            assertTrue(testMode.isTestMode)
            assertEquals("activateTestMode lambda must fire once", 1, s.activateCalls.get())
        }

    @Test
    fun `reevaluateTestMode is noop when state unchanged`() =
        runTest(timeout = 30.seconds) {
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storageForTm, isTestEnvironment = false))
            assertFalse(testMode.isTestMode)

            val s =
                setup(
                    backgroundScope,
                    testModeBehavior = TestModeBehavior.NEVER,
                    injectedTestMode = testMode,
                )
            s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "anyone")
            advanceUntilIdle()

            assertFalse(testMode.isTestMode)
            verify(exactly = 0) { testMode.clearTestModeState() }
            assertTrue("No subscription status published on no-op", s.statuses.isEmpty())
            assertEquals("activateTestMode must not fire on no-op", 0, s.activateCalls.get())
        }

    // Both reevaluateTestMode and ApplyConfig mutate TestMode.state. They
    // must be serialized through the config actor — never overlap.
    @Test
    fun `reevaluateTestMode and ApplyConfig do not overlap on TestMode state`() =
        runTest(timeout = 30.seconds) {
            val storage = mockk<Storage>(relaxed = true)
            val inFlight = AtomicInteger(0)
            val maxOverlap = AtomicInteger(0)
            val testMode = spyk(TestMode(storage = storage, isTestEnvironment = false))
            // Track in-flight evaluateTestMode calls. If serialization holds,
            // maxOverlap stays at 1.
            every {
                testMode.evaluateTestMode(any(), any(), any(), any(), any())
            } answers {
                val n = inFlight.incrementAndGet()
                maxOverlap.updateAndGet { kotlin.math.max(it, n) }
                Thread.sleep(20) // simulate non-trivial work
                inFlight.decrementAndGet()
                callOriginal()
            }

            val s = setup(
                backgroundScope,
                injectedTestMode = testMode,
                testModeBehavior = TestModeBehavior.ALWAYS,
            )

            // Race: drive ApplyConfig (via fetchConfiguration) and
            // reevaluateTestMode in parallel.
            val a = launch { s.manager.fetchConfiguration() }
            val b = launch { s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "u") }
            val c = launch { s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "v") }
            a.join(); b.join(); c.join()
            advanceUntilIdle()

            assertEquals(
                "evaluateTestMode must never overlap with itself when serialized through the actor — saw ${maxOverlap.get()} concurrent",
                1,
                maxOverlap.get(),
            )
        }

    @Test
    fun `fetchConfig in test mode skips web entitlements and product preload`() =
        runTest(timeout = 30.seconds) {
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = TestMode(storage = storageForTm, isTestEnvironment = false)
            val s =
                setup(
                    backgroundScope,
                    shouldPreload = true,
                    testModeBehavior = TestModeBehavior.ALWAYS,
                    injectedTestMode = testMode,
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertTrue(testMode.isTestMode)
            coVerify(exactly = 0) { s.storeManager.products(any()) }
            coVerify(exactly = 0) { s.webRedeemer.redeem(any()) }
        }

    @Test
    fun `refreshConfig runs full applyConfig fanout`() =
        runTest(timeout = 30.seconds) {
            val initial = config(buildId = "initial", triggers = setOf(Trigger.stub().copy(eventName = "trigger_a")), enableRefresh = true)
            val refreshed = config(buildId = "refreshed", triggers = setOf(Trigger.stub().copy(eventName = "trigger_b")), enableRefresh = true)
            val getCalls = AtomicInteger(0)
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storageForTm, isTestEnvironment = false))

            val s =
                setup(
                    backgroundScope,
                    networkConfigAnswer = {
                        Either.Success(if (getCalls.incrementAndGet() == 1) initial else refreshed)
                    },
                    injectedTestMode = testMode,
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { (it as? ConfigState.Retrieved)?.config?.buildId == "initial" }
            advanceUntilIdle()
            assertTrue(s.manager.triggersByEventName.containsKey("trigger_a"))

            s.manager.refreshConfiguration(force = true)
            s.manager.configState.first { (it as? ConfigState.Retrieved)?.config?.buildId == "refreshed" }
            advanceUntilIdle()

            assertTrue(s.manager.triggersByEventName.containsKey("trigger_b"))
            assertFalse(s.manager.triggersByEventName.containsKey("trigger_a"))
            verify(atLeast = 2) { testMode.evaluateTestMode(any(), any(), any(), any(), any()) }
            verify { s.storage.write(LatestConfig, refreshed) }
        }

    @Test
    fun `preloadAllPaywalls bypasses shouldPreload gate`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope, shouldPreload = false)
            s.manager.applyRetrievedConfigForTesting(Config.stub())

            s.manager.preloadAllPaywalls()
            advanceUntilIdle()

            coVerify(exactly = 1) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `preloadPaywallsByNames bypasses shouldPreload gate`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope, shouldPreload = false)
            s.manager.applyRetrievedConfigForTesting(Config.stub())

            s.manager.preloadPaywallsByNames(setOf("evt"))
            advanceUntilIdle()

            coVerify(exactly = 1) { s.preload.preloadPaywallsByNames(any(), eq(setOf("evt"))) }
        }

    @Test
    fun `tracking emits ConfigRefresh isCached false and DeviceAttributes on fresh fetch`() =
        runTest(timeout = 30.seconds) {
            val s =
                setup(
                    backgroundScope,
                    networkConfig = Either.Success(Config.stub().copy(buildId = "fresh-id")),
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            val refresh = s.tracked.filterIsInstance<InternalSuperwallEvent.ConfigRefresh>()
            assertTrue("Expected ConfigRefresh event, got ${s.tracked}", refresh.isNotEmpty())
            assertFalse("Fresh fetch must mark isCached=false", refresh.last().isCached)
            assertEquals("fresh-id", refresh.last().buildId)

            val deviceAttrs = s.tracked.filterIsInstance<InternalSuperwallEvent.DeviceAttributes>()
            assertTrue("Expected at least one DeviceAttributes event", deviceAttrs.isNotEmpty())
        }

    @Test
    fun `tracking emits ConfigRefresh isCached true on cached path`() =
        runTest(timeout = 30.seconds) {
            val cached = config(buildId = "cached-id", enableRefresh = true)
            val s =
                setup(
                    backgroundScope,
                    cachedConfig = cached,
                    cachedEnrichment = Enrichment.stub(),
                    // network failure on cached-refresh path → FetchConfig's
                    // `.into { if (Failure) Success(oldConfig) }` falls back to cache.
                    networkConfig = Either.Failure(NetworkError.Unknown()),
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { (it as? ConfigState.Retrieved)?.config?.buildId == "cached-id" }
            advanceUntilIdle()

            val refresh = s.tracked.filterIsInstance<InternalSuperwallEvent.ConfigRefresh>()
            assertTrue(
                "Cached path must publish ConfigRefresh with isCached=true",
                refresh.any { it.isCached && it.buildId == "cached-id" },
            )
        }

    @Test
    fun `tracking emits ConfigFail on failure without cache`() =
        runTest(timeout = 30.seconds) {
            val s =
                setup(
                    backgroundScope,
                    networkConfig = Either.Failure(NetworkError.Unknown()),
                )

            s.manager.fetchConfiguration()
            advanceUntilIdle()

            val fails = s.tracked.filterIsInstance<InternalSuperwallEvent.ConfigFail>()
            assertTrue("Expected at least one ConfigFail, got ${s.tracked}", fails.isNotEmpty())
        }

    @Test
    fun `fetchConfig skips when already in Retrying state`() =
        runTest(timeout = 30.seconds) {
            val calls = AtomicInteger(0)
            val s =
                setup(
                    backgroundScope,
                    networkConfigAnswer = { retryCb ->
                        calls.incrementAndGet()
                        retryCb() // flip to Retrying
                        delay(800)
                        Either.Success(Config.stub())
                    },
                )

            val first = launch { s.manager.fetchConfiguration() }
            s.manager.configState.first { it is ConfigState.Retrying }
            s.manager.fetchConfiguration() // must early-return
            first.join()

            assertEquals("Expected exactly one network.getConfig", 1, calls.get())
        }

    @Test
    fun `enrichment success writes LatestEnrichment on cached boot`() =
        runTest(timeout = 30.seconds) {
            val freshEnrichment = Enrichment.stub()
            val cached = config(enableRefresh = true)
            val s =
                setup(
                    backgroundScope,
                    cachedConfig = cached,
                    cachedEnrichment = null,
                )
            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            verify(atLeast = 1) { s.storage.write(LatestEnrichment, freshEnrichment) }
        }

    @Test
    fun `applyConfig populates entitlements from products and productsV3`() =
        runTest(timeout = 30.seconds) {
            val productMap = mapOf("p1" to setOf<com.superwall.sdk.models.entitlements.Entitlement>())
            val crossplatformMap = mapOf("p2" to setOf<com.superwall.sdk.models.entitlements.Entitlement>())
            io.mockk.mockkObject(ConfigLogic)
            try {
                every { ConfigLogic.extractEntitlementsByProductId(any()) } returns productMap
                every { ConfigLogic.extractEntitlementsByProductIdFromCrossplatform(any()) } returns crossplatformMap
                every { ConfigLogic.getTriggersByEventName(any()) } returns emptyMap()

                val entitlements = mockk<Entitlements>(relaxed = true).also {
                    every { it.status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { it.entitlementsByProductId } returns emptyMap()
                }
                val configWithV3 =
                    Config.stub().copy(productsV3 = listOf(mockk(relaxed = true)))
                val s =
                    setup(
                        backgroundScope,
                        networkConfig = Either.Success(configWithV3),
                        entitlementsOverride = entitlements,
                    )

                s.manager.fetchConfiguration()
                s.manager.configState.first { it is ConfigState.Retrieved }
                advanceUntilIdle()

                verify(exactly = 1) { entitlements.addEntitlementsByProductId(productMap) }
                verify(exactly = 1) { entitlements.addEntitlementsByProductId(crossplatformMap) }
            } finally {
                io.mockk.unmockkObject(ConfigLogic)
            }
        }

    @Test
    fun `applyConfig skips crossplatform entitlements when productsV3 is null`() =
        runTest(timeout = 30.seconds) {
            io.mockk.mockkObject(ConfigLogic)
            try {
                val productMap = mapOf("p1" to setOf<com.superwall.sdk.models.entitlements.Entitlement>())
                every { ConfigLogic.extractEntitlementsByProductId(any()) } returns productMap
                every { ConfigLogic.extractEntitlementsByProductIdFromCrossplatform(any()) } returns emptyMap()
                every { ConfigLogic.getTriggersByEventName(any()) } returns emptyMap()

                val entitlements = mockk<Entitlements>(relaxed = true).also {
                    every { it.status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { it.entitlementsByProductId } returns emptyMap()
                }
                val s =
                    setup(
                        backgroundScope,
                        networkConfig = Either.Success(Config.stub().copy(productsV3 = null)),
                        entitlementsOverride = entitlements,
                    )

                s.manager.fetchConfiguration()
                s.manager.configState.first { it is ConfigState.Retrieved }
                advanceUntilIdle()

                verify(exactly = 1) { entitlements.addEntitlementsByProductId(productMap) }
                verify(exactly = 0) {
                    ConfigLogic.extractEntitlementsByProductIdFromCrossplatform(any())
                }
            } finally {
                io.mockk.unmockkObject(ConfigLogic)
            }
        }

    @Test
    fun `applyConfig falls back to identityManager for appUserId and aliasId`() =
        runTest(timeout = 30.seconds) {
            val identity =
                mockk<IdentityManager>(relaxed = true) {
                    every { appUserId } returns "from-identity"
                    every { aliasId } returns "alias-from-identity"
                }
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storageForTm, isTestEnvironment = false))
            val s =
                setup(
                    backgroundScope,
                    injectedTestMode = testMode,
                    identityManager = identity,
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            verify(atLeast = 1) {
                testMode.evaluateTestMode(
                    config = any(),
                    bundleId = "com.test",
                    appUserId = "from-identity",
                    aliasId = "alias-from-identity",
                    testModeBehavior = any(),
                )
            }
        }

    @Test
    fun `reevaluateTestMode falls back to identityManager when ids omitted`() =
        runTest(timeout = 30.seconds) {
            val identity =
                mockk<IdentityManager>(relaxed = true) {
                    every { appUserId } returns "from-identity"
                    every { aliasId } returns "alias-from-identity"
                }
            val storageForTm = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storageForTm, isTestEnvironment = false))
            val s =
                setup(
                    backgroundScope,
                    testModeBehavior = TestModeBehavior.AUTOMATIC,
                    injectedTestMode = testMode,
                    identityManager = identity,
                )

            s.manager.reevaluateTestMode(config = Config.stub())
            advanceUntilIdle()

            verify(atLeast = 1) {
                testMode.evaluateTestMode(
                    config = any(),
                    bundleId = "com.test",
                    appUserId = "from-identity",
                    aliasId = "alias-from-identity",
                    testModeBehavior = any(),
                )
            }
        }

    @Test
    fun `fetchConfig completes when storeManager_products throws`() =
        runTest(timeout = 30.seconds) {
            val storeManager =
                mockk<StoreManager>(relaxed = true) {
                    coEvery { products(any()) } throws RuntimeException("billing exploded")
                    coEvery { loadPurchasedProducts(any()) } just Runs
                }
            val s =
                setup(
                    backgroundScope,
                    networkConfig = Either.Success(Config.stub().copy(buildId = "ok")),
                    shouldPreload = true, // forces the products() call
                    storeManagerOverride = storeManager,
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertEquals(
                "ok",
                (s.manager.configState.value as ConfigState.Retrieved).config.buildId,
            )
            coVerify(atLeast = 1) { storeManager.products(any()) }
        }

    @Test
    fun `tier override false suppresses preload even when shouldPreload is true`() =
        runTest(timeout = 30.seconds) {
            val s =
                setup(
                    backgroundScope,
                    deviceTier = Tier.LOW,
                    shouldPreload = true,
                    preloadDeviceOverrides = mapOf(Tier.LOW to false),
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `tier override true forces preload even when shouldPreload is false`() =
        runTest(timeout = 30.seconds) {
            val s =
                setup(
                    backgroundScope,
                    deviceTier = Tier.LOW,
                    shouldPreload = false,
                    preloadDeviceOverrides = mapOf(Tier.LOW to true),
                )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(atLeast = 1) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `getAssignments success triggers PreloadIfEnabled`() =
        runTest(timeout = 30.seconds) {
            val configWithTriggers =
                Config.stub().copy(triggers = setOf(Trigger.stub().copy(eventName = "evt")))
            val assignments =
                mockk<Assignments>(relaxed = true) {
                    coEvery { getAssignments(any()) } returns Either.Success(emptyList())
                }
            val s =
                setup(
                    backgroundScope,
                    shouldPreload = true,
                    assignments = assignments,
                )
            s.manager.applyRetrievedConfigForTesting(configWithTriggers)
            // Reset the mock so we only count post-assignment preloads.
            io.mockk.clearMocks(s.preload, answers = false)
            coEvery { s.preload.preloadAllPaywalls(any(), any(), any()) } just Runs
            coEvery { s.preload.preloadPaywallsByNames(any(), any()) } just Runs
            coEvery { s.preload.removeUnusedPaywallVCsFromCache(any(), any()) } just Runs

            s.manager.getAssignments()
            advanceUntilIdle()

            coVerify(atLeast = 1) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `getAssignments suspends until config is Retrieved`() =
        runTest(timeout = 30.seconds) {
            val assignments = mockk<Assignments>(relaxed = true) {
                coEvery { getAssignments(any()) } returns Either.Success(emptyList())
            }
            val s = setup(backgroundScope, assignments = assignments)

            val job = launch { s.manager.getAssignments() }
            delay(50)
            assertTrue("getAssignments should still be suspended", job.isActive)
            coVerify(exactly = 0) { assignments.getAssignments(any()) }

            s.manager.applyRetrievedConfigForTesting(
                Config.stub().copy(triggers = setOf(Trigger.stub().copy(eventName = "e"))),
            )
            job.join()
            advanceUntilIdle()

            coVerify(atLeast = 1) { assignments.getAssignments(any()) }
        }

    @Test
    fun `getAssignments with no triggers does not hit the network`() =
        runTest(timeout = 30.seconds) {
            val assignments = mockk<Assignments>(relaxed = true)
            val s = setup(backgroundScope, assignments = assignments)
            s.manager.applyRetrievedConfigForTesting(Config.stub().copy(triggers = emptySet()))

            s.manager.getAssignments()
            advanceUntilIdle()

            coVerify(exactly = 0) { assignments.getAssignments(any()) }
        }

    @Test
    fun `getAssignments network error is swallowed and state stays Retrieved`() =
        runTest(timeout = 30.seconds) {
            val assignments = mockk<Assignments>(relaxed = true) {
                coEvery { getAssignments(any()) } returns Either.Failure(NetworkError.Unknown())
            }
            val s = setup(backgroundScope, assignments = assignments)
            s.manager.applyRetrievedConfigForTesting(
                Config.stub().copy(triggers = setOf(Trigger.stub().copy(eventName = "e"))),
            )

            s.manager.getAssignments()
            advanceUntilIdle()

            assertTrue(s.manager.configState.value is ConfigState.Retrieved)
        }

    @Test
    fun `refreshConfiguration without retrieved config does not hit network`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            s.manager.refreshConfiguration()
            advanceUntilIdle()
            coVerify(exactly = 0) { s.network.getConfig(any()) }
        }

    @Test
    fun `refreshConfiguration with flag disabled and force false does not hit network`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            s.manager.applyRetrievedConfigForTesting(Config.stub()) // no enableConfigRefresh flag
            io.mockk.clearMocks(s.network, answers = false)
            coEvery { s.network.getConfig(any()) } returns Either.Success(Config.stub())

            s.manager.refreshConfiguration(force = false)
            advanceUntilIdle()

            coVerify(exactly = 0) { s.network.getConfig(any()) }
        }

    @Test
    fun `refreshConfiguration force true ignores disabled flag`() =
        runTest(timeout = 30.seconds) {
            val s = setup(
                backgroundScope,
                networkConfig = Either.Success(Config.stub().copy(buildId = "forced")),
            )
            s.manager.applyRetrievedConfigForTesting(Config.stub())
            io.mockk.clearMocks(s.network, answers = false)
            coEvery { s.network.getConfig(any()) } returns Either.Success(Config.stub().copy(buildId = "forced"))
            coEvery { s.network.getEnrichment(any(), any(), any()) } returns Either.Success(Enrichment.stub())

            s.manager.refreshConfiguration(force = true)
            advanceUntilIdle()

            coVerify(atLeast = 1) { s.network.getConfig(any()) }
        }

    @Test
    fun `refreshConfiguration is noop when state is Retrieving or None`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)

            s.manager.setConfigStateForTesting(ConfigState.Retrieving)
            s.manager.refreshConfiguration(force = false)
            advanceUntilIdle()
            coVerify(exactly = 0) { s.network.getConfig(any()) }

            s.manager.setConfigStateForTesting(ConfigState.None)
            s.manager.refreshConfiguration(force = false)
            advanceUntilIdle()
            coVerify(exactly = 0) { s.network.getConfig(any()) }
        }

    @Test
    fun `refreshConfiguration success resets paywall request cache and removes unused`() =
        runTest(timeout = 30.seconds) {
            val oldConfig = config(buildId = "old", enableRefresh = true)
            val newConfig = config(buildId = "new", enableRefresh = true)
            val paywallManager = mockk<PaywallManager>(relaxed = true)
            val preload = mockk<PaywallPreload>(relaxed = true) {
                coEvery { preloadAllPaywalls(any(), any(), any()) } just Runs
                coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
            }
            val s = setup(backgroundScope)
            val mgr = ConfigManagerForTest(
                context = mockk(relaxed = true),
                storage = s.storage,
                network = mockk<SuperwallAPI> {
                    coEvery { getConfig(any()) } returns Either.Success(newConfig)
                    coEvery { getEnrichment(any(), any(), any()) } returns Either.Success(Enrichment.stub())
                },
                deviceHelper = s.deviceHelper,
                paywallManager = paywallManager,
                storeManager = s.storeManager,
                preload = preload,
                webRedeemer = s.webRedeemer,
                factory = mockk(relaxed = true) {
                    coEvery { makeSessionDeviceAttributes() } returns HashMap()
                },
                entitlements = mockk(relaxed = true) {
                    every { status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { entitlementsByProductId } returns emptyMap()
                },
                assignments = mockk(relaxed = true),
                options = SuperwallOptions().apply { paywalls.shouldPreload = false },
                ioScope = backgroundScope,
                testMode = null,
                tracker = {},
                setSubscriptionStatus = null,
                activateTestMode = { _, _ -> },
            )
            mgr.applyRetrievedConfigForTesting(oldConfig)

            mgr.refreshConfiguration()
            advanceUntilIdle()

            verify(atLeast = 1) { paywallManager.resetPaywallRequestCache() }
            coVerify(atLeast = 1) { preload.removeUnusedPaywallVCsFromCache(oldConfig, newConfig) }
        }

    @Test
    fun `refreshConfig failure preserves Retrieved state`() =
        runTest(timeout = 30.seconds) {
            val oldConfig = config(buildId = "old", enableRefresh = true)
            val s = setup(
                backgroundScope,
                networkConfig = Either.Failure(NetworkError.Unknown()),
            )
            s.manager.applyRetrievedConfigForTesting(oldConfig)

            s.manager.refreshConfiguration(force = true)
            advanceUntilIdle()

            assertTrue(s.manager.configState.value is ConfigState.Retrieved)
            assertEquals("old", s.manager.config?.buildId)
        }

    @Test
    fun `reset without config does not preload`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            s.manager.reset()
            advanceUntilIdle()
            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `reset with config rebuilds assignments synchronously`() =
        runTest(timeout = 30.seconds) {
            val assignments = mockk<Assignments>(relaxed = true)
            val s = setup(backgroundScope, assignments = assignments)
            s.manager.applyRetrievedConfigForTesting(Config.stub())
            io.mockk.clearMocks(assignments, answers = false)
            io.mockk.justRun { assignments.reset() }
            io.mockk.justRun { assignments.choosePaywallVariants(any()) }

            s.manager.reset()
            verify(exactly = 1) { assignments.reset() }
            verify(exactly = 1) { assignments.choosePaywallVariants(any()) }
        }

    @Test
    fun `preloadAllPaywalls suspends until config is Retrieved`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            val job = launch { s.manager.preloadAllPaywalls() }
            delay(50)
            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }

            val cfg = Config.stub().copy(buildId = "preload-all")
            s.manager.applyRetrievedConfigForTesting(cfg)
            job.join()
            advanceUntilIdle()

            coVerify(exactly = 1) { s.preload.preloadAllPaywalls(eq(cfg), any(), any()) }
        }

    @Test
    fun `preloadPaywallsByNames suspends until config is Retrieved`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            val names = setOf("evt")
            val job = launch { s.manager.preloadPaywallsByNames(names) }
            delay(50)
            coVerify(exactly = 0) { s.preload.preloadPaywallsByNames(any(), any()) }

            val cfg = Config.stub().copy(buildId = "preload-named")
            s.manager.applyRetrievedConfigForTesting(cfg)
            job.join()
            advanceUntilIdle()

            coVerify(exactly = 1) { s.preload.preloadPaywallsByNames(eq(cfg), eq(names)) }
        }

    @Test
    fun `fetchConfiguration updates trigger cache and persists feature flags`() =
        runTest(timeout = 30.seconds) {
            val cfg = Config.stub().copy(
                triggers = setOf(Trigger.stub().copy(eventName = "evt_a")),
                rawFeatureFlags = listOf(
                    RawFeatureFlag("enable_config_refresh_v2", true),
                    RawFeatureFlag("disable_verbose_events", true),
                ),
            )
            val s = setup(backgroundScope, networkConfig = Either.Success(cfg))

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertEquals(setOf("evt_a"), s.manager.triggersByEventName.keys)
            verify { s.storage.write(DisableVerboseEvents, true) }
            verify { s.storage.write(LatestConfig, cfg) }
        }

    @Test
    fun `fetchConfiguration loads purchased products when not in test mode`() =
        runTest(timeout = 30.seconds) {
            val storeManager = mockk<StoreManager>(relaxed = true) {
                coEvery { products(any()) } returns emptySet()
                coEvery { loadPurchasedProducts(any()) } just Runs
            }
            val s = setup(backgroundScope, storeManagerOverride = storeManager)

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(atLeast = 1) { storeManager.loadPurchasedProducts(any()) }
        }

    @Test
    fun `fetchConfiguration redeems existing web entitlements when not in test mode`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(atLeast = 1) { s.webRedeemer.redeem(WebPaywallRedeemer.RedeemType.Existing) }
        }

    @Test
    fun `fetchConfiguration preloads products when preloading enabled`() =
        runTest(timeout = 30.seconds) {
            val cfg = Config.stub().copy(
                paywalls = listOf(
                    com.superwall.sdk.models.paywall.Paywall.stub().copy(productIds = listOf("a", "b")),
                    com.superwall.sdk.models.paywall.Paywall.stub().copy(productIds = listOf("b", "c")),
                ),
            )
            val storeManager = mockk<StoreManager>(relaxed = true) {
                coEvery { products(any()) } returns emptySet()
                coEvery { loadPurchasedProducts(any()) } just Runs
            }
            val s = setup(
                backgroundScope,
                networkConfig = Either.Success(cfg),
                shouldPreload = true,
                storeManagerOverride = storeManager,
            )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(exactly = 1) {
                storeManager.products(match { it == setOf("a", "b", "c") })
            }
        }

    @Test
    fun `fetchConfiguration emits Retrieving then Failed without cache`() =
        runTest(timeout = 30.seconds) {
            val s = setup(
                backgroundScope,
                networkConfig = Either.Failure(NetworkError.Unknown()),
            )
            val states = CopyOnWriteArrayList<ConfigState>()
            val collector = CoroutineScope(Dispatchers.Unconfined).launch {
                s.manager.configState.collect { states.add(it) }
            }
            s.manager.fetchConfiguration()
            advanceUntilIdle()
            collector.cancel()

            assertTrue("Expected Retrieving in lineage, got $states", states.any { it is ConfigState.Retrieving })
            assertTrue("Expected last state Failed, got ${states.last()}", states.last() is ConfigState.Failed)
        }

    @Test
    fun `cached config wins when network getConfig returns Failure`() =
        runTest(timeout = 30.seconds) {
            val cached = config(buildId = "cached", enableRefresh = true)
            val s = setup(
                backgroundScope,
                cachedConfig = cached,
                cachedEnrichment = Enrichment.stub(),
                networkConfig = Either.Failure(NetworkError.Unknown()),
            )

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertEquals("cached", s.manager.config?.buildId)
        }

    @Test
    fun `quick network success returns fresh config`() =
        runTest(timeout = 30.seconds) {
            val fresh = Config.stub().copy(buildId = "fresh")
            val s = setup(backgroundScope, networkConfig = Either.Success(fresh))

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertEquals("fresh", s.manager.config?.buildId)
        }

    @Test
    fun `cached path with delayed network falls back to cache`() =
        runTest(timeout = 30.seconds) {
            val cached = config(buildId = "cached", enableRefresh = true)
            val s = setup(
                backgroundScope,
                cachedConfig = cached,
                cachedEnrichment = Enrichment.stub(),
                networkConfig = Either.Failure(NetworkError.Unknown()),
            )

            s.manager.fetchConfiguration()
            s.manager.configState.first { (it as? ConfigState.Retrieved)?.config?.buildId == "cached" }
            advanceUntilIdle()

            assertEquals("cached", s.manager.config?.buildId)
        }

    @Test
    fun `network retry callback transitions state to Retrying`() =
        runTest(timeout = 30.seconds) {
            val retries = AtomicInteger(0)
            val s = setup(
                backgroundScope,
                networkConfigAnswer = { cb ->
                    cb()
                    cb()
                    retries.set(2)
                    Either.Success(Config.stub())
                },
            )
            val seen = CopyOnWriteArrayList<ConfigState>()
            val collector = CoroutineScope(Dispatchers.Unconfined).launch {
                s.manager.configState.collect { seen.add(it) }
            }
            s.manager.fetchConfiguration()
            advanceUntilIdle()
            collector.cancel()

            assertEquals(2, retries.get())
            assertTrue("Expected Retrying in $seen", seen.any { it is ConfigState.Retrying })
        }

    @Test
    fun `cached config success preloads before refresh`() =
        runTest(timeout = 30.seconds) {
            val cached = config(buildId = "cached", enableRefresh = true)
            val fresh = config(buildId = "fresh", enableRefresh = true)
            val getCalls = AtomicInteger(0)
            val s = setup(
                backgroundScope,
                cachedConfig = cached,
                cachedEnrichment = Enrichment.stub(),
                shouldPreload = true,
                networkConfigAnswer = {
                    val n = getCalls.incrementAndGet()
                    if (n == 1) Either.Failure(NetworkError.Unknown()) // cached fallback wins
                    else Either.Success(fresh)
                },
            )

            s.manager.fetchConfiguration()
            s.manager.configState.first { (it as? ConfigState.Retrieved)?.config?.buildId == "fresh" }
            advanceUntilIdle()

            coVerifyOrder {
                s.preload.preloadAllPaywalls(any(), any(), any())
                s.network.getConfig(any())
            }
            assertTrue(getCalls.get() >= 2)
        }

    @Test
    fun `concurrent fetchConfiguration calls dedup while Retrieving`() =
        runTest(timeout = 30.seconds) {
            val calls = AtomicInteger(0)
            val s = setup(
                backgroundScope,
                networkConfigAnswer = {
                    calls.incrementAndGet()
                    delay(300)
                    Either.Success(Config.stub())
                },
            )

            val first = launch { s.manager.fetchConfiguration() }
            s.manager.configState.first { it is ConfigState.Retrieving }
            s.manager.fetchConfiguration() // must early-return
            first.join()

            assertEquals(1, calls.get())
        }

    @Test
    fun `reevaluateTestMode flips state synchronously`() =
        runTest(timeout = 30.seconds) {
            val storage = mockk<Storage>(relaxed = true)
            val testMode = TestMode(storage = storage, isTestEnvironment = false)
            val s = setup(
                backgroundScope,
                injectedTestMode = testMode,
                testModeBehavior = TestModeBehavior.ALWAYS,
            )
            assertFalse(testMode.isTestMode)

            s.manager.reevaluateTestMode(config = Config.stub(), appUserId = "u")
            assertTrue(testMode.isTestMode)
        }

    @Test
    fun `applyConfig side effects happen before Retrieved`() =
        runTest(timeout = 30.seconds) {
            val cfg = Config.stub().copy(
                triggers = setOf(Trigger.stub().copy(eventName = "evt")),
                rawFeatureFlags = listOf(RawFeatureFlag("disable_verbose_events", true)),
            )
            val s = setup(backgroundScope, networkConfig = Either.Success(cfg))

            val triggersAtRetrieved = mutableListOf<String>()
            val collector = launch {
                s.manager.configState.first { it is ConfigState.Retrieved }
                triggersAtRetrieved.addAll(s.manager.triggersByEventName.keys)
            }
            s.manager.fetchConfiguration()
            collector.join()

            assertTrue(triggersAtRetrieved.contains("evt"))
            verify { s.storage.write(DisableVerboseEvents, true) }
        }

    @Test
    fun `applyConfig skips LatestConfig write when refresh flag off`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope, networkConfig = Either.Success(Config.stub()))

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            verify(exactly = 0) { s.storage.write(LatestConfig, any()) }
            verify { s.storage.write(DisableVerboseEvents, any()) }
        }

    @Test
    fun `applyConfig with null testMode loads purchased products`() =
        runTest(timeout = 30.seconds) {
            val storeManager = mockk<StoreManager>(relaxed = true) {
                coEvery { loadPurchasedProducts(any()) } just Runs
                coEvery { products(any()) } returns emptySet()
            }
            val s = setup(backgroundScope, injectedTestMode = null, storeManagerOverride = storeManager)

            s.manager.fetchConfiguration()
            s.manager.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(atLeast = 1) { storeManager.loadPurchasedProducts(any()) }
        }

    @Test
    fun `applyConfig testMode just-activated publishes subscription status`() =
        runTest(timeout = 30.seconds) {
            val storage = mockk<Storage>(relaxed = true)
            val testMode = TestMode(storage = storage, isTestEnvironment = false)
            val s = setup(
                backgroundScope,
                injectedTestMode = testMode,
                testModeBehavior = TestModeBehavior.ALWAYS,
            )
            assertFalse(testMode.isTestMode)

            s.manager.fetchConfiguration()
            advanceUntilIdle()

            assertTrue(testMode.isTestMode)
            assertTrue(testMode.overriddenSubscriptionStatus != null)
        }

    @Test
    fun `applyConfig deactivates testMode when user no longer qualifies`() =
        runTest(timeout = 30.seconds) {
            val storage = mockk<Storage>(relaxed = true)
            val testMode = spyk(TestMode(storage = storage, isTestEnvironment = false))
            testMode.evaluateTestMode(
                Config.stub(), "com.app", null, null,
                testModeBehavior = TestModeBehavior.ALWAYS,
            )
            assertTrue(testMode.isTestMode)

            val s = setup(
                backgroundScope,
                injectedTestMode = testMode,
                testModeBehavior = TestModeBehavior.AUTOMATIC,
            )

            s.manager.fetchConfiguration()
            advanceUntilIdle()

            assertFalse(testMode.isTestMode)
            verify(atLeast = 1) { testMode.clearTestModeState() }
        }

    @Test
    fun `enrichment failure with cached fallback uses cache and schedules retry`() =
        runTest(timeout = 30.seconds) {
            val cachedEnrichment = Enrichment.stub()
            val cached = config(enableRefresh = true)
            val helper = mockk<DeviceHelper>(relaxed = true) {
                every { appVersion } returns "1.0"
                every { locale } returns "en-US"
                every { deviceTier } returns Tier.MID
                every { bundleId } returns "com.test"
                every { setEnrichment(any()) } just Runs
                coEvery { getTemplateDevice() } returns emptyMap()
                coEvery { getEnrichment(any(), any()) } returns Either.Failure(NetworkError.Unknown())
            }
            val s = setup(
                backgroundScope,
                cachedConfig = cached,
                cachedEnrichment = cachedEnrichment,
            )
            val mgr = ConfigManagerForTest(
                context = mockk(relaxed = true),
                storage = s.storage,
                network = s.network,
                deviceHelper = helper,
                paywallManager = mockk(relaxed = true),
                storeManager = s.storeManager,
                preload = s.preload,
                webRedeemer = s.webRedeemer,
                factory = mockk(relaxed = true) {
                    coEvery { makeSessionDeviceAttributes() } returns HashMap()
                },
                entitlements = mockk(relaxed = true) {
                    every { status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { entitlementsByProductId } returns emptyMap()
                },
                assignments = mockk(relaxed = true),
                options = SuperwallOptions(),
                ioScope = backgroundScope,
                testMode = null,
                tracker = {},
                setSubscriptionStatus = null,
                activateTestMode = { _, _ -> },
            )

            mgr.fetchConfiguration()
            mgr.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            verify { helper.setEnrichment(cachedEnrichment) }
            coVerify(atLeast = 1) { helper.getEnrichment(6, 1.seconds) }
        }

    @Test
    fun `enrichment failure with no cache still reaches Retrieved`() =
        runTest(timeout = 30.seconds) {
            val helper = mockk<DeviceHelper>(relaxed = true) {
                every { appVersion } returns "1.0"
                every { locale } returns "en-US"
                every { deviceTier } returns Tier.MID
                every { bundleId } returns "com.test"
                every { setEnrichment(any()) } just Runs
                coEvery { getTemplateDevice() } returns emptyMap()
                coEvery { getEnrichment(any(), any()) } returns Either.Failure(NetworkError.Unknown())
            }
            val s = setup(backgroundScope)
            val mgr = ConfigManagerForTest(
                context = mockk(relaxed = true),
                storage = s.storage,
                network = s.network,
                deviceHelper = helper,
                paywallManager = mockk(relaxed = true),
                storeManager = s.storeManager,
                preload = s.preload,
                webRedeemer = s.webRedeemer,
                factory = mockk(relaxed = true) {
                    coEvery { makeSessionDeviceAttributes() } returns HashMap()
                },
                entitlements = mockk(relaxed = true) {
                    every { status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { entitlementsByProductId } returns emptyMap()
                },
                assignments = mockk(relaxed = true),
                options = SuperwallOptions(),
                ioScope = backgroundScope,
                testMode = null,
                tracker = {},
                setSubscriptionStatus = null,
                activateTestMode = { _, _ -> },
            )

            mgr.fetchConfiguration()
            mgr.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            coVerify(atLeast = 1) { helper.getEnrichment(6, 1.seconds) }
        }

    @Test
    fun `cached path retry callback invokes awaitUtilNetwork`() =
        runTest(timeout = 30.seconds) {
            val cached = config(enableRefresh = true)
            val awaitCalls = AtomicInteger(0)
            val network = mockk<SuperwallAPI> {
                coEvery { getConfig(any()) } coAnswers {
                    val cb = firstArg<suspend () -> Unit>()
                    cb()
                    Either.Success(cached)
                }
                coEvery { getEnrichment(any(), any(), any()) } returns Either.Success(Enrichment.stub())
            }
            val storage = mockk<Storage>(relaxed = true) {
                every { read(LatestConfig) } returns cached
                every { read(LatestEnrichment) } returns null
                every { write(any(), any<Any>()) } just Runs
            }
            val mgr = ConfigManagerForTest(
                context = mockk(relaxed = true),
                storage = storage,
                network = network,
                deviceHelper = mockk(relaxed = true) {
                    every { appVersion } returns "1.0"
                    every { locale } returns "en-US"
                    every { deviceTier } returns Tier.MID
                    every { bundleId } returns "com.test"
                    every { setEnrichment(any()) } just Runs
                    coEvery { getTemplateDevice() } returns emptyMap()
                    coEvery { getEnrichment(any(), any()) } returns Either.Success(Enrichment.stub())
                },
                paywallManager = mockk(relaxed = true),
                storeManager = mockk(relaxed = true) {
                    coEvery { products(any()) } returns emptySet()
                    coEvery { loadPurchasedProducts(any()) } just Runs
                },
                preload = mockk(relaxed = true) {
                    coEvery { preloadAllPaywalls(any(), any(), any()) } just Runs
                    coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                    coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                },
                webRedeemer = mockk(relaxed = true),
                factory = mockk(relaxed = true) {
                    coEvery { makeSessionDeviceAttributes() } returns HashMap()
                },
                entitlements = mockk(relaxed = true) {
                    every { status } returns kotlinx.coroutines.flow.MutableStateFlow(SubscriptionStatus.Unknown)
                    every { entitlementsByProductId } returns emptyMap()
                },
                assignments = mockk(relaxed = true),
                options = SuperwallOptions(),
                ioScope = backgroundScope,
                testMode = null,
                tracker = {},
                setSubscriptionStatus = null,
                activateTestMode = { _, _ -> },
                awaitUtilNetwork = { awaitCalls.incrementAndGet() },
            )

            mgr.fetchConfiguration()
            mgr.configState.first { it is ConfigState.Retrieved }
            advanceUntilIdle()

            assertTrue("awaitUtilNetwork must fire on retry callback, got ${awaitCalls.get()}", awaitCalls.get() >= 1)
        }

    @Test
    fun `config getter on Failed returns null and dispatches a refetch`() =
        runTest(timeout = 30.seconds) {
            val calls = AtomicInteger(0)
            val s = setup(
                backgroundScope,
                networkConfigAnswer = {
                    val n = calls.incrementAndGet()
                    if (n == 1) Either.Failure(NetworkError.Unknown())
                    else Either.Success(Config.stub())
                },
            )
            s.manager.setConfigStateForTesting(ConfigState.Failed(Exception("boom")))
            assertEquals(null, s.manager.config)
            advanceUntilIdle()
            assertTrue("Expected getter to trigger a refetch, calls=${calls.get()}", calls.get() >= 1)
        }

    @Test
    fun `config getter on Retrieved does not dispatch fetch`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            s.manager.applyRetrievedConfigForTesting(Config.stub())
            io.mockk.clearMocks(s.network, answers = false)
            coEvery { s.network.getConfig(any()) } returns Either.Success(Config.stub())

            repeat(5) { s.manager.config }
            advanceUntilIdle()
            coVerify(exactly = 0) { s.network.getConfig(any()) }
        }

    @Test
    fun `hasConfig emits when config is set`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            val expected = Config.stub().copy(buildId = "has-config")
            val emitted = launch {
                assertEquals(expected.buildId, s.manager.hasConfig.first().buildId)
            }
            s.manager.applyRetrievedConfigForTesting(expected)
            advanceUntilIdle()
            emitted.join()
        }

    @Test
    fun `hasConfig emits exactly once`() =
        runTest(timeout = 30.seconds) {
            val s = setup(backgroundScope)
            val emissions = mutableListOf<Config>()
            val collector = launch {
                s.manager.hasConfig.collect { emissions.add(it) }
            }
            s.manager.applyRetrievedConfigForTesting(Config.stub().copy(buildId = "first"))
            advanceUntilIdle()
            s.manager.setConfigStateForTesting(ConfigState.None)
            advanceUntilIdle()
            s.manager.applyRetrievedConfigForTesting(Config.stub().copy(buildId = "second"))
            advanceUntilIdle()
            collector.cancel()

            assertEquals(1, emissions.size)
            assertEquals("first", emissions.single().buildId)
        }

    // MARK: - recheckPreloadIfNeeded

    private fun triggerWith(behavior: TriggerPreloadBehavior, experimentId: String = "exp"): Trigger =
        Trigger.stub().apply {
            rules =
                listOf(
                    TriggerRule.stub().apply {
                        this.experimentId = experimentId
                        this.preload.behavior = behavior
                    },
                )
        }

    /** Wires `lastFingerprint` on a PaywallPreload mock to a real AtomicReference. */
    private fun bindFingerprintHolder(preload: PaywallPreload): java.util.concurrent.atomic.AtomicReference<String?> {
        val holder = java.util.concurrent.atomic.AtomicReference<String?>(null)
        every { preload.lastFingerprint } returns holder
        return holder
    }

    @Test
    fun `recheckPreloadIfNeeded is a no-op when config has no IF_TRUE rule`() =
        runTest(timeout = 5.seconds) {
            val s = setup(backgroundScope)
            bindFingerprintHolder(s.preload)
            coEvery { s.deviceHelper.preloadFingerprint() } returns "X"

            s.manager.applyRetrievedConfigForTesting(
                config(triggers = setOf(triggerWith(TriggerPreloadBehavior.ALWAYS))),
            )
            advanceUntilIdle()

            // Ignore any preload calls triggered by config retrieval itself.
            io.mockk.clearMocks(s.preload, answers = false, recordedCalls = true)
            bindFingerprintHolder(s.preload)
            coEvery { s.preload.preloadAllPaywalls(any(), any(), any()) } just Runs

            s.manager.recheckPreloadIfNeeded()
            advanceUntilIdle()

            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `recheckPreloadIfNeeded is a no-op when fingerprint has not changed`() =
        runTest(timeout = 5.seconds) {
            val s = setup(backgroundScope)
            val holder = bindFingerprintHolder(s.preload)
            holder.set("SAME")
            coEvery { s.deviceHelper.preloadFingerprint() } returns "SAME"

            s.manager.applyRetrievedConfigForTesting(
                config(triggers = setOf(triggerWith(TriggerPreloadBehavior.IF_TRUE))),
            )
            advanceUntilIdle()

            io.mockk.clearMocks(s.preload, answers = false, recordedCalls = true)
            every { s.preload.lastFingerprint } returns holder
            coEvery { s.preload.preloadAllPaywalls(any(), any(), any()) } just Runs

            s.manager.recheckPreloadIfNeeded()
            advanceUntilIdle()

            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }

    @Test
    fun `recheckPreloadIfNeeded dispatches preload when fingerprint changes`() =
        runTest(timeout = 5.seconds) {
            // shouldPreload = true so PreloadIfEnabled's gate doesn't short-circuit.
            val s = setup(backgroundScope, shouldPreload = true)
            val holder = bindFingerprintHolder(s.preload)
            holder.set("OLD")
            coEvery { s.deviceHelper.preloadFingerprint() } returns "NEW"

            val cfg = config(triggers = setOf(triggerWith(TriggerPreloadBehavior.IF_TRUE)))
            s.manager.applyRetrievedConfigForTesting(cfg)
            advanceUntilIdle()

            io.mockk.clearMocks(s.preload, answers = false, recordedCalls = true)
            every { s.preload.lastFingerprint } returns holder
            coEvery { s.preload.preloadAllPaywalls(any(), any(), any()) } just Runs

            s.manager.recheckPreloadIfNeeded()
            advanceUntilIdle()

            coVerify(exactly = 1) { s.preload.preloadAllPaywalls(eq(cfg), any(), eq("NEW")) }
        }

    @Test
    fun `recheckPreloadIfNeeded does not preload when shouldPreload is disabled`() =
        runTest(timeout = 5.seconds) {
            // shouldPreload = false → PreloadIfEnabled's gate must short-circuit.
            val s = setup(backgroundScope, shouldPreload = false)
            val holder = bindFingerprintHolder(s.preload)
            holder.set("OLD")
            coEvery { s.deviceHelper.preloadFingerprint() } returns "NEW"

            val cfg = config(triggers = setOf(triggerWith(TriggerPreloadBehavior.IF_TRUE)))
            s.manager.applyRetrievedConfigForTesting(cfg)
            advanceUntilIdle()

            io.mockk.clearMocks(s.preload, answers = false, recordedCalls = true)
            every { s.preload.lastFingerprint } returns holder
            coEvery { s.preload.preloadAllPaywalls(any(), any(), any()) } just Runs

            s.manager.recheckPreloadIfNeeded()
            advanceUntilIdle()

            coVerify(exactly = 0) { s.preload.preloadAllPaywalls(any(), any(), any()) }
        }
}

/**
 * Test-only ConfigManager subclass that hardwires the actor and exposes the
 * test-only `applyRetrievedConfigForTesting` helper.
 */
internal class ConfigManagerForTest(
    context: Context,
    storage: Storage,
    network: SuperwallAPI,
    deviceHelper: DeviceHelper,
    paywallManager: PaywallManager,
    storeManager: StoreManager,
    preload: PaywallPreload,
    webRedeemer: WebPaywallRedeemer,
    factory: ConfigManager.Factory,
    entitlements: Entitlements,
    assignments: Assignments,
    options: SuperwallOptions,
    ioScope: CoroutineScope,
    testMode: TestMode?,
    tracker: suspend (TrackableSuperwallEvent) -> Unit,
    setSubscriptionStatus: ((SubscriptionStatus) -> Unit)?,
    activateTestMode: suspend (Config, Boolean) -> Unit,
    identityManager: (() -> IdentityManager)? = null,
    awaitUtilNetwork: suspend () -> Unit = {},
) : ConfigManager(
        context = context,
        storeManager = storeManager,
        entitlements = entitlements,
        storage = storage,
        network = network,
        deviceHelper = deviceHelper,
        options = options,
        paywallManager = paywallManager,
        webPaywallRedeemer = { webRedeemer },
        factory = factory,
        assignments = assignments,
        paywallPreload = preload,
        ioScope = IOScope(Dispatchers.Unconfined),
        tracker = tracker,
        testMode = testMode,
        identityManager = identityManager,
        setSubscriptionStatus = setSubscriptionStatus,
        awaitUtilNetwork = awaitUtilNetwork,
        activateTestMode = activateTestMode,
        actor = SequentialActor(ConfigState.None, CoroutineScope(Dispatchers.Unconfined)),
    )
