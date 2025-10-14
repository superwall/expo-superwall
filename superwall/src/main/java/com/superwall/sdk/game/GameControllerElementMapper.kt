package com.superwall.sdk.game

import android.view.KeyEvent
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger

object GameControllerElementMapper {
    // Map GCControllerElement to button names
    fun mapToButtonName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2 Button"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1 Button"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2 Button"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1 Button"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "Left Thumbstick"
            KeyEvent.KEYCODE_BUTTON_THUMBL - 1 -> "Left Thumbstick Button"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "Right Thumbstick"
            KeyEvent.KEYCODE_BUTTON_THUMBR - 1 -> "Right Thumbstick Button"
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> "Direction Pad"
            KeyEvent.KEYCODE_BUTTON_A -> "A Button"
            KeyEvent.KEYCODE_BUTTON_B -> "B Button"
            KeyEvent.KEYCODE_BUTTON_X -> "X Button"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y Button"
            // On backbone this is on the left side of the controller
            // so we'll remap it to Options to match iOS
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Options Button"
            // On backbone this is on the right side of the controller
            // so we'll remap it to Menu to match iOS
            KeyEvent.KEYCODE_BUTTON_START -> "Menu Button"
            else -> {
                Logger.debug(LogLevel.debug, LogScope.gameControllerManager, "Unknown button: $keyCode")
                return "Unknown Button"
            }
        }
    }
}
