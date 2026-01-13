package com.superwall.sdk.paywall.request

import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.network.Network
import com.superwall.sdk.network.device.DeviceInfo
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaywallRequestManagerTest {
    private lateinit var storeManager: StoreManager
    private lateinit var network: Network
    private lateinit var factory: PaywallRequestManagerDepFactory
    private lateinit var ioScope: IOScope
    private lateinit var requestManager: PaywallRequestManager
    private lateinit var deviceInfo: DeviceInfo
    private val trackedEvents = mutableListOf<TrackableSuperwallEvent>()
    private val globalOverrides = mutableMapOf<String, String>()

    @Before
    fun setup() {
        trackedEvents.clear()
        globalOverrides.clear()

        storeManager = mockk(relaxed = true)
        network = mockk()
        deviceInfo =
            mockk {
                every { locale } returns "en_US"
            }

        factory =
            mockk {
                every { makeDeviceInfo() } returns deviceInfo
                every { makeStaticPaywall(any(), any()) } returns null
                every { activePaywallId() } returns null
            }

        ioScope = IOScope(Dispatchers.Unconfined)

        requestManager =
            PaywallRequestManager(
                storeManager = storeManager,
                network = network,
                factory = factory,
                ioScope = ioScope,
                track = { trackedEvents.add(it) },
                getGlobalOverrides = { globalOverrides },
            )
    }

    @Test
    fun test_getPaywall_returnsPaywall_whenNetworkSucceeds() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            val result = requestManager.getPaywall(request)

            assertTrue(result is Either.Success)
            assertNotNull((result as Either.Success).value)
            assertEquals("test_paywall", result.value.identifier)
        }

    @Test
    fun test_getPaywall_cachesByRequestHash() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            // First call
            val result1 = requestManager.getPaywall(request)
            // Second call should use cache
            val result2 = requestManager.getPaywall(request)

            assertTrue(result1 is Either.Success)
            assertTrue(result2 is Either.Success)
            // Network should only be called once due to caching
            coVerify(exactly = 1) { network.getPaywall(any(), any()) }
        }

    @Test
    fun test_getPaywall_skipsCache_whenDebuggerLaunched() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns true
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            // First call
            requestManager.getPaywall(request)
            // Second call should NOT use cache
            requestManager.getPaywall(request)

            // Network should be called twice since cache is skipped
            coVerify(exactly = 2) { network.getPaywall(any(), any()) }
        }

    @Test
    fun test_getPaywall_tracksEvents() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            requestManager.getPaywall(request)

            // Should track: PaywallLoad.Start, PaywallLoad.Complete, PaywallProductsLoad.Start, PaywallProductsLoad.Complete
            assertTrue(trackedEvents.size >= 2)
        }

    @Test
    fun test_getPaywall_setsExperiment() =
        runTest {
            val experiment = mockk<Experiment>()
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { this@mockk.experiment = any() } just Runs
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall", experiment = experiment)
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            requestManager.getPaywall(request)

            coVerify { paywall.experiment = experiment }
        }

    @Test
    fun test_getPaywall_setsPresentationSourceType() =
        runTest {
            val sourceType = "implicit"
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { presentationSourceType = any() } just Runs
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns sourceType
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            requestManager.getPaywall(request)

            coVerify { paywall.presentationSourceType = sourceType }
        }

    @Test
    fun test_resetCache_clearsPaywallCache() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            // First call to populate cache
            requestManager.getPaywall(request)
            coVerify(exactly = 1) { network.getPaywall(any(), any()) }

            // Reset cache
            requestManager.resetCache()

            // Second call should hit network again since cache was cleared
            requestManager.getPaywall(request)
            coVerify(exactly = 2) { network.getPaywall(any(), any()) }
        }

    @Test
    fun test_getPaywall_usesStaticPaywall_whenAvailable() =
        runTest {
            val staticPaywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "static_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                    every { productItems } returns emptyList()
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "static_paywall")
                    every { eventData } returns null
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { isDebuggerLaunched } returns false
                    every { presentationSourceType } returns null
                }

            every { factory.makeStaticPaywall(any(), any()) } returns staticPaywall
            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns emptyList()
                    every { productsByFullId } returns emptyMap()
                    every { this@mockk.paywall } returns null
                }

            val result = requestManager.getPaywall(request)

            assertTrue(result is Either.Success)
            assertEquals("static_paywall", (result as Either.Success).value.identifier)
            // Network should not be called when static paywall is available
            coVerify(exactly = 0) { network.getPaywall(any(), any()) }
        }

    @Test
    fun test_addProducts_callsStoreManager() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { productItems = any() } just Runs
                    every { productVariables = any() } just Runs
                    every { isFreeTrialAvailable = any() } just Runs
                    every { productsLoadingInfo } returns mockk(relaxed = true)
                }
            val request =
                mockk<PaywallRequest> {
                    every { overrides } returns PaywallRequest.Overrides(products = null, isFreeTrial = null)
                    every { eventData } returns null
                }

            val productItem =
                mockk<ProductItem> {
                    every { name } returns "primary"
                    every { fullProductId } returns "com.example.product1"
                }
            val storeProduct = mockk<StoreProduct>(relaxed = true)

            coEvery { storeManager.getProducts(any(), any(), any()) } returns
                mockk {
                    every { productItems } returns listOf(productItem)
                    every { productsByFullId } returns mapOf("com.example.product1" to storeProduct)
                    every { this@mockk.paywall } returns null
                }

            val result = requestManager.addProducts(paywall, request)

            assertNotNull(result)
            coVerify { storeManager.getProducts(any(), paywall, request) }
        }

    @Test
    fun test_getRawPaywall_success() =
        runTest {
            val paywall =
                mockk<Paywall>(relaxed = true) {
                    every { identifier } returns "test_paywall"
                    every { responseLoadingInfo } returns
                        mockk(relaxed = true) {
                            every { startAt } returns null
                            every { endAt } returns null
                        }
                    every { getInfo(any()) } returns mockk<PaywallInfo>()
                }
            val request =
                mockk<PaywallRequest> {
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                    every { eventData } returns null
                    every { isDebuggerLaunched } returns false
                }

            coEvery { network.getPaywall(any(), any()) } returns Either.Success(paywall)

            val result = requestManager.getRawPaywall(request)

            assertTrue(result is Either.Success)
        }
}
