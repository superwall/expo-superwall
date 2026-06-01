package com.superwall.sdk.config

import And
import Given
import Then
import When
import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.Tier
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.primitives.SequentialActor
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.VariantOption
import com.superwall.sdk.network.NetworkMock
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.storage.CONSTANT_API_KEY
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StorageMock
import com.superwall.sdk.storage.StoredEntitlementsByProductId
import com.superwall.sdk.storage.StoredSubscriptionStatus
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.web.WebPaywallRedeemer
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Storage round-trip integration tests. Pure logic lives in src/test/.
open class ConfigManagerUnderTest(
    context: Context,
    storage: Storage,
    network: SuperwallAPI,
    paywallManager: PaywallManager,
    storeManager: StoreManager,
    factory: Factory,
    deviceHelper: DeviceHelper,
    assignments: Assignments,
    paywallPreload: PaywallPreload,
    ioScope: CoroutineScope,
    private val testOptions: SuperwallOptions = SuperwallOptions(),
    testEntitlements: Entitlements =
        Entitlements(
            mockk<Storage>(relaxUnitFun = true) {
                every { read(StoredSubscriptionStatus) } returns SubscriptionStatus.Unknown
                every { read(StoredEntitlementsByProductId) } returns emptyMap()
                every { read(LatestRedemptionResponse) } returns null
            },
        ),
    webRedeemer: WebPaywallRedeemer = mockk(relaxed = true),
) : ConfigManager(
        context = context,
        storage = storage,
        network = network,
        paywallManager = paywallManager,
        storeManager = storeManager,
        factory = factory,
        deviceHelper = deviceHelper,
        options = testOptions,
        assignments = assignments,
        paywallPreload = paywallPreload,
        ioScope = IOScope(ioScope.coroutineContext),
        tracker = {},
        entitlements = testEntitlements,
        webPaywallRedeemer = { webRedeemer },
        actor = SequentialActor(ConfigState.None, IOScope(ioScope.coroutineContext)),
    ) {
    fun setConfig(config: Config) {
        applyRetrievedConfigForTesting(config)
    }
}

@RunWith(AndroidJUnit4::class)
class ConfigManagerTests {
    private val mockDeviceHelper =
        mockk<DeviceHelper> {
            every { appVersion } returns "1.0"
            every { locale } returns "en-US"
            every { deviceTier } returns Tier.MID
            every { bundleId } returns "com.test"
            every { setEnrichment(any()) } just Runs
            coEvery { getTemplateDevice() } returns emptyMap()
            coEvery { getEnrichment(any(), any()) } returns Either.Success(Enrichment.stub())
            coEvery { preloadFingerprint() } returns "stub-fingerprint"
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
                    DependencyContainer(context, null, null, activityProvider = null, apiKey = "")
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignments = Assignments(storage, network, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any(), any()) } just Runs
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
    fun test_loadAssignments_saveAssignmentsFromServer() =
        runTest(timeout = 30.seconds) {
            Given("we have a ConfigManager with assignments from the server") {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val network = NetworkMock()
                val storage = StorageMock(context = context, coroutineScope = backgroundScope)
                val assignmentStore = Assignments(storage, network, backgroundScope)
                val preload =
                    mockk<PaywallPreload> {
                        coEvery { preloadAllPaywalls(any(), any(), any()) } just Runs
                        coEvery { preloadPaywallsByNames(any(), any()) } just Runs
                        coEvery { removeUnusedPaywallVCsFromCache(any(), any()) } just Runs
                    }
                val configManager =
                    ConfigManagerUnderTest(
                        context = context,
                        storage = storage,
                        network = network,
                        paywallManager = mockk(relaxed = true),
                        storeManager = mockk(relaxed = true),
                        factory = mockk(relaxed = true),
                        deviceHelper = mockDeviceHelper,
                        assignments = assignmentStore,
                        paywallPreload = preload,
                        ioScope = backgroundScope,
                    )

                val variantId = "variantId"
                val experimentId = "experimentId"
                val assignments: List<Assignment> =
                    listOf(Assignment(experimentId = experimentId, variantId = variantId))
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
                    advanceUntilIdle()
                }

                Then("the assignments should be stored correctly") {
                    assertEquals(
                        storage.getConfirmedAssignments()[experimentId],
                        variantOption.toVariant(),
                    )
                    assertTrue(configManager.unconfirmedAssignments.isEmpty())
                }
            }
        }
}
