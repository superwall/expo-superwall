package com.superwall.sdk.paywall.request

import Given
import Then
import When
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallIdentifier
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.network.Network
import com.superwall.sdk.network.device.DeviceInfo
import com.superwall.sdk.store.GetProductsResponse
import com.superwall.sdk.store.StoreManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.coroutines.CoroutineContext

class PaywallRequestManagerTest {
    private lateinit var storeManager: StoreManager
    private lateinit var network: Network
    private lateinit var factory: PaywallRequestManagerDepFactory
    private lateinit var ioScope: IOScope
    private lateinit var paywallRequestManager: PaywallRequestManager

    @Before
    fun setup() {
        storeManager =
            mockk {
                coEvery { getProducts(any(), any(), any()) } returns
                    GetProductsResponse(
                        emptyMap(),
                        emptyList(),
                    )
            }
        network = mockk()
        factory =
            mockk {
                every { makeStaticPaywall(any(), any()) } answers {
                    Paywall.stub().copy(identifier = it.invocation.args[0] as PaywallIdentifier)
                }
            }
        ioScope = mockk()
    }

    fun manager(ctx: CoroutineContext) {
        paywallRequestManager =
            PaywallRequestManager(
                storeManager = storeManager,
                network = network,
                factory = factory,
                ioScope = IOScope(ctx),
                track = {},
                getGlobalOverrides = { emptyMap() },
            )
    }

    @Test
    fun test_get_paywall_with_new_request() =
        runTest {
            Given("a new paywall request") {
                manager(this@runTest.coroutineContext)
                val request =
                    PaywallRequest(
                        responseIdentifiers =
                            ResponseIdentifiers(
                                paywallId = "test_paywall",
                                experiment = null,
                            ),
                        eventData = EventData(name = "test", parameters = emptyMap(), createdAt = Date()),
                        presentationSourceType = "test_source",
                        isDebuggerLaunched = false,
                        overrides = PaywallRequest.Overrides(null, null),
                        retryCount = 1,
                    )
                val expectedPaywall = Paywall.stub().copy(identifier = "test_paywall")

                coEvery { network.getPaywall(any(), any()) } returns Either.Success(expectedPaywall)
                coEvery { factory.makeDeviceInfo() } returns DeviceInfo("123", "en_US")

                coEvery { factory.activePaywallId() } returns null

                When("getting the paywall") {
                    val result = paywallRequestManager.getPaywall(request)

                    Then("it should return the paywall successfully") {
                        assert(result is Either.Success)
                        val paywall = (result as Either.Success).value
                        assert(paywall.identifier == expectedPaywall.identifier)
                        assert(paywall.experiment == expectedPaywall.experiment)
                    }
                }
            }
        }

    @Test
    fun test_get_paywall_with_cached_request() =
        runTest {
            manager(this@runTest.coroutineContext)
            Given("a paywall request that has been cached") {
                val request =
                    PaywallRequest(
                        responseIdentifiers =
                            ResponseIdentifiers(
                                paywallId = "test_paywall",
                                experiment = null,
                            ),
                        eventData = EventData(name = "test", parameters = emptyMap(), createdAt = Date()),
                        presentationSourceType = "test_source",
                        isDebuggerLaunched = false,
                        overrides = PaywallRequest.Overrides(null, null),
                        retryCount = 1,
                    )
                val cachedPaywall =
                    Paywall.stub().copy(
                        identifier = "test_paywall",
                        experiment = null,
                        presentationSourceType = "test_source",
                    )

                // First request to cache the paywall
                coEvery { network.getPaywall(any(), any()) } returns Either.Success(cachedPaywall)
                coEvery { factory.makeDeviceInfo() } returns DeviceInfo("123", "en_US")

                coEvery { factory.activePaywallId() } returns null

                // Make first request to cache the paywall
                paywallRequestManager.getPaywall(request)

                When("getting the paywall again with the same request") {
                    val result = paywallRequestManager.getPaywall(request)

                    Then("it should return the cached paywall without making a network request") {
                        assert(result is Either.Success)
                        val paywall = (result as Either.Success).value
                        assert(paywall.identifier == cachedPaywall.identifier)
                        assert(paywall.experiment == cachedPaywall.experiment)
                        assert(paywall.presentationSourceType == cachedPaywall.presentationSourceType)
                    }
                }
            }
        }

