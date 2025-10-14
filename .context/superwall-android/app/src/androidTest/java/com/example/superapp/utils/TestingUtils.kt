package com.example.superapp.utils

import android.util.Log
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.dropbox.dropshots.Dropshots
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.paywall.view.ShimmerView
import com.superwall.superapp.MainActivity
import com.superwall.superapp.test.UITestInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.util.LinkedList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ScreenshotTestFlow(
    val testInfo: UITestInfo,
) {
    var steps: LinkedList<Step> = LinkedList()

    private val device = "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}"

    @ScreenshotTestDSL
    fun step(
        name: String? = null,
        action: suspend CoroutineScope.(UITestInfo) -> Unit,
    ) {
        val stepName = name ?: "step_${steps.size + 1}"
        steps.add(
            Step(stepName) { it, et ->

                val step = "${device}_TestCase_${testInfo.number}${
                    if (stepName.isEmpty()) "" else "_$stepName"
                }"
                Log.e(
                    "Testing Flow",
                    "Executing step $step",
                )

                action(et, it)
                et.delayFor(300.milliseconds)
                Log.e("Testing Flow", "Taking screenshot of $step")
                assertSnapshot(
                    step,
                )
                Log.e("Testing Flow", "Taken screenshot of $step")
            },
        )
    }

    data class Step(
        val name: String,
        val action: suspend Dropshots.(UITestInfo, CoroutineScope) -> Unit,
    )
}

@DslMarker
annotation class ScreenshotTestDSL

@DslMarker
annotation class UiTestDSL

@ScreenshotTestDSL
fun Dropshots.screenshotFlow(
    testInfo: UITestInfo,
    config: FlowTestConfiguration = FlowTestConfiguration(),
    flow: ScreenshotTestFlow.() -> Unit,
) {
    val flow = ScreenshotTestFlow(testInfo).apply(flow)
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    val testCase = testInfo
    println("-----------")
    println("Executing test case: ${testCase.number}")
    println("Description:\n ${testCase.description}")
    println("-----------")
    val testReady = MutableStateFlow(false)
    scenario.moveToState(Lifecycle.State.STARTED)
    val scope = CoroutineScope(Dispatchers.IO)
    scenario.onActivity {
        val ctx = it

        runBlocking {
            scope.launch {
                testReady.first { it }
                testCase.test(ctx)
            }
        }
    }

    runTest(timeout = config.timeout) {
        if (config.waitForConfig) {
            Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
        }
        try {
            flow.steps.forEach {
                if (!testReady.value) {
                    testReady.update { true }
                }
                it.action(this@screenshotFlow, testCase, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.cancel()
            throw e
        } finally {
            scope.cancel()
        }
    }
    closeActivity()
    scenario.close()
}

@ScreenshotTestDSL
fun Dropshots.paywallPresentsFor(
    testInfo: UITestInfo,
    config: FlowTestConfiguration = FlowTestConfiguration(),
) {
    screenshotFlow(testInfo, config) {
        step("") {
            it.waitFor { it is SuperwallEvent.PaywallWebviewLoadComplete }
            // Since there is a delay between webview finishing loading and the actual render
            // We need to wait for the webview to finish loading before taking the snapshot
            awaitUntilShimmerDisappears()
            awaitUntilWebviewAppears()
            delay(1000)
        }
    }
}

@ScreenshotTestDSL
fun Dropshots.paywallDoesntPresentFor(
    testInfo: UITestInfo,
    config: FlowTestConfiguration = FlowTestConfiguration(),
) {
    screenshotFlow(testInfo, config) {
        step("") {
            it.waitFor { it is SuperwallEvent.PaywallPresentationRequest }
            // We delay a bit to ensure the paywall doesn't render after presentation request
            delayFor(1.seconds)
        }
    }
}

@ScreenshotTestDSL
fun Dropshots.paywallDoesntPresentForNoConfig(
    testInfo: UITestInfo,
    config: FlowTestConfiguration = FlowTestConfiguration(false),
) {
    screenshotFlow(testInfo, config) {
        step("") {
            it.waitFor { it is SuperwallEvent.PaywallPresentationRequest }
            // We delay a bit to ensure the paywall doesn't render after presentation request
            delayFor(1.seconds)
        }
    }
}

@UiTestDSL
suspend fun CoroutineScope.awaitUntilShimmerDisappears(): Boolean {
    while (getShimmerFromPaywall()?.visibility == View.VISIBLE) {
        delayFor(100.milliseconds)
    }
    return true
}

private fun getShimmerFromPaywall() =
    Superwall.instance.paywallView
        ?.children
        ?.find { it is ShimmerView }

private fun getWebviewFromPaywall() =
    Superwall.instance.paywallView
        ?.children
        ?.find { it is WebView }

@UiTestDSL
suspend fun awaitUntilWebviewAppears(): Boolean {
    val device = UiDevice.getInstance(getInstrumentation())
    device.wait(Until.findObject(By.clazz(WebView::class.java.name)), 1000)
    device.waitForIdle()
    delay(300.milliseconds)
    return true
}

@UiTestDSL
suspend fun enableWebviewDebugging() {
    UiDevice
        .getInstance(getInstrumentation())
        .findObject(By.clazz(WebView::class.java))
        .apply {
        }
}

@UiTestDSL
suspend fun clickButtonWith(text: String) {
    val selector = UiSelector()
    val device = UiDevice.getInstance(getInstrumentation())
    device
        .findObject(
            UiSelector().textContains(text),
        ).click()
}

@UiTestDSL
suspend fun setInput(text: String) {
    val device = UiDevice.getInstance(getInstrumentation())
    with(
        device.findObject(
            UiSelector().focusable(true),
        ),
    ) {
        this.setText(text)
    }
}

@UiTestDSL
suspend fun goBack() {
    val device = UiDevice.getInstance(getInstrumentation())
    device.pressBack()
}

@UiTestDSL
suspend fun awaitUntilDialogAppears(): Boolean {
    val selector = UiSelector()
    val device = UiDevice.getInstance(getInstrumentation())
    device.wait(Until.findObject(By.clazz(AlertDialog::class.java.name)), 10000)
    return true
}

@UiTestDSL
suspend fun awaitUntilWebviewDisappears() {
    while (getWebviewFromPaywall()?.visibility == View.VISIBLE) {
        delay(100)
    }
    delay(100)
}

@UiTestDSL
suspend fun UITestInfo.waitFor(event: (SuperwallEvent) -> Boolean) {
    events()
        .filterNotNull()
        .first(event)
}

// To close the SuperwallPaywallActivity or MainActivity if no paywall was presented
@UiTestDSL
fun closeActivity() {
    getInstrumentation().runOnMainSync {
        ActivityLifecycleMonitorRegistry
            .getInstance()
            .getActivitiesInStage(Stage.RESUMED)
            .forEach {
                it.finish()
            }
    }
}

suspend fun CoroutineScope.delayFor(duration: Duration) =
    async(Dispatchers.IO) {
        delay(duration)
    }.await()
