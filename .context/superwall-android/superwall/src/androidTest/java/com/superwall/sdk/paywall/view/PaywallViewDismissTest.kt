package com.superwall.sdk.paywall.view

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.paywall.presentation.PaywallCloseReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class PaywallViewDismissTest {
    private lateinit var app: Application

    @Before
    fun setUp() =
        runBlocking {
            app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
            // Configure Superwall if not already configured
            if (!Superwall.initialized) {
                val options =
                    SuperwallOptions().apply {
                        // Keep startup light for tests
                        paywalls.shouldPreload = false
                        paywalls.useCachedTemplates = false
                    }
                Superwall.configure(
                    applicationContext = app,
                    apiKey = "test_api_key",
                    options = options,
                    activityProvider = null,
                )
            }
        }

    private suspend fun makeView(delegate: PaywallViewDelegateAdapter?): PaywallView =
        withContext(Dispatchers.Main) {
            val paywall = Paywall.stub()
            Superwall.instance.dependencyContainer.makePaywallView(
                paywall = paywall,
                cache = null,
                delegate = delegate,
            )
        }

    private fun makeRequest(): com.superwall.sdk.paywall.presentation.internal.PresentationRequest {
        val dc = Superwall.instance.dependencyContainer
        val info =
            PresentationInfo.ExplicitTrigger(
                EventData(name = "test_event", parameters = emptyMap(), createdAt = Date()),
            )
        return dc.makePresentationRequest(
            presentationInfo = info,
            isPaywallPresented = false,
            type = PresentationRequestType.Presentation,
        )
    }

    @Test
    fun dismiss_purchased_emits_dismissed_and_clears_publisher() =
        runTest {
            var callbackShouldDismiss: Boolean? = null
            val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
            val callback =
                object : PaywallViewCallback {
                    override fun onFinished(
                        paywall: PaywallView,
                        result: PaywallResult,
                        shouldDismiss: Boolean,
                    ) {
                        callbackShouldDismiss = shouldDismiss
                        finished.complete(Unit)
                    }
                }
            val delegate = PaywallViewDelegateAdapter(callback)
            val view = makeView(delegate)

            val publisher = MutableSharedFlow<PaywallState>(replay = 1, extraBufferCapacity = 1)
            val request = makeRequest()
            withContext(Dispatchers.Main) {
                view.set(request, publisher, null)
                view.onViewCreated()
            }

            withContext(Dispatchers.Main) {
                view.dismiss(
                    result = PaywallResult.Purchased(productId = "product1"),
                    closeReason = PaywallCloseReason.SystemLogic,
                )
            }
            // Simulate lifecycle cleanup
            withContext(Dispatchers.Main) { view.beforeOnDestroy() }
            withContext(Dispatchers.Main) { view.destroyed() }

            // Await delegate callback completion
            withContext(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.withTimeout(2000) { finished.await() }
                } catch (_: Throwable) {
                }
            }

            val dismissed = publisher.replayCache.lastOrNull() as? PaywallState.Dismissed
            assertNotNull(dismissed)
            dismissed!!
            assertEquals("product1", (dismissed.paywallResult as PaywallResult.Purchased).productId)
            // Publisher should be cleared when stateShouldComplete is true (SystemLogic)
            assertNull(view.state.paywallStatePublisher)
            // Callback should have been invoked with shouldDismiss = true
            assertEquals(true, callbackShouldDismiss)
        }

    @Test
    fun dismiss_declined_for_next_paywall_does_not_clear_publisher() =
        runTest {
            val callback =
                object : PaywallViewCallback {
                    override fun onFinished(
                        paywall: PaywallView,
                        result: PaywallResult,
                        shouldDismiss: Boolean,
                    ) {
                    }
                }
            val delegate = PaywallViewDelegateAdapter(callback)
            val view = makeView(delegate)

            val publisher = MutableSharedFlow<PaywallState>(replay = 1, extraBufferCapacity = 1)
            val request = makeRequest()
            withContext(Dispatchers.Main) {
                view.set(request, publisher, null)
                view.onViewCreated()
            }

            withContext(Dispatchers.Main) {
                view.dismiss(
                    result = PaywallResult.Declined(),
                    closeReason = PaywallCloseReason.ForNextPaywall,
                )
                view.beforeOnDestroy()
                view.destroyed()
            }

            val dismissed = publisher.replayCache.lastOrNull() as? PaywallState.Dismissed
            assertNotNull(dismissed)
            // Publisher should remain when stateShouldComplete is false (ForNextPaywall)
            assertNotNull(view.state.paywallStatePublisher)
        }
}
