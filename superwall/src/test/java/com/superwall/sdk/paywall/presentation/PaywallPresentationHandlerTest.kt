package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallPresentationHandlerTest {
    private val handler = PaywallPresentationHandler()

    @Test
    fun onPresent_invokesStoredBlock() {
        val expected = PaywallInfo.empty()
        var invoked = false

        handler.onPresent {
            invoked = true
            assertSame(expected, it)
        }

        handler.onPresentHandler?.invoke(expected)

        assertTrue(invoked)
    }

    @Test
    fun onDismiss_invokesStoredBlock() {
        val expected = PaywallInfo.empty()
        val result = PaywallResult.Declined()
        var invoked = false

        handler.onDismiss { info, paywallResult ->
            invoked = true
            assertSame(expected, info)
            assertSame(result::class, paywallResult::class)
        }

        handler.onDismissHandler?.invoke(expected, result)

        assertTrue(invoked)
    }

    @Test
    fun onError_invokesStoredBlock() {
        val throwable = IllegalStateException("boom")
        var invoked = false

        handler.onError {
            invoked = true
            assertSame(throwable, it)
        }

        handler.onErrorHandler?.invoke(throwable)

        assertTrue(invoked)
    }

    @Test
    fun onSkip_invokesStoredBlock() {
        val reason = PaywallSkippedReason.NoAudienceMatch()
        var invoked = false

        handler.onSkip {
            invoked = true
            assertSame(reason::class, it::class)
        }

        handler.onSkipHandler?.invoke(reason)

        assertTrue(invoked)
    }
}
