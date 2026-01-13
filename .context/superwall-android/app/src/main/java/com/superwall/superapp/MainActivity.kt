package com.superwall.superapp

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.superwall.sdk.Superwall
import com.superwall.superapp.debug.SuperwallDebugActivity
import com.superwall.superapp.test.UITestActivity
import com.superwall.superapp.test.WebTestActivity
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    val events by lazy {
        (applicationContext as MainApplication).events
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MainApplication).activity = WeakReference(this)

        // Setup deep linking handling
        respondToDeepLinks()

        // campaign_trigger
        val campaignTriggerButton: Button = findViewById(R.id.campaignTriggerButton)

        // Attach a click listener to the button
        campaignTriggerButton.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("campaign_trigger")
        }

        // campaign_trigger_v3
        val campaignTriggerV3Button: Button = findViewById(R.id.campaignTriggerV3Button)

        // Attach a click listener to the button
        campaignTriggerV3Button.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("campaign_trigger_v3")
        }

        // another_paywall
        val anotherPaywallButton: Button = findViewById(R.id.anotherPaywallButton)

        // Attach a click listener to the button
        anotherPaywallButton.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("another_paywall")
        }

        // wide_paywall
        val widePaywallButton: Button = findViewById(R.id.widePaywallButton)

        // Attach a click listener to the button
        widePaywallButton.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("wide_paywall")
        }

        // jetpack compose activity
        val jetpackComposeButton: Button = findViewById(R.id.jetpackComposeButton)

        // Attach a click listener to the button
        jetpackComposeButton.setOnClickListener {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }

        // UITest button
        val uiTestButton: Button = findViewById(R.id.uiTest)
        uiTestButton.setOnClickListener {
            val intent = Intent(this, UITestActivity::class.java)
            startActivity(intent)
        }

        val devicePropertiesTestButton: Button = findViewById(R.id.devicePropertiesTest)
        devicePropertiesTestButton.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("device_product_test")
        }

        val webButton = findViewById<Button>(R.id.webTests)
        webButton.setOnClickListener {
            val intent = Intent(this, WebTestActivity::class.java)
            startActivity(intent)
        }
        val backboneTestButton: Button = findViewById(R.id.backboneTest)
        backboneTestButton.setOnClickListener {
            val app = application as? MainApplication
            app?.invokeRegister("game_controller")

            // Wait for 5 seconds then press down, then press up
//            CoroutineScope(Dispatchers.Main).launch {
//                delay(5000)
//                Superwall.instance.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
//                delay(1000)
//                Superwall.instance.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))
//            }
        }

        val superwallDebugButton: Button = findViewById(R.id.superwallDebug)
        superwallDebugButton.setOnClickListener {
            val intent = Intent(this, SuperwallDebugActivity::class.java)
            startActivity(intent)
        }
    }

    /*
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) {
            return super.onKeyDown(keyCode, event)
        }
        Superwall.instance.dispatchKeyEvent(event)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) {
            return super.onKeyUp(keyCode, event)
        }
        Superwall.instance.dispatchKeyEvent(event)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onGenericMotionEvent(event)
        }
        Superwall.instance.dispatchMotionEvent(event)
        return true
    }
     */

    //region Deep Links

    override fun onNewIntent(
        intent: Intent,
        caller: ComponentCaller,
    ) {
        super.onNewIntent(intent, caller)
    }

    private fun respondToDeepLinks() {
        intent?.data?.let { uri ->
            Superwall.handleDeepLink(uri)
        }
    }

    //endregion
}
