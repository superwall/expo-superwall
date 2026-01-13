package com.superwall.sdk.config

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.webview.webViewExists
import com.superwall.sdk.storage.LocalStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class PaywallPreloadTest {
    private val testDispatcher = StandardTestDispatcher()

    @After
    fun tearDown() {
        runCatching { unmockkObject(ConfigLogic) }
        runCatching { unmockkStatic("com.superwall.sdk.paywall.view.webview.SWWebViewKt") }
    }

    @Test
    fun `preloadAllPaywalls only schedules one task when already in progress`() =
        runTest(testDispatcher) {
            Given("preloading already running") {
                val factory = mockk<PaywallPreload.Factory>(relaxed = true)
                val storage = mockk<LocalStorage>(relaxed = true)
                val assignments = mockk<Assignments>(relaxed = true)
                val paywallManager = mockk<PaywallManager>(relaxed = true)

                mockkObject(ConfigLogic)
                every { ConfigLogic.filterTriggers(any(), any()) } returns emptySet()
                coEvery {
                    ConfigLogic.getAllActiveTreatmentPaywallIds(any(), any(), any(), any())
                } returns emptySet()

                mockkStatic("com.superwall.sdk.paywall.view.webview.SWWebViewKt")
                every { webViewExists() } returns true

                val preload =
                    PaywallPreload(
                        factory = factory,
                        scope = IOScope(testDispatcher),
                        storage = storage,
                        assignments = assignments,
                        paywallManager = paywallManager,
                        track = {},
                    )

                val config = Config.stub()
                val context = mockk<android.content.Context>()

                When("preloadAllPaywalls is invoked twice before tasks complete") {
                    // First call schedules the job
                    preload.preloadAllPaywalls(config, context)
                    // Second call should short-circuit because currentPreloadingTask is non-null
                    preload.preloadAllPaywalls(config, context)

                    Then("the factory is invoked only once after execution") {
                        testDispatcher.scheduler.advanceUntilIdle()
                        coVerify(exactly = 1) { factory.provideRuleEvaluator(context) }
                    }
                }
            }
        }

    @Test
    fun `removeUnusedPaywallVCsFromCache clears removed and changed paywalls but not presented`() =
        runTest(testDispatcher) {
            Given("old and new configs with removed and changed paywalls") {
                val factory = mockk<PaywallPreload.Factory>(relaxed = true)
                val storage = mockk<LocalStorage>(relaxed = true)
                val assignments = mockk<Assignments>(relaxed = true)
                val paywallManager = mockk<PaywallManager>(relaxed = true)

                val presentedPaywall = Paywall.stub().copy(identifier = "presented", cacheKey = "presented-ck")
                val presentedView =
                    mockk<PaywallView> {
                        every { state } returns PaywallViewState(presentedPaywall, locale = "en")
                    }
                every { paywallManager.currentView } returns presentedView

                val oldConfig =
                    Config.stub().copy(
                        paywalls =
                            listOf(
                                Paywall.stub().copy(identifier = "keep", cacheKey = "keep-ck"),
                                Paywall.stub().copy(identifier = "remove", cacheKey = "remove-ck"),
                                Paywall.stub().copy(identifier = "changed", cacheKey = "old-ck"),
                                presentedPaywall,
                            ),
                    )
                val newConfig =
                    Config.stub().copy(
                        paywalls =
                            listOf(
                                Paywall.stub().copy(identifier = "keep", cacheKey = "keep-ck"),
                                Paywall.stub().copy(identifier = "changed", cacheKey = "new-ck"),
                                presentedPaywall.copy(cacheKey = "updated-presented-ck"),
                            ),
                    )

                val preload =
                    PaywallPreload(
                        factory = factory,
                        scope = IOScope(testDispatcher),
                        storage = storage,
                        assignments = assignments,
                        paywallManager = paywallManager,
                        track = {},
                    )

                When("removeUnusedPaywallVCsFromCache runs") {
                    preload.removeUnusedPaywallVCsFromCache(oldConfig, newConfig)

                    Then("only removed and changed, non-presented paywalls are cleared from cache") {
                        verify { paywallManager.removePaywallView("remove") }
                        verify { paywallManager.removePaywallView("changed") }
                        verify(exactly = 0) { paywallManager.removePaywallView("keep") }
                        verify(exactly = 0) { paywallManager.removePaywallView("presented") }
                    }
                }
            }
        }
}
