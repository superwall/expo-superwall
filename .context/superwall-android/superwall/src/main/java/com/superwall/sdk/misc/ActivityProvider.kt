package com.superwall.sdk.misc

import android.app.Activity

interface ActivityProvider {
    /**
     * Provide an activity for Superwall to use instead of passing it in
     * on configure.
     */
    fun getCurrentActivity(): Activity?
}
