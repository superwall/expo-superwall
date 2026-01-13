package com.example.superapp.test

import android.app.Application
import android.os.Build
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.ThresholdValidator
import com.example.superapp.utils.CustomComparator
import com.example.superapp.utils.FlowTestConfiguration
import com.example.superapp.utils.awaitUntilDialogAppears
import com.example.superapp.utils.awaitUntilShimmerDisappears
import com.example.superapp.utils.awaitUntilWebviewAppears
import com.example.superapp.utils.delayFor
import com.example.superapp.utils.paywallDoesntPresentFor
import com.example.superapp.utils.paywallPresentsFor
import com.example.superapp.utils.screenshotFlow
import com.example.superapp.utils.waitFor
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.superapp.Keys
import com.superwall.superapp.test.UITestHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SimpleScreenshotTestExecutor {
    @get:Rule
    val dropshots =
        Dropshots(
            resultValidator = ThresholdValidator(0.06f),
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
    fun test_paywall_displays_with_attribute_first() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test0Info)
        }

    @Test
    fun test_paywall_displays_with_attribute_second() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test1Info)
        }

    @Test
    fun test_paywall_displays_without_attribute() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test2Info)
        }

    @Test
    fun test_paywall_displays_with_two_products_alt() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test6Info)
        }

    @Test
    fun test_paywall_doesnt_display() =
        with(dropshots) {
            paywallDoesntPresentFor(UITestHandler.test8Info)
        }

    @Test
    fun test_paywall_doesnt_display_with_trigger_off() =
        with(dropshots) {
            paywallDoesntPresentFor(UITestHandler.test12Info)
        }

    @Test
    fun test_paywall_doesnt_display_with_trigger_not_in_dashboard() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test13Info) {
                step {
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_presents_with_handler_invoked() =
        with(dropshots) {
            val mainScope = CoroutineScope(Dispatchers.Main)
            screenshotFlow(UITestHandler.test16Info) {
                step("") {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears() || awaitUntilWebviewAppears()
                    delayFor(2.seconds)
                    mainScope
                        .async {
                            // We scroll a bit to display the button
                            Superwall.instance.paywallView
                                ?.apply {
                                    // Disable the scrollbar for the test
                                    // so its not visible in screenshots
                                    isVerticalScrollBarEnabled = false
                                    scrollTo(300)
                                }
                        }.await()
                    // We delay a bit to ensure the button is visible
                    delayFor(100.milliseconds)
                    // We scroll back to the top
                    mainScope
                        .async {
                            Superwall.instance.paywallView
                                ?.apply {
                                    scrollTo(0)
                                }
                        }.await()
                    // We delay a bit to ensure scroll has finished
                    delayFor(500.milliseconds)
                }
            }
        }

    @Test
    fun test_paywall_presents_with_browser() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test18Info)
        }

    @Test
    fun test_paywall_presents_with_unsubscribe() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test23Info)
        }

    @Test
    fun test_paywall_doesnt_present_pregated() =
        with(dropshots) {
            paywallDoesntPresentFor(UITestHandler.test24Info)
        }

    @Test
    fun test_paywall_doesnt_present_without_feature_gate() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test25Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallPresentationRequest }
                }
            }
        }

    /*
     * Commented out temporarily since Firebase remote lab connection is broken and recording tests is not possible
     * @Test
     * fun test_paywall_presents_without_showing_alert_after_dismiss() =
     *    with(dropshots) {
     *        screenshotFlow(UITestHandler.test26Info) {
     *            step("") {
     *                it.waitFor { it is SuperwallEvent.PaywallPresentationRequest }
     *                awaitUntilDialogAppears()
     *            }
     *        }
     *    }
     * */

    @Test
    fun test_paywall_doesnt_present_calls_feature_block() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test27Info) {
                step("") {
                    awaitUntilDialogAppears()
                }
            }
        }

    @Test
    fun test_paywall_presents_double_identify_with_same_id() =
        with(dropshots) {
            paywallPresentsFor(UITestHandler.test33Info)
        }

    @Test
    fun test_feature_closure_with_config_not_subscribed_not_gated() =
        runTest {
            with(dropshots) {
                screenshotFlow(UITestHandler.test45Info) {
                    step("") {
                        it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                        awaitUntilShimmerDisappears() || awaitUntilWebviewAppears()
                        delayFor(2.seconds)
                    }
                    step("") {
                        awaitUntilDialogAppears()
                    }
                }
            }
        }

    @Test
    fun test_feature_closure_with_config_not_subscribed_gated() =
        runTest {
            with(dropshots) {
                screenshotFlow(UITestHandler.test46Info, FlowTestConfiguration(true)) {
                    step("") {
                        it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                        awaitUntilShimmerDisappears() || awaitUntilWebviewAppears()
                        delayFor(2.seconds)
                    }
                }
            }
        }

    @Test
    fun test_feature_closure_with_config_subscribed_not_gated() =
        runTest {
            with(dropshots) {
                screenshotFlow(UITestHandler.test47Info, FlowTestConfiguration(false)) {
                    step("") {
                        awaitUntilDialogAppears()
                    }
                }
            }
        }

    @Test
    fun test_feature_closure_with_config_subscribed_gated() =
        runTest {
            with(dropshots) {
                screenshotFlow(UITestHandler.test48Info, FlowTestConfiguration(false)) {
                    step("") {
                        awaitUntilDialogAppears()
                    }
                }
            }
        }
}
