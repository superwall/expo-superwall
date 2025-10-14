package com.example.superapp.test

import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.ThresholdValidator
import com.example.superapp.utils.CustomComparator
import org.junit.Rule

class SurveyTests {
    @get:Rule
    val dropshots =
        Dropshots(
            resultValidator = ThresholdValidator(0.01f),
            imageComparator = CustomComparator(),
        )

    /*

    Note: These test are temporarily disabled due to orchestrator flakiness.

    @Before
    fun setup() {
        Superwall.configure(
            getInstrumentation().targetContext.applicationContext as Application,
            Keys.CONSTANT_API_KEY,
            options = SuperwallOptions().apply {
                paywalls.shouldPreload = false
            }
        )
        Superwall.instance.reset()
    }

    @Test
    fun test_survey_response_input() {
        with(dropshots) {
            val test = UITestHandler.test65Info
            screenshotFlow(test) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    goBack()
                }

                step {
                    clickButtonWith("Other")
                    setInput("Test")
                    clickButtonWith("SUBMIT")
                    awaitUntilDialogAppears()
                }
                step {
                    val messages = test.messages()
                        .take(2)
                        .toList()
                    assert(messages.first() is SuperwallEvent.SurveyResponse)
                    assert(messages.last() is SuperwallEvent.PaywallClose)
                }
            }
        }
    }

    @Test
    fun test_survey_with_close_button() {
        with(dropshots) {
            screenshotFlow(UITestHandler.test74Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    goBack()
                    delayFor(100.milliseconds)
                }
                step {
                    // Survey should appear
                    awaitUntilDialogAppears()
                    goBack()
                    delayFor(100.milliseconds)
                }
            }
        }
    }

    @Test
    fun test_survey_shows_once() {
        with(dropshots) {
            Superwall.instance.identify("survey_2")
            screenshotFlow(UITestHandler.test64Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    goBack()
                    delayFor(100.milliseconds)
                }
                step {
                    clickButtonWith("Option")
                    awaitUntilDialogAppears()
                    delayFor(100.milliseconds)
                }
                step {
                    goBack()
                    Superwall.instance.register("show_survey_with_other")
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(100.milliseconds)
                }
                step {
                    goBack()
                    awaitUntilDialogAppears()
                    delayFor(100.milliseconds)
                }
            }
        }
    }


    @Test
    fun test_survey_with_campaign_trigger() {
        Superwall.instance.identify("survey_2")

        with(dropshots) {
            screenshotFlow(UITestHandler.test68Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    goBack()
                    delayFor(100.milliseconds)
                }
                step {
                    clickButtonWith("Option 1")
                    delayFor(100.milliseconds)
                    // Verify second paywall appears
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    delayFor(100.milliseconds)
                }
                step {
                    goBack()
                    awaitUntilDialogAppears()
                }
            }
        }
    }

    @Test
    fun test_survey_with_delegate() {
        with(dropshots) {
            Superwall.instance.identify("survey_3")
            screenshotFlow(UITestHandler.test70Info) {
                step {
                    it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    awaitUntilShimmerDisappears()
                    awaitUntilWebviewAppears()
                    goBack()
                    delayFor(100.milliseconds)
                }
                step {
                    clickButtonWith("Option")
                    delayFor(100.milliseconds)
                }
            }
        }
    }

     */
}
