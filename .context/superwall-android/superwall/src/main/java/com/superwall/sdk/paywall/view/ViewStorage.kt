package com.superwall.sdk.paywall.view

import android.view.View
import java.util.concurrent.ConcurrentHashMap

interface ViewStorage {
    val views: ConcurrentHashMap<String, View>

    fun storeView(
        key: String,
        view: View,
    ) {
        views[key] = view
    }

    fun removeView(key: String) {
        views.remove(key)
    }

    fun retrieveView(key: String): View? = views.get(key)

    fun all() = views.values.toList()

    fun keys() = views.keys
}
