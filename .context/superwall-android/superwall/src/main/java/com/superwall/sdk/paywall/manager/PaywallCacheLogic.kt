package com.superwall.sdk.paywall.manager

object PaywallCacheLogic {
    fun key(
        identifier: String,
        locale: String,
    ): String = "${identifier}_$locale"
}
