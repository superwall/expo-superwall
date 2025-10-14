package com.superwall.sdk.misc

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLifecycleObserver : DefaultLifecycleObserver {
    // Using MutableStateFlow to track the background state
    private val _isInBackground = MutableStateFlow(true)

    // Publicly exposed as a read-only StateFlow
    val isInBackground = _isInBackground.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App enters the foreground
        _isInBackground.value = false
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App enters the background
        _isInBackground.value = true
    }
}
