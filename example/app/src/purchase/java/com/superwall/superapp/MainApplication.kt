package com.superwall.superapp

import android.app.Application
import com.superwall.sdk.Superwall
import com.superwall.superapp.Keys

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Superwall.configure(
            this,
            Keys.EXAMPLE_KEY,
        )
    }
}
