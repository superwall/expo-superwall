package com.superwall.sdk.paywall.manager

import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallIdentifier
import com.superwall.sdk.network.device.DeviceInfo
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.paywall.request.PaywallRequestManager
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaywallManagerTest {
    private lateinit var factory: PaywallManager.Factory
    private lateinit var paywallRequestManager: PaywallRequestManager
    private lateinit var paywallManager: PaywallManager
    private lateinit var cache: PaywallViewCache
    private lateinit var deviceInfo: DeviceInfo

    @Before
    fun setup() {
        cache = mockk(relaxed = true)
        deviceInfo =
            mockk {
                every { locale } returns "en_US"
            }

        factory =
            mockk {
                every { makeCache() } returns cache
                every { makeDeviceInfo() } returns deviceInfo
                every { mainScope() } returns MainScope(Dispatchers.Unconfined)
            }

        paywallRequestManager = mockk()
        paywallManager = PaywallManager(factory, paywallRequestManager)
    }

    @Test
    fun test_currentView_returnsActivePaywallView() {
        val mockView = mockk<PaywallView>()
        every { cache.activePaywallView } returns mockView

        val result = paywallManager.currentView

        assertEquals(mockView, result)
    }

    @Test
    fun test_currentView_returnsNull_whenNoActiveView() {
        every { cache.activePaywallView } returns null

        val result = paywallManager.currentView

        assertEquals(null, result)
    }

    @Test
    fun test_removePaywallView_callsCacheRemove() {
        val identifier: PaywallIdentifier = "test_paywall"
        every { cache.removePaywallView(any()) } just Runs

        paywallManager.removePaywallView(identifier)

        verify { cache.removePaywallView(identifier) }
    }

    @Test
    fun test_resetCache_destroysWebviewsAndClearsCache() =
        runTest {
            val mockView1 = mockk<PaywallView>(relaxed = true)
            val mockView2 = mockk<PaywallView>(relaxed = true)

            every { mockView1.destroyWebview() } just Runs
            every { mockView2.destroyWebview() } just Runs
            every { cache.getAllPaywallViews() } returns listOf(mockView1, mockView2)
            every { cache.entries } returns emptyMap()
            every { cache.activePaywallVcKey } returns null
            every { cache.removeAll() } just Runs

            paywallManager.resetCache()

            verify { mockView1.destroyWebview() }
            verify { mockView2.destroyWebview() }
            verify { cache.removeAll() }
        }

    @Test
    fun test_getPaywallView_success_returnsView() =
        runTest {
            val paywall =
                mockk<Paywall> {
                    every { identifier } returns "test_paywall"
                }
            val request =
                mockk<PaywallRequest> {
                    every { isDebuggerLaunched } returns false
                    every { responseIdentifiers } returns ResponseIdentifiers(paywallId = "test_paywall")
                }
            val mockView =
                mockk<PaywallView> {
                    every { loadingState } returns PaywallLoadingState.Unknown
                    every { loadWebView() } just Runs
                }

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Success(paywall)
            every { cache.getPaywallView(any()) } returns null
            coEvery { factory.makePaywallView(any(), any(), any()) } returns mockView
            every { cache.save(any(), any()) } just Runs

            val result = paywallManager.getPaywallView(request, true, false, null)

            assertTrue(result is Either.Success)
            assertEquals(mockView, (result as Either.Success).value)
            verify { cache.save(mockView, "test_paywall") }
        }

    @Test
    fun test_getPaywallView_returnsCachedView_whenAvailable() =
        runTest {
            val paywall =
                mockk<Paywall> {
                    every { identifier } returns "test_paywall"
                }
            val request =
                mockk<PaywallRequest> {
                    every { isDebuggerLaunched } returns false
                }
            val mockView =
                mockk<PaywallView>(relaxed = true) {
                    every { loadingState } returns PaywallLoadingState.Unknown
                }
            val delegate = mockk<PaywallViewDelegateAdapter>()

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Success(paywall)
            every { cache.getPaywallView(any()) } returns mockView
            every { mockView.callback = any() } just Runs
            every { mockView.updateState(any()) } just Runs

            val result = paywallManager.getPaywallView(request, false, false, delegate)

            assertTrue(result is Either.Success)
            assertEquals(mockView, (result as Either.Success).value)
            verify { mockView.callback = delegate }
            verify { mockView.updateState(any()) }
        }

    @Test
    fun test_getPaywallView_skipsCache_whenDebuggerLaunched() =
        runTest {
            val paywall =
                mockk<Paywall> {
                    every { identifier } returns "test_paywall"
                }
            val request =
                mockk<PaywallRequest> {
                    every { isDebuggerLaunched } returns true
                }
            val mockView =
                mockk<PaywallView> {
                    every { loadingState } returns PaywallLoadingState.Unknown
                    every { loadWebView() } just Runs
                }

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Success(paywall)
            coEvery { factory.makePaywallView(any(), any(), any()) } returns mockView
            every { cache.save(any(), any()) } just Runs

            val result = paywallManager.getPaywallView(request, true, false, null)

            assertTrue(result is Either.Success)
            verify(exactly = 0) { cache.getPaywallView(any()) }
        }

    @Test
    fun test_getPaywallView_loadsWebView_whenPresentingAndUnknownState() =
        runTest {
            val paywall =
                mockk<Paywall> {
                    every { identifier } returns "test_paywall"
                }
            val request =
                mockk<PaywallRequest> {
                    every { isDebuggerLaunched } returns false
                }
            val mockView =
                mockk<PaywallView> {
                    every { loadingState } returns PaywallLoadingState.Unknown
                    every { loadWebView() } just Runs
                }

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Success(paywall)
            every { cache.getPaywallView(any()) } returns null
            coEvery { factory.makePaywallView(any(), any(), any()) } returns mockView
            every { cache.save(any(), any()) } just Runs

            paywallManager.getPaywallView(request, isForPresentation = true, isPreloading = false, null)

            verify { mockView.loadWebView() }
        }

    @Test
    fun test_getPaywallView_doesNotLoadWebView_whenNotForPresentation() =
        runTest {
            val paywall =
                mockk<Paywall> {
                    every { identifier } returns "test_paywall"
                }
            val request =
                mockk<PaywallRequest> {
                    every { isDebuggerLaunched } returns false
                }
            val mockView =
                mockk<PaywallView> {
                    every { loadingState } returns PaywallLoadingState.Unknown
                }

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Success(paywall)
            every { cache.getPaywallView(any()) } returns null
            coEvery { factory.makePaywallView(any(), any(), any()) } returns mockView
            every { cache.save(any(), any()) } just Runs

            paywallManager.getPaywallView(request, isForPresentation = false, isPreloading = false, null)

            verify(exactly = 0) { mockView.loadWebView() }
        }

    @Test
    fun test_getPaywallView_failure_returnsError() =
        runTest {
            val error = RuntimeException("Network error")
            val request = mockk<PaywallRequest>()

            coEvery { paywallRequestManager.getPaywall(any(), any()) } returns Either.Failure(error)

            val result = paywallManager.getPaywallView(request, true, false, null)

            assertTrue(result is Either.Failure)
            assertEquals(error, (result as Either.Failure).error)
        }

    @Test
    fun test_resetPaywallRequestCache_callsRequestManagerReset() {
        every { paywallRequestManager.resetCache() } just Runs

        paywallManager.resetPaywallRequestCache()

        verify { paywallRequestManager.resetCache() }
    }

    @Test
    fun test_cacheIsCreatedLazily() {
        val manager = PaywallManager(factory, paywallRequestManager)

        // Access currentView which triggers cache creation
        manager.currentView

        verify(exactly = 1) { factory.makeCache() }
    }

    @Test
    fun test_cacheIsCreatedOnlyOnce() {
        val manager = PaywallManager(factory, paywallRequestManager)

        // Access currentView multiple times
        manager.currentView
        manager.currentView
        manager.currentView

        // Cache should only be created once
        verify(exactly = 1) { factory.makeCache() }
    }
}
