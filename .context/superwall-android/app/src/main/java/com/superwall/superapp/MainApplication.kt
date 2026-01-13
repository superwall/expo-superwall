package com.superwall.superapp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.purchase.RevenueCatPurchaseController
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.ref.WeakReference

object Keys {
    const val CONSTANT_API_KEY = "pk_0ff90006c5c2078e1ce832bd2343ba2f806ca510a0a1696a"
    const val ANDROID_MAIN_SCREEN_API_KEY = "pk_d1f0959f70c761b1d55bb774a03e22b2b6ed290ce6561f85"
    const val UI_TEST_API_KEY = "pk_0ff90006c5c2078e1ce832bd2343ba2f806ca510a0a1696a"
    const val DEEP_LINK_OPEN_API_KEY = "pk_3faea4c721179218a245475ea9d378d1ecb9bf059411a0c0"
    const val APP_LAUNCH_API_KEY = "pk_fb295f846b075fae6619eebb43d126ecddd1e3b18e7028b8"
    const val APP_INSTALL_API_KEY = "pk_8db958db59cc8460969659822351d5e177d8d65cb295cff2"
    const val SESSION_START_API_KEY = "pk_6c881299e2f8db59f697646e399397be76432fa0968ca254"
    const val PAYWALL_DECLINE_API_KEY = "pk_6892bf95fdf329f01b7da4d4b77fcc6c1033d1ec3ef31da2"
    const val TRANSACTION_ABANDON_API_KEY = "pk_f406422339b71cf568ffe8cba02f849ab27e9791bb9b2ed4"
    const val TRANSACTION_FAIL_API_KEY = "pk_b6cd945401435766da627080a3fbe349adb2dcd69ab767f3"
    const val SURVEY_RESPONSE_API_KEY = "pk_3698d9fe123f1e4aa8014ceca111096ca06fd68d31d9e662"
    const val WEB_2_APP_API_KEY = "pk_c6190cdd41b924c020e3b88deb2755d51f68dff0b9c8a3a6"
}

class MainApplication :
    android.app.Application(),
    SuperwallDelegate {
    var activity: WeakReference<Activity>? = null

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        StrictMode.setThreadPolicy(
            ThreadPolicy
                .Builder()
                .detectCustomSlowCalls()
                .detectResourceMismatches()
                .detectUnbufferedIo() // or .detectAll() for all detectable problems
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            VmPolicy
                .Builder()
                .detectLeakedSqlLiteObjects()
                .detectActivityLeaks()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .penaltyDeath()
                .build(),
        )

        if (!isRunningTest()) {
            configureWithAutomaticInitialization()
        }
//        configureWithRevenueCatInitialization()
    }

    val events = MutableSharedFlow<SuperwallEventInfo>()

    fun configureWithAutomaticInitialization() {
        Superwall.configure(
            this,
            Keys.CONSTANT_API_KEY,
            options =
                SuperwallOptions().apply {
                    logging.level = LogLevel.debug
                    paywalls =
                        PaywallOptions().apply {
                            shouldPreload = false
                        }
                },
        )
        Superwall.instance.delegate = this

        // Make sure we enable the game controller
        // Superwall.instance.options.isGameControllerEnabled = true
    }

    fun configureWithObserverMode() {
        Superwall.configure(
            this@MainApplication,
            Keys.WEB_2_APP_API_KEY,
            options =
                SuperwallOptions().apply {
                    paywalls =
                        PaywallOptions().apply {
                            shouldPreload = true
                        }
                },
        )
        Superwall.instance.delegate = this@MainApplication
        // Make sure we enable the game controller
        // Superwall.instance.options.isGameControllerEnabled = true
    }

    fun configureWithRevenueCatInitialization() {
        val purchaseController = RevenueCatPurchaseController(this)

        Superwall.configure(
            this,
            Keys.CONSTANT_API_KEY,
            purchaseController,
        )
        Superwall.instance.delegate = this

        // Make sure we enable the game controller
        // Superwall.instance.options.isGameControllerEnabled = true

        purchaseController.syncSubscriptionStatus()
    }

    fun invokeRegister(
        event: String = "campaign_trigger",
        params: Map<String, Any>? = null,
    ) {
        Superwall.instance.register(event, params)
    }

    override fun handleLog(
        level: String,
        scope: String,
        message: String?,
        info: Map<String, Any>?,
        error: Throwable?,
    ) {
        val ctx = activity?.get() ?: return
        if (level == LogLevel.error.toString() &&
            scope == LogScope.productsManager.toString()
        ) {
            AlertDialog
                .Builder(ctx)
                .apply {
                    setTitle("Error")
                    setMessage(message)
                    setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                }.show()
        }
        super.handleLog(level, scope, message, info, error)
    }

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        println(
            "\n!! SuperwallDelegate !! \n" +
                "\tEvent name:" + eventInfo.event.rawName + "" +
                ",\n\tParams:" + eventInfo.params + "\n",
        )
    }

    override fun subscriptionStatusDidChange(
        from: SubscriptionStatus,
        to: SubscriptionStatus,
    ) {
        Log.e("Redeemed", "Status changing from: $from")
        Log.e("Redeemed", "Status changing to: $from")
        super.subscriptionStatusDidChange(from, to)
    }
}

@Synchronized
fun isRunningTest(): Boolean =
    try {
        // "android.support.test.espresso.Espresso" if you haven't migrated to androidx yet
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
