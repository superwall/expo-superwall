@file:Suppress("ktlint:standard:no-empty-file")

import android.app.Application
import android.os.Build
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.ThresholdValidator
import com.example.superapp.utils.CustomComparator
import com.example.superapp.utils.awaitUntilDialogAppears
import com.example.superapp.utils.awaitUntilShimmerDisappears
import com.example.superapp.utils.awaitUntilWebviewAppears
import com.example.superapp.utils.clickButtonWith
import com.example.superapp.utils.delayFor
import com.example.superapp.utils.goBack
import com.example.superapp.utils.screenshotFlow
import com.example.superapp.utils.waitFor
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.Keys
import com.superwall.superapp.test.UITestHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FlowScreenshotTestExecutor {
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
        Superwall.instance.reset()
    }

    val mainScope = CoroutineScope(Dispatchers.Main)

    @Test
    fun test_paywall_reappers_with_video() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test4Info) {
                step("first_paywall") {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(500.milliseconds)
                }
                step("second_paywall") {
                    it.waitFor { it is SuperwallEvent.PaywallOpen }
                    awaitUntilWebviewAppears()
                    delayFor(2.seconds)
                }
            }
        }

    @Test
    fun test_paywall_presents_regardless_of_subscription() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test9Info) {
                step("") {
                    it.waitFor { it is SuperwallEvent.PaywallOpen }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(500.milliseconds)
                    // We scroll a bit to display the button
                    mainScope
                        .async {
                            Superwall.instance.paywallView
                                ?.scrollBy(300) ?: kotlin.run {
                                throw IllegalStateException("No view found")
                            }
                        }.await()
                    // We delay a bit to ensure the button is visible
                    delayFor(100.milliseconds)
                    // We scroll back to the top
                    mainScope
                        .async {
                            Superwall.instance.paywallView
                                ?.scrollTo(0) ?: kotlin.run {
                                throw IllegalStateException("No view found")
                            }
                        }.await()
                    // We delay a bit to ensure scroll has finished
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_reappears_after_dismissing() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test11Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears() || awaitUntilWebviewAppears()
                    delayFor(1.seconds)
                }
                step {
                    it.waitFor { it is SuperwallEvent.PaywallOpen }
                    awaitUntilWebviewAppears()
                    delayFor(1.seconds)
                }
                step {
                    it.waitFor { it is SuperwallEvent.PaywallOpen }
                    awaitUntilWebviewAppears()
                    delayFor(1.seconds)
                }
            }
        }

    @Test
    fun test_paywall_calls_reset_after_present() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test34Info) {
                step("") {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(300.milliseconds)
                }
                step {
                    it.waitFor { it is SuperwallEvent.Reset }
                    delayFor(100.milliseconds)
                }
            }
        }

    @Test
    fun test_invalid_url_doesnt_crash() =
        with(dropshots) {
            screenshotFlow(UITestHandler.test62Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(300.milliseconds)
                }
                step {
                    clickButtonWith("Perform 3")
                    delayFor(300.milliseconds)
                }
            }
        }

    @Test
    fun test_restore_alert_shows() {
        with(dropshots) {
            screenshotFlow(UITestHandler.test63Info) {
                step {
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    clickButtonWith("Restore")
                    awaitUntilDialogAppears()
                }
            }
        }
    }

    @Test
    fun test_ensure_only_one_holdout() {
        with(dropshots) {
            screenshotFlow(UITestHandler.test83Info) {
                step {
                    delayFor(100.milliseconds)
                }
                step {
                    Superwall.instance.register(placement = "holdout_one_time_occurrence")
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(600.milliseconds)
                }
            }
        }
    }

    @Test
    fun test_ensure_only_one_displayed_with_limit() {
        with(dropshots) {
            screenshotFlow(UITestHandler.testAndroid9Info) {
                step {
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(300.milliseconds)
                }
                step {
                    goBack()
                    Superwall.instance.register(placement = "one_time_occurrence")
                    delayFor(300.milliseconds)
                }
            }
        }
    }

    @Test
    fun test_ensure_time_limit_works() {
        with(dropshots) {
            screenshotFlow(UITestHandler.testAndroid18Info) {
                step {
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(300.milliseconds)
                }
                step {
                    goBack()
                    Superwall.instance.register(placement = "once_a_minute")
                    delayFor(300.milliseconds)
                }
                step {
                    Superwall.instance.register(placement = "once_a_minute")
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(300.milliseconds)
                }
            }
        }
    }

    @Test
    fun should_show_baseplan_with_free_trial() =
        with(dropshots) {
            screenshotFlow(UITestHandler.testAndroid21Info) {
                step {
                    it.waitFor {
                        it is SuperwallEvent.PaywallWebviewLoadComplete
                    }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delay(300.milliseconds)
                }
            }
        }

    @Test
    fun should_set_attributes_properly() {
        with(dropshots) {
            val test = UITestHandler.testAndroid23Info
            screenshotFlow(test) {
                step("") {
                    delayFor(300.milliseconds)
                    test.messages().take(2).toList().let {
                        assert(
                            (it.first() as Map<String, Any?>).containsKey("first_name"),
                        )
                        assert(
                            !(it.last() as Map<String, Any?>).containsKey("first_name"),
                        )
                    }
                }
            }
        }
    }
}

/*

Commented out due to inability to re-record tests until Firebase Android Studio plugin is fixed

@Test
fun test_paywall_presents_then_dismisses_without_reappearing() =
    with(dropshots) {
        screenshotFlow(UITestHandler.test14Info) {
            step {
                it.waitFor { it is SuperwallEvent.ShimmerViewComplete }
                awaitUntilShimmerDisappears()
                awaitUntilWebviewAppears()
                delayFor(300.milliseconds)
                mainScope
                    .async {
                        // We scroll a bit to display the button
                        Superwall.instance.paywallView
                            ?.webView
                            ?.apply {
                                // Disable the scrollbar for the test
                                // so its not visible in screenshots
                                isVerticalScrollBarEnabled = false
                                scrollTo(0, 300)
                            }
                    }.await()
                // We delay a bit to ensure the button is visible
                delayFor(300.milliseconds)
                // We scroll back to the top
                mainScope
                    .async {
                        Superwall.instance.paywallView
                            ?.webView
                            ?.apply {
                                scrollTo(0, 0)
                            }
                    }.await()
                // We delay a bit to ensure scroll has finished
                delayFor(500.milliseconds)
            }
        }
    }
}
*/
