package com.superwall.sdk.storage

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.superwall.sdk.Superwall
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

const val CONSTANT_API_KEY = "pk_0ff90006c5c2078e1ce832bd2343ba2f806ca510a0a1696a"

@RunWith(AndroidJUnit4::class)
class ConfigureSDKTest {
    @Test
    fun configure_should_trigger_hasInitialised() =
        runTest(timeout = 5.minutes) {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val superwall = Superwall.configure(appContext.applicationContext as Application, CONSTANT_API_KEY)
            val res = Superwall.hasInitialized.first()
            assertEquals(true, res)
        }
}
