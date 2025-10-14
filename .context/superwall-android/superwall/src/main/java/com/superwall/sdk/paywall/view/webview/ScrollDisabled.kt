package com.superwall.sdk.paywall.view.webview

import android.view.GestureDetector
import android.view.MotionEvent

class ScrollDisabledListener : GestureDetector.SimpleOnGestureListener() {
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean = true

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean = true
}
