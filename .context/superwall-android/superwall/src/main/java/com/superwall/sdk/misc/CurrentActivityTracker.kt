package com.superwall.sdk.misc

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import kotlin.time.Duration

class CurrentActivityTracker :
    Application.ActivityLifecycleCallbacks,
    ActivityProvider {
    private var currentActivity: WeakReference<Activity>? = null
    private val activityState = MutableStateFlow<Activity?>(null)

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        Logger.debug(
            LogLevel.debug,
            LogScope.all,
            "!! onActivityCreated: $activity",
        )
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.debug(
            LogLevel.debug,
            LogScope.all,
            "!! onActivityStarted: $activity",
        )
        currentActivity = WeakReference(activity)
        activityState.value = activity
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.debug(
            LogLevel.debug,
            LogScope.all,
            "!! onActivityResumed: $activity",
        )
        currentActivity = WeakReference(activity)
        activityState.value = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity?.get() === activity) {
            activityState.value = null
        }
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {
        Logger.debug(
            LogLevel.debug,
            LogScope.all,
            "!! onActivityDestroyed: $activity",
        )
        if (currentActivity?.get() === activity) {
            currentActivity = null
            activityState.value = null
        }
    }

    override fun getCurrentActivity(): Activity? {
        Logger.debug(
            LogLevel.debug,
            LogScope.all,
            "!! getCurrentActivity: $currentActivity",
        )
        return currentActivity?.get()
    }

    /**
     * Suspends until an activity becomes available, or returns null on timeout.
     */
    suspend fun awaitActivity(timeout: Duration): Activity? =
        currentActivity?.get() ?: withTimeoutOrNull(timeout) {
            activityState.first { it != null }
        }
}
