package com.superwall.sdk.paywall.view

import android.view.View
import androidx.lifecycle.ViewModel
import java.util.concurrent.ConcurrentHashMap

/*
* Stores already loaded or preloaded paywalls
* */
class ViewStorageViewModel :
    ViewModel(),
    ViewStorage {
    override val views = ConcurrentHashMap<String, View>()
}
