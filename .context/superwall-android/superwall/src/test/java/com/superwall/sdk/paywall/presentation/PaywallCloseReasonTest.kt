package com.superwall.sdk.paywall.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallCloseReasonTest {
    @Test
    fun stateShouldComplete_returnsTrue_forSystemDrivenClosures() {
        assertTrue(PaywallCloseReason.SystemLogic.stateShouldComplete)
        assertTrue(PaywallCloseReason.WebViewFailedToLoad.stateShouldComplete)
        assertTrue(PaywallCloseReason.ManualClose.stateShouldComplete)
    }

    @Test
    fun stateShouldComplete_returnsFalse_whenNextPaywallOrPending() {
        assertFalse(PaywallCloseReason.ForNextPaywall.stateShouldComplete)
        assertFalse(PaywallCloseReason.None.stateShouldComplete)
    }
}
