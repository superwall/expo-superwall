package com.superwall.sdk.analytics.session

import java.util.*

object AppSessionLogic {
    /**
     * Tells you if the session started depending on when the app was last closed and the specified session timeout.
     *
     * @param lastAppClose The date when the app was last closed.
     * @param timeout The timeout for the session, as defined by the config, in milliseconds. By default, this value is 1 hour.
     */
    fun didStartNewSession(
        lastAppClose: Date?,
        timeout: Long? = 3600000L,
    ): Boolean {
        // If the app has never been closed, we've started a new session.
        if (lastAppClose == null) {
            return true
        }

        // Determine the elapsed duration between now and the last app close (in milliseconds).
        val elapsedDuration = System.currentTimeMillis() - lastAppClose.time

        // If it's been longer than the provided session timeout duration, we should consider
        // this the start of a new session.
        return elapsedDuration > (timeout ?: 3600000L)
    }
}
