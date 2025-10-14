package com.superwall.sdk.game

//
//  File.swift
//
//
//  Created by Yusuf Tör on 29/09/2022.
//

import android.view.KeyEvent
import android.view.MotionEvent
import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.runOnUiThread

// / Forwards Game controller events to the paywall.
// /
// / Call this in Gamepad's `valueChanged` function to forward game controller events to the paywall via `paywall.js`.
// /
// / See [Game Controller Support](https://docs.superwall.com/docs/game-controller-support) for more information.
// /
// / - Parameters:
// /   - keyEvent: The key event.
public fun Superwall.dispatchKeyEvent(keyEvent: KeyEvent) {
    runOnUiThread {
        GameControllerManager.shared.dispatchKeyEvent(keyEvent)
    }
}

// / Forwards Game controller events to the paywall.
// /
// / Call this in Gamepad's `valueChanged` function to forward game controller events to the paywall via `paywall.js`.
// /
// / See [Game Controller Support](https://docs.superwall.com/docs/game-controller-support) for more information.
// /
// / - Parameters:
// /   - motionEvent: The motion event.
public fun Superwall.dispatchMotionEvent(motionEvent: MotionEvent) {
    runOnUiThread {
        GameControllerManager.shared.dispatchMotionEvent(motionEvent)
    }
}
