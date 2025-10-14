package com.superwall.sdk.misc

import android.os.Handler
import android.os.Looper

fun runOnUiThread(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action()
    } else {
        Handler(Looper.getMainLooper()).post(action)
    }
}