    @Test
    fun test_concurrent_requests_for_same_paywall() =
        runTest {
            manager(this@runTest.coroutineContext)
            Given("multiple concurrent requests for the same paywall") {
                val request =
                    PaywallRequest(
                        responseIdentifiers =
                            ResponseIdentifiers(
                                paywallId = "test_paywall",
                                experiment = null,
                            ),
                        eventData = EventData(name = "test", parameters = emptyMap(), createdAt = Date()),
                        presentationSourceType = "test_source",
                        isDebuggerLaunched = false,
                        overrides = PaywallRequest.Overrides(null, null),
                        retryCount = 1,
                    )
                val expectedPaywall =
                    Paywall.stub().copy(
                        identifier = "test_paywall",
                        experiment = null,
                        presentationSourceType = "test_source",
                    )

                coEvery { network.getPaywall(any(), any()) } returns Either.Success(expectedPaywall)
                coEvery { factory.makeDeviceInfo() } returns DeviceInfo("123", "en_US")

                coEvery { factory.activePaywallId() } returns null

                When("making multiple concurrent requests") {
                    val results =
                        List(3) {
                            async {
                                paywallRequestManager.getPaywall(request)
                            }
                        }.map { it.await() }

                    Then("all requests should return the same paywall") {
                        results.forEach { result ->
                            assert(result is Either.Success)
                            val paywall = (result as Either.Success).value
                            assert(paywall.identifier == expectedPaywall.identifier)
                            assert(paywall.experiment == expectedPaywall.experiment)
                            assert(paywall.presentationSourceType == expectedPaywall.presentationSourceType)
                        }
                    }
                }
            }
        }

    @Test
    fun test_preload_active_paywall() =
        runTest {
            manager(this@runTest.coroutineContext)
            Given("a preload request for an active paywall") {
                val paywallId = "test_paywall"
                val originalExperiment =
                    Experiment(
                        "original_experiment",
                        "original_group",
                        Experiment.Variant(
                            "original_variant",
                            Experiment.Variant.VariantType.HOLDOUT,
                            paywallId = paywallId,
                        ),
                    )
                // First request to get the original paywall
                val originalRequest =
                    PaywallRequest(
                        responseIdentifiers =
                            ResponseIdentifiers(
                                paywallId = paywallId,
                                experiment = originalExperiment,
                            ),
                        eventData = EventData(name = "test", parameters = emptyMap(), createdAt = Date()),
                        presentationSourceType = "test_source",
                        isDebuggerLaunched = false,
                        overrides = PaywallRequest.Overrides(null, null),
                        retryCount = 1,
                    )
                val originalPaywall =
                    Paywall.stub().copy(
                        identifier = paywallId,
                        experiment = originalExperiment,
                        presentationSourceType = "test_source",
                    )

                // Preload request with different experiment
                val preloadRequest =
                    PaywallRequest(
                        responseIdentifiers =
                            ResponseIdentifiers(
                                paywallId = paywallId,
                                experiment = null,
                            ),
                        eventData = EventData(name = "test", parameters = emptyMap(), createdAt = Date()),
                        presentationSourceType = "test_source",
                        isDebuggerLaunched = false,
                        overrides = PaywallRequest.Overrides(null, null),
                        retryCount = 1,
                    )

                coEvery { network.getPaywall(any(), any()) } returns
                    Either.Success(originalPaywall)

                coEvery { factory.makeDeviceInfo() } returns DeviceInfo("123", "en_US")

                When("getting the original paywall should not override values") {
                    // First get the original paywall
                    val originalResult = paywallRequestManager.getPaywall(originalRequest)
                    originalResult.getThrowable()?.printStackTrace()
                    assert(originalResult is Either.Success)
                    val firstPaywall = (originalResult as Either.Success).value
                    assert(firstPaywall.experiment == originalExperiment)

                    every { factory.activePaywallId() } returns paywallId
                    // Then try to preload it
                    val preloadResult =
                        paywallRequestManager.getPaywall(preloadRequest, isPreloading = true)
                    assert(preloadResult is Either.Success)
                    val preloadedPaywall = (preloadResult as Either.Success).value

                    Then("the preloaded paywall should maintain the original experiment") {
                        assert(preloadedPaywall.identifier == paywallId)
                        println(preloadedPaywall.experiment)
                        assert(preloadedPaywall.experiment == originalExperiment)
                        assert(preloadedPaywall.presentationSourceType == "test_source")
                    }
                }
            }
        }
}
