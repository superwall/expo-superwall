package com.superwall.sdk.config

import And
import Given
import Then
import When
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.models.getConfig
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.config.RawFeatureFlag
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.VariantOption
import com.superwall.sdk.network.Network
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.NetworkMock
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.storage.CONSTANT_API_KEY
import com.superwall.sdk.storage.LatestConfig
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StorageMock
import com.superwall.sdk.storage.StoredEntitlementsByProductId
import com.superwall.sdk.storage.StoredSubscriptionStatus
import com.superwall.sdk.storage.core_data.convertToJsonElement
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.web.WebPaywallRedeemer
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConfigManagerUnderTest(
    private val context: Context,
    private val storage: Storage,
    private val network: SuperwallAPI,
    private val paywallManager: PaywallManager,
    private val storeManager: StoreManager,
    private val factory: Factory,
    private val deviceHelper: DeviceHelper,
    private val assignments: Assignments,
    private val paywallPreload: PaywallPreload,
    private val ioScope: CoroutineScope,
) : ConfigManager(
        context = context,
        storage = storage,
        network = network,
        paywallManager = paywallManager,
        storeManager = storeManager,
        factory = factory,
        deviceHelper = deviceHelper,
        options = SuperwallOptions(),
        assignments = assignments,
        paywallPreload = paywallPreload,
        ioScope = IOScope(ioScope.coroutineContext),
        track = {},
        entitlements =
            Entitlements(
                mockk<Storage>(relaxUnitFun = true) {
                    every { read(StoredSubscriptionStatus) } returns SubscriptionStatus.Unknown
                    every { read(StoredEntitlementsByProductId) } returns emptyMap()
                    every { read(LatestRedemptionResponse) } returns null
                },
            ),
        awaitUtilNetwork = {},
        webPaywallRedeemer = { mockk<WebPaywallRedeemer>(relaxed = true) },
    ) {
    suspend fun setConfig(config: Config) {
        configState.emit(ConfigState.Retrieved(config))
    }
}

@RunWith(AndroidJUnit4::class)
class ConfigManagerTests {
    val mockDeviceHelper =
        mockk<DeviceHelper> {
            every { appVersion } returns "1.0"
            every { locale } returns "en-US"
            coEvery { getTemplateDevice() } returns emptyMap()
            coEvery {
                getEnrichment(any(), any())
            } returns Either.Success(Enrichment.stub())
        }

    @Before
    fun setup() {
        if (!Superwall.initialized) {
            Superwall.configure(
                InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application,
                CONSTANT_API_KEY,
                options = SuperwallOptions().apply { paywalls.shouldPreload = false },
            )
        }
    }

