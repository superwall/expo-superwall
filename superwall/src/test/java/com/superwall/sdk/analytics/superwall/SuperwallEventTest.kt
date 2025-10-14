package com.superwall.sdk.analytics.superwall

import org.junit.Test

class SuperwallEventTest {
    @Test
    fun test_app_install() {
        val event = SuperwallEvent.AppInstall()
        assert(event.backingEvent == SuperwallEvents.AppInstall)
    }
}
