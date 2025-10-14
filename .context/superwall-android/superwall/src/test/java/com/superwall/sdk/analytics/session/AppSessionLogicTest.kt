package com.superwall.sdk.analytics.session

import com.superwall.sdk.assertFalse
import com.superwall.sdk.assertTrue
import org.junit.Test
import java.util.*

class AppSessionLogicTests {
    @Test
    fun `testDidStartNewSession noTimeout`() {
        val threeHoursAgo = Date(System.currentTimeMillis() - 10800 * 1000)
        val sessionDidStart = AppSessionLogic.didStartNewSession(threeHoursAgo)
        assertTrue(sessionDidStart)
    }

    @Test
    fun `testDidStartNewSession noTimeout lastAppClosedFiftyMinsAgo`() {
        val fiftyMinsAgo = Date(System.currentTimeMillis() - 3000 * 1000)
        val sessionDidStart = AppSessionLogic.didStartNewSession(fiftyMinsAgo)
        assertFalse(sessionDidStart)
    }

    @Test
    fun `testDidStartNewSession freshAppOpen`() {
        val timeout = 3600000L
        val sessionDidStart = AppSessionLogic.didStartNewSession(null, timeout)
        assertTrue(sessionDidStart)
    }

    @Test
    fun `testDidStartNewSession lastAppClosedThirtyMinsAgo`() {
        val thirtyMinsAgo = Date(System.currentTimeMillis() - 1800 * 1000)
        val timeout = 3600000L
        val sessionDidStart = AppSessionLogic.didStartNewSession(thirtyMinsAgo, timeout)
        assertFalse(sessionDidStart)
    }

    @Test
    fun `testDidStartNewSession lastAppClosedThreeHoursAgo`() {
        val threeHoursAgo = Date(System.currentTimeMillis() - 10800 * 1000)
        val timeout = 3600000L
        val sessionDidStart = AppSessionLogic.didStartNewSession(threeHoursAgo, timeout)
        assertTrue(sessionDidStart)
    }
}
