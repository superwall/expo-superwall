package com.superwall.sdk.analytics.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.config.models.getConfig
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.dependencies.UserAttributesEventFactory
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.Date

interface AppManagerDelegate {
    suspend fun didUpdateAppSession(appSession: AppSession)
}

//
class AppSessionManager(
    private val configManager: ConfigManager,
    private val storage: LocalStorage,
    private val delegate: Factory,
    private val backgroundScope: IOScope,
) : DefaultLifecycleObserver {
    interface Factory :
        AppManagerDelegate,
        DeviceHelperFactory,
        UserAttributesEventFactory

    var appSessionTimeout: Long? = null

    var appSession = AppSession()
        set(value) {
            field = value
            CoroutineScope(Dispatchers.Main).launch {
                delegate.didUpdateAppSession(value)
            }
        }
    private var lastAppClose: Date? = null
    private var didTrackAppLaunch = false

    init {
        listenForAppSessionTimeout()
    }

    // Called when the app goes to the foreground
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        backgroundScope.launch {
            Superwall.instance.track(InternalSuperwallEvent.AppOpen())
        }
        sessionCouldRefresh()
    }

    // Called when the app goes to the background
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundScope.launch {
            Superwall.instance.track(InternalSuperwallEvent.AppClose())
        }
        lastAppClose = Date()
        appSession.endAt = Date()
    }

    private fun willTerminate() {
        appSession.endAt = Date()
    }

    fun listenForAppSessionTimeout() {
        backgroundScope.launch {
            configManager.configState
                .mapNotNull { it.getConfig() }
                .collect { config ->
                    appSessionTimeout = config.appSessionTimeout
                    // Account for fact that dev may have delayed the init of Superwall
                    // such that applicationDidBecomeActive() doesn't activate.
                    if (!didTrackAppLaunch) {
                        sessionCouldRefresh()
                    }
                }
        }
    }

    private fun sessionCouldRefresh() {
        withErrorTracking {
            detectNewSession()
            trackAppLaunch()
            backgroundScope.launch {
                storage.recordFirstSeenTracked()
            }
        }
    }

    private fun detectNewSession() {
        withErrorTracking {
            val didStartNewSession =
                AppSessionLogic.didStartNewSession(
                    lastAppClose,
                    appSessionTimeout,
                )

            if (didStartNewSession) {
                appSession = AppSession()

                backgroundScope.launch {
                    val deviceAttributes = delegate.makeSessionDeviceAttributes()
                    val userAttributes = delegate.makeUserAttributesEvent()

                    Superwall.instance.track(InternalSuperwallEvent.SessionStart())

                    // Only track device attributes if we've already tracked app launch before.
                    // This is because we track device attributes after the config is first fetched.
                    // Otherwise we'd track it twice and it won't contain geo info here on cold app start.
                    if (didTrackAppLaunch) {
                        Superwall.instance.track(
                            InternalSuperwallEvent.DeviceAttributes(
                                deviceAttributes,
                            ),
                        )
                    }
                    Superwall.instance.track(userAttributes)
                }
                // If we are returning to the app, we can refresh the config here.
                backgroundScope.launch {
                    configManager.refreshConfiguration()
                }
            } else {
                appSession.endAt = null
            }
        }
    }

    //
    private fun trackAppLaunch() {
        if (didTrackAppLaunch) {
            return
        }
        backgroundScope.launch {
            Superwall.instance.track(InternalSuperwallEvent.AppLaunch())
        }
        didTrackAppLaunch = true
    }
}