    @Test
    fun test_confirmAssignment() =
        runTest(timeout = 5.minutes) {
            Given("we have a ConfigManager with a mock assignment") {
                // get context
                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val experimentId = "abc"
                val variantId = "def"
                val variant =
                    Experiment.Variant(
                        id = variantId,
                        type = Experiment.Variant.VariantType.TREATMENT,
                        paywallId = "jkl",
                    )
                val assignment =
                    ConfirmableAssignment(experimentId = experimentId, variant = variant)
                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignments = Assignments(storage, network, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any()) } just Runs
                        coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                        coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                    }

                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = network,
                        paywallManager = dependencyContainer.paywallManager,
                        storeManager = dependencyContainer.storeManager,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignments,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we confirm the assignment") {
                    assignments.confirmAssignment(assignment)
                    delay(500)
                }

                Then("the assignment should be confirmed and stored correctly") {
                    assertTrue(network.assignmentsConfirmed)
                    assertEquals(storage.getConfirmedAssignments()[experimentId], variant)
                    assertNull(configManager.unconfirmedAssignments[experimentId])
                }
            }
        }

    @Test
    fun test_loadAssignments_noConfig() =
        runTest(timeout = 5.minutes) {
            Given("we have a ConfigManager with no config") {
                // get context
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignments = Assignments(storage, network, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any()) } just Runs
                        coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                        coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                    }

                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = network,
                        paywallManager = dependencyContainer.paywallManager,
                        storeManager = dependencyContainer.storeManager,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignments,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                Log.e("test", "test_loadAssignments_noConfig")
                When("we try to get assignments") {
                    val job =
                        launch {
                            configManager.getAssignments()
                            ensureActive()
                            assert(false) // Make sure we never get here...
                        }
                    delay(1000)
                    job.cancel()
                }

                Log.e("test", "test_loadAssignments_noConfig")
                Then("no assignments should be stored") {
                    assertTrue(storage.getConfirmedAssignments().isEmpty())
                    assertTrue(configManager.unconfirmedAssignments.isEmpty())
                }
            }
            return@runTest
        }

    @Test
    fun test_loadAssignments_noTriggers() =
        runTest(timeout = 5.minutes) {
            Given("we have a ConfigManager with a config that has no triggers") {
                // get context
                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignments = Assignments(storage, network, backgroundScope)
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = network,
                        paywallManager = dependencyContainer.paywallManager,
                        storeManager = dependencyContainer.storeManager,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignments,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )
                configManager.setConfig(
                    Config.stub().apply { this.triggers = emptySet() },
                )

                When("we get assignments") {
                    configManager.getAssignments()
                }

                Then("no assignments should be stored") {
                    assertTrue(storage.getConfirmedAssignments().isEmpty())
                    assertTrue(configManager.unconfirmedAssignments.isEmpty())
                }
            }
        }

    @Test
    fun test_loadAssignments_saveAssignmentsFromServer() =
        runTest(timeout = 5.minutes) {
            Given("we have a ConfigManager with assignments from the server") {
                // get context
                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignmentStore = Assignments(storage, network, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any()) } just Runs
                        coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                        coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                    }
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = network,
                        paywallManager = dependencyContainer.paywallManager,
                        storeManager = dependencyContainer.storeManager,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                val variantId = "variantId"
                val experimentId = "experimentId"

                val assignments: List<Assignment> =
                    listOf(
                        Assignment(experimentId = experimentId, variantId = variantId),
                    )
                network.assignments = assignments.toMutableList()

                val variantOption = VariantOption.stub().apply { id = variantId }
                configManager.setConfig(
                    Config.stub().apply {
                        triggers =
                            setOf(
                                Trigger.stub().apply {
                                    rules =
                                        listOf(
                                            TriggerRule.stub().apply {
                                                this.experimentId = experimentId
                                                this.variants = listOf(variantOption)
                                            },
                                        )
                                },
                            )
                    },
                )

                When("we get assignments") {
                    configManager.getAssignments()
                    delay(1)
                }

                Then("the assignments should be stored correctly") {
                    assertEquals(
                        storage.getConfirmedAssignments()[experimentId],
                        variantOption.toVariant(),
                    )
                    assertTrue(configManager.unconfirmedAssignments.isEmpty())
                }
            }
            return@runTest
        }

    @Test
    fun should_refresh_config_successfully() =
        runTest(timeout = Duration.INFINITE) {
            Given("we have a ConfigManager with an old config") {
                val mockNetwork =
                    mockk<Network> {
                        coEvery { getConfig(any()) } returns
                            Either.Success<Config, NetworkError>(
                                Config.stub(),
                            )
                        coEvery {
                            getEnrichment(
                                any(),
                                any(),
                                any(),
                            )
                        } returns Either.Success(mockk<Enrichment>())
                    }
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val oldConfig =
                    Config.stub().copy(
                        rawFeatureFlags =
                            listOf(
                                RawFeatureFlag("enable_config_refresh_v2", true),
                            ),
                    )

                val mockPaywallManager =
                    mockk<PaywallManager> {
                        every { resetPaywallRequestCache() } just Runs
                        every { currentView } returns null
                    }

                val mockContainer =
                    spyk(dependencyContainer) {
                        every { deviceHelper } returns mockDeviceHelper
                        every { paywallManager } returns mockPaywallManager
                    }
                val assignments = Assignments(storage, mockNetwork, backgroundScope)

                val testId = "123"
                val configManager =
                    spyk(
                        ConfigManagerUnderTest(
                            context,
                            storage,
                            mockNetwork,
                            mockPaywallManager,
                            dependencyContainer.storeManager,
                            mockContainer,
                            mockDeviceHelper,
                            assignments = assignments,
                            paywallPreload = preload,
                            ioScope = backgroundScope,
                        ),
                    ) {
                        every { config } returns oldConfig.copy(requestId = testId)
                    }

                When("we refresh the configuration") {
                    Superwall.configure(
                        context.applicationContext as Application,
                        "pk_test_1234",
                        null,
                        null,
                        null,
                        null,
                    )
                    configManager.refreshConfiguration()
                }

                Then("the config should be refreshed and the paywall cache reset") {
                    coVerify { mockNetwork.getConfig(any()) }
                    verify { mockPaywallManager.resetPaywallRequestCache() }
                    assertTrue(configManager.config?.requestId === testId)
                }
            }
        }

    @Test
    fun should_fail_refreshing_config_and_keep_old_config() =
        runTest(timeout = Duration.INFINITE) {
            Given("we have a ConfigManager with an old config and a network that fails") {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val mockNetwork =
                    mockk<Network> {
                        coEvery { getConfig(any()) } returns Either.Failure(NetworkError.Unknown())
                        coEvery { getEnrichment(any(), any(), any()) } returns Either.Success(mockk<Enrichment>())
                    }
                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val oldConfig =
                    Config.stub().copy(
                        rawFeatureFlags =
                            listOf(
                                RawFeatureFlag("enable_config_refresh_v2", true),
                            ),
                    )

                val mockPaywallManager =
                    mockk<PaywallManager> {
                        every { resetPaywallRequestCache() } just Runs
                        every { currentView } returns null
                    }

                val mockContainer =
                    spyk(dependencyContainer) {
                        every { deviceHelper } returns mockDeviceHelper
                        every { paywallManager } returns mockPaywallManager
                    }
                val assignments = Assignments(storage, mockNetwork, backgroundScope)

                val testId = "123"
                val configManager =
                    spyk(
                        ConfigManagerUnderTest(
                            context,
                            storage,
                            mockNetwork,
                            mockPaywallManager,
                            dependencyContainer.storeManager,
                            mockContainer,
                            mockDeviceHelper,
                            assignments = assignments,
                            paywallPreload = preload,
                            ioScope = backgroundScope,
                        ),
                    ) {
                        every { config } returns oldConfig.copy(requestId = testId)
                    }

                When("we try to refresh the configuration") {
                    configManager.refreshConfiguration()

                    Then("the old config should be kept") {
                        coVerify { mockNetwork.getConfig(any()) }
                        assertTrue(configManager.config?.requestId === testId)
                    }
                }
            }
        }

    private val storage =
        mockk<Storage> {
            coEvery { write(any(), any()) } just Runs
            coEvery { read(LatestRedemptionResponse) } returns null
            coEvery { read(StoredEntitlementsByProductId) } returns emptyMap()
        }
    private val dependencyContainer =
        mockk<DependencyContainer> {
            coEvery { makeSessionDeviceAttributes() } returns hashMapOf()
            coEvery { provideRuleEvaluator(any()) } returns mockk()
        }

    private val manager =
        mockk<PaywallManager> {
            every { resetPaywallRequestCache() } just Runs
            every { resetCache() } just Runs
        }
    private val storeKit =
        mockk<StoreManager> {
            coEvery { products(any()) } returns emptySet()
            coEvery { loadPurchasedProducts(any()) } just Runs
        }
    private val preload =
        mockk<PaywallPreload> {
            coEvery { preloadAllPaywalls(any(), any()) } just Runs
            coEvery { preloadPaywallsByNames(any(), any()) } just Runs
            coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
        }

    private val localStorage =
        mockk<LocalStorage> {
            every { getConfirmedAssignments() } returns emptyMap()
            every { saveConfirmedAssignments(any()) } just Runs
            coEvery { read(LatestRedemptionResponse) } returns null
            coEvery { read(StoredEntitlementsByProductId) } returns emptyMap()
        }
    private val mockNetwork = mockk<Network>()

    @Test
    fun test_network_delay_with_cached_version() =
        runTest(timeout = 5.minutes) {
            Given("we have a cached config and a delayed network response") {
                val cachedConfig =
                    Config.stub().copy(
                        buildId = "cached",
                        rawFeatureFlags = listOf(RawFeatureFlag("enable_config_refresh_v2", true)),
                    )
                val newConfig = Config.stub().copy(buildId = "not")

                coEvery { storage.read(LatestRedemptionResponse) } returns null
                coEvery { localStorage.read(LatestRedemptionResponse) } returns null
                coEvery { storage.read(LatestConfig) } returns cachedConfig
                coEvery { storage.write(any(), any()) } just Runs
                coEvery { storage.read(LatestEnrichment) } returns Enrichment.stub()
                coEvery { mockNetwork.getConfig(any()) } coAnswers {
                    delay(1200)
                    Either.Success(newConfig)
                }
                coEvery { mockNetwork.getEnrichment(any(), any(), any()) } coAnswers {
                    delay(1200)
                    Either.Success(Enrichment.stub())
                }

                coEvery {
                    mockDeviceHelper.getEnrichment(any(), any())
                } coAnswers {
                    delay(1200)
                    Either.Success(Enrichment.stub())
                }

                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)
                val mockContainer =
                    spyk(dependencyContainer) {
                        every { deviceHelper } returns mockDeviceHelper
                        every { paywallManager } returns manager
                    }

                val assignmentStore = Assignments(localStorage, mockNetwork, backgroundScope)
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = mockNetwork,
                        paywallManager = mockContainer.paywallManager,
                        storeManager = mockContainer.storeManager,
                        factory = mockContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we fetch the configuration") {
                    configManager.fetchConfiguration()

                    Then("the cached config should be used initially") {
                        coVerify(exactly = 1) { storage.read(LatestConfig) }
                        configManager.configState.first { it is ConfigState.Retrieved }
                        assertEquals("cached", configManager.config?.buildId)
                        advanceUntilIdle()
                    }
                }
            }
        }

    @Test
    fun test_network_delay_without_cached_version() =
        runTest(timeout = 5.minutes) {
            Given("we have no cached config and a delayed network response") {
                coEvery { storage.read(LatestRedemptionResponse) } returns null
                coEvery { storage.read(LatestConfig) } returns null
                coEvery { localStorage.read(LatestRedemptionResponse) } returns null
                coEvery { localStorage.read(LatestEnrichment) } returns null
                coEvery { storage.read(LatestEnrichment) } returns null
                coEvery {
                    mockDeviceHelper.getEnrichment(any(), any())
                } returns Either.Failure(NetworkError.Unknown())
                coEvery {
                    mockNetwork.getEnrichment(any(), any(), any())
                } returns Either.Failure(NetworkError.Unknown())
                coEvery { mockNetwork.getConfig(any()) } coAnswers {
                    delay(1200)
                    Either.Success(Config.stub().copy(buildId = "not"))
                }
                coEvery { mockDeviceHelper.getTemplateDevice() } returns emptyMap()

                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val assignmentStore = Assignments(localStorage, mockNetwork, backgroundScope)
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = mockNetwork,
                        paywallManager = manager,
                        storeManager = storeKit,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we fetch the configuration") {
                    configManager.fetchConfiguration()

                    And("we wait for it to be retrieved") {
                        configManager.configState.first { it is ConfigState.Retrieved }

                        Then("the new config should be fetched exactly once and used") {
                            coVerify(exactly = 1) { mockNetwork.getConfig(any()) }
                            assertEquals("not", configManager.config?.buildId)
                        }
                    }
                }
            }
        }

    @Test
    fun test_network_failure_with_cached_version() =
        runTest(timeout = 5.minutes) {
            Given("we have a cached config and a network failure") {
                val cachedConfig =
                    Config.stub().copy(
                        buildId = "cached",
                        rawFeatureFlags =
                            listOf(
                                RawFeatureFlag("enable_config_refresh_v2", true),
                            ),
                    )
                coEvery { storage.read(LatestRedemptionResponse) } returns null
                coEvery { localStorage.read(LatestRedemptionResponse) } returns null
                coEvery { storage.read(LatestConfig) } returns cachedConfig
                coEvery { mockNetwork.getConfig(any()) } returns Either.Failure(NetworkError.Unknown())
                coEvery { localStorage.read(LatestEnrichment) } returns null
                coEvery { storage.read(LatestEnrichment) } returns null
                coEvery {
                    mockNetwork.getEnrichment(any(), any(), any())
                } returns Either.Failure(NetworkError.Unknown())

                coEvery {
                    mockDeviceHelper.getEnrichment(any(), any())
                } returns Either.Failure(NetworkError.Unknown())

                coEvery { mockDeviceHelper.getTemplateDevice() } returns emptyMap()

                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val assignmentStore = Assignments(localStorage, mockNetwork, backgroundScope)
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = mockNetwork,
                        paywallManager = manager,
                        storeManager = storeKit,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we fetch the configuration") {
                    configManager.fetchConfiguration()

                    Then("the cached config should be used") {
                        configManager.configState.first { it is ConfigState.Retrieved }
                        coEvery { mockNetwork.getConfig(any()) } returns
                            Either.Success(
                                Config.stub().copy(buildId = "not"),
                            )
                        assertEquals("cached", configManager.config?.buildId)

                        And("the network becomes available and we fetch again") {
                            coEvery { mockNetwork.getConfig(any()) } returns
                                Either.Success(
                                    Config.stub().copy(buildId = "not"),
                                )

                            Then("the new config should be set and used") {
                                configManager.configState
                                    .onEach {
                                        println("$it is ${it::class}")
                                    }.drop(1)
                                    .first { it is ConfigState.Retrieved }
                                assertEquals("not", configManager.config?.buildId)
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun test_quick_network_success() =
        runTest {
            Given("we have a quick network response") {
                val newConfig = Config.stub().copy(buildId = "not")
                coEvery { storage.read(LatestRedemptionResponse) } returns null
                coEvery { localStorage.read(LatestRedemptionResponse) } returns null
                coEvery { storage.read(LatestConfig) } returns null
                coEvery { mockNetwork.getConfig(any()) } coAnswers {
                    delay(200)
                    Either.Success(newConfig)
                }

                coEvery { localStorage.read(LatestEnrichment) } returns null
                coEvery { storage.read(LatestEnrichment) } returns null
                coEvery {
                    mockNetwork.getEnrichment(any(), any(), any())
                } returns Either.Success(Enrichment.stub())

                coEvery {
                    mockDeviceHelper.getEnrichment(any(), any())
                } returns Either.Success(Enrichment.stub())

                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val assignmentStore = Assignments(localStorage, mockNetwork, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any()) } just Runs
                        coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                        coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                    }

                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = mockNetwork,
                        paywallManager = manager,
                        storeManager = storeKit,
                        factory = dependencyContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we fetch the configuration") {
                    configManager.fetchConfiguration()

                    Then("the new config should be used immediately") {
                        assertEquals("not", configManager.config?.buildId)
                    }
                }
                return@runTest
            }
        }

    @Test
    fun test_config_and_geo_calls_both_cached() =
        runTest(timeout = 500.seconds) {
            Given("we have cached config and geo info, and delayed network responses") {
                val cachedConfig =
                    Config.stub().copy(
                        buildId = "cached",
                        rawFeatureFlags = listOf(RawFeatureFlag("enable_config_refresh_v2", true)),
                    )
                val newConfig = Config.stub().copy(buildId = "not")
                val cachedGeo = Enrichment.stub().copy()
                val newGeo = Enrichment.stub().copy(_device = JsonObject(mapOf("demandTier" to "gold".convertToJsonElement())))

                coEvery { preload.preloadAllPaywalls(any(), any()) } just Runs
                coEvery { storage.read(LatestRedemptionResponse) } returns null
                coEvery { localStorage.read(LatestRedemptionResponse) } returns null
                coEvery { storage.read(LatestConfig) } returns cachedConfig
                coEvery { storage.read(LatestEnrichment) } returns cachedGeo
                coEvery { storage.write(any(), any()) } just Runs
                coEvery { localStorage.read(LatestEnrichment) } returns cachedGeo
                every { manager.resetPaywallRequestCache() } just Runs
                coEvery { preload.removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                var callCount = 0
                coEvery { mockNetwork.getConfig(any()) } coAnswers {
                    if (callCount == 0) {
                        callCount += 1
                        delay(5000)
                    }
                    Either.Success(newConfig)
                }
                var enrichmentCallCount = 0
                every { mockDeviceHelper.setEnrichment(any()) } just Runs
                coEvery { mockDeviceHelper.getEnrichment(any(), any()) } coAnswers {
                    enrichmentCallCount += 1
                    if (enrichmentCallCount == 1) {
                        delay(5000)
                        Either.Failure(NetworkError.Timeout)
                    } else {
                        delay(100)
                        Either.Success(newGeo)
                    }
                }
                coEvery { mockDeviceHelper.getTemplateDevice() } returns emptyMap()

                val context = InstrumentationRegistry.getInstrumentation().targetContext

                val dependencyContainer =
                    DependencyContainer(context, null, null, activityProvider = null)

                val mockContainer =
                    spyk(dependencyContainer) {
                        every { deviceHelper } returns mockDeviceHelper
                        every { paywallManager } returns manager
                    }

                val assignmentStore = Assignments(localStorage, mockNetwork, backgroundScope)
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = mockNetwork,
                        paywallManager = mockContainer.paywallManager,
                        storeManager = mockContainer.storeManager,
                        factory = mockContainer,
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                When("we fetch the configuration") {
                    configManager.fetchConfiguration()

                    Then("the cached config and geo info should be used initially") {
                        configManager.configState.first { it is ConfigState.Retrieved }.also {
                            assertEquals("cached", it.getConfig()?.buildId)
                        }
                        coEvery { mockNetwork.getConfig(any()) } coAnswers {
                            delay(100)
                            Either.Success(newConfig)
                        }

                        And("we wait until new config is available") {
                            configManager.configState.drop(1).first { it is ConfigState.Retrieved }

                            Then("the new config and geo info should be fetched and used") {
                                assertEquals("not", configManager.config?.buildId)
                                advanceUntilIdle()
                            }
                        }
                    }
                }
            }
        }

    @After
    fun tearDown() {
        clearMocks(dependencyContainer, manager, storage, preload, localStorage, mockNetwork)
    }
}
