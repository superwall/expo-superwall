package com.example.superapp.test

import android.app.Application
import android.os.Build
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.ThresholdValidator
import com.example.superapp.utils.CustomComparator
import com.example.superapp.utils.delayFor
import com.example.superapp.utils.screenshotFlow
import com.example.superapp.utils.waitFor
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.superapp.Keys
import com.superwall.superapp.test.UITestHandler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PresentationRuleTests {
    @get:Rule
    val dropshots =
        Dropshots(
            resultValidator = ThresholdValidator(0.01f),
            imageComparator = CustomComparator(),
        )

    @Before
    fun grantPhonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().uiAutomation.executeShellCommand(
                (
                    "pm grant " + getTargetContext().packageName +
                        " android.permission.WRITE_EXTERNAL_STORAGE"
                ),
            )
        }
    }

    @Before
    fun setup() {
        Superwall.configure(
            getInstrumentation().targetContext.applicationContext as Application,
            Keys.CONSTANT_API_KEY,
            options =
                SuperwallOptions().apply {
                    paywalls.shouldPreload = false
                },
        )
    }

    @Test
    fun test_paywall_doesnt_present_result_experiment() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test28Info) {
                step("") {
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_doesnt_present_result_no_rule_match() =
        with(dropshots) {
            Superwall.instance.reset()
            screenshotFlow(UITestHandler.test29Info) {
                step("") {
                    it.waitFor { it is SuperwallEvent.UserAttributes }
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_doesnt_present_result_event_not_found() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test30Info) {
                step("") {
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_doesnt_present_result_holdout() =
        with(dropshots) {
            Superwall.instance.reset()
            screenshotFlow(UITestHandler.test31Info) {
                step("") {
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_doesnt_present_result_subscription_change() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test32Info) {
                step("") {
                    it.waitFor { it is SuperwallEvent.SubscriptionStatusDidChange }
                    delayFor(1.seconds)
                }
            }
        }
}
