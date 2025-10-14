package com.superwall.sdk.game

import android.view.KeyEvent
import android.view.MotionEvent

interface GameControllerDelegate {
    fun gameControllerEventOccured(event: GameControllerEvent)
}

class GameControllerManager {
    companion object {
        public var shared = GameControllerManager()
    }

    private var delegate: GameControllerDelegate? = null

    fun setDelegate(delegate: GameControllerDelegate) {
        this.delegate = delegate
    }

    fun clearDelegate(delegate: GameControllerDelegate) {
        if (this.delegate == delegate) {
            this.delegate = null
        }
    }

    private fun valueChanged(
        name: String,
        value: Float,
        x: Float = 0f,
        y: Float = 0f,
        directional: Boolean = false,
    ) {
        val event =
            GameControllerEvent(
                controllerElement = name,
                value = value.toDouble(),
                x = x.toDouble(),
                y = y.toDouble(),
                directional = directional,
            )
        delegate?.gameControllerEventOccured(event)
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val name = GameControllerElementMapper.mapToButtonName(event.keyCode)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            valueChanged(name, 1f)
        } else if (event.action == KeyEvent.ACTION_UP) {
            valueChanged(name, 0f)
        }
        return true
    }

    fun dispatchMotionEvent(event: MotionEvent): Boolean {
        // You will likely want to filter the motion events so that they only happen from the
        // joystick
        // TODO: Filter for joystick ?
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            // Right thumbstick
            // TODO: Filter for joystick ?
            valueChanged("Right Thumbstick", 1f, axisX, axisY, true)
        } else {
            // Not sure when this will happen
        }
        return true
    }
}
