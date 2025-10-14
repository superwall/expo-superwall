package com.example.superapp.test

import android.app.Application
import android.os.Build
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.ThresholdValidator
import com.example.superapp.utils.CustomComparator
import com.example.superapp.utils.FlowTestConfiguration
import com.example.superapp.utils.awaitUntilDialogAppears
import com.example.superapp.utils.paywallDoesntPresentForNoConfig
import com.example.superapp.utils.screenshotFlow
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.network.NetworkConsts
import com.superwall.sdk.network.NetworkConsts.retryCount
import com.superwall.superapp.Keys
import com.superwall.superapp.test.UITestHandler
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoConnectionTestExecutor {
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

        getInstrumentation().uiAutomation.executeShellCommand("svc wifi disable")
        getInstrumentation().uiAutomation.executeShellCommand("svc data disable")
    }

    @Test
    fun test_feature_closure_no_config_not_subscribed_not_gated() =
        runTest {
            mockkObject(NetworkConsts) {
                every { retryCount() } returns 0
            }

            with(dropshots) {
                val case = UITestHandler.test41Info
                paywallDoesntPresentForNoConfig(case)
                case.messages().first { it == false }
            }
        }

    @Test
    fun test_feature_closure_no_config_not_subscribed_gated() =
        runTest {
            mockkObject(NetworkConsts) {
                every { retryCount() } returns 0
            }

            with(dropshots) {
                val case = UITestHandler.test42Info
                paywallDoesntPresentForNoConfig(case)
                case.messages().first { it == false }
            }
        }

    @Test
    fun test_feature_closure_no_config_subscribed_not_gated() =
        runTest {
            // Disable network to simulate no config
            mockkObject(NetworkConsts) {
                every { retryCount() } returns 0
            }

            with(dropshots) {
                screenshotFlow(UITestHandler.test43Info, FlowTestConfiguration(false)) {
                    step("") {
                        awaitUntilDialogAppears()
                    }
                }
            }
        }

    @Test
    fun test_feature_closure_no_config_subscribed_gated() =
        runTest {
            mockkObject(NetworkConsts) {
                every { retryCount() } returns 0
            }

            with(dropshots) {
                screenshotFlow(UITestHandler.test44Info, FlowTestConfiguration(false)) {
                    step("") {
                        awaitUntilDialogAppears()
                    }
                }
            }
        }

    @After
    fun after() {
        InstrumentationRegistry
            .getInstrumentation()
            .getUiAutomation()
            .executeShellCommand("svc wifi enable")
        InstrumentationRegistry
            .getInstrumentation()
            .getUiAutomation()
            .executeShellCommand("svc data enable")
    }
}
