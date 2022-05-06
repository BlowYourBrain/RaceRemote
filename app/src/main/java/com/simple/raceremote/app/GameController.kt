package com.simple.raceremote.app

import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent

//todo extract interface
class GameController {

    /***
     * 1f  максимальный поворот вправо
     * 0f  состояние покоя
     * -1f максимальный поворот влево
     */
    var steeringWheelUpdateCallback: ((Float) -> Unit)? = null

    /**
     * 1f  максимальная скорость вперед
     * 0f  состояние покоя
     * -1f максимальная скорость назад
     * */
    var movementUpdateCallback: ((Float) -> Unit)? = null

    fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean = ev?.let { motionEvent ->
        val isJoystickSource =
            motionEvent.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

        if (isJoystickSource && motionEvent.action == MotionEvent.ACTION_MOVE) {
            // Process the movements starting from the
            // earliest historical position in the batch
            (0 until motionEvent.historySize).forEach { i ->
                // Process the event at historical position i
                processJoystickInput(motionEvent, i)
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(motionEvent, -1)
        }

        isJoystickSource
    } ?: false

    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {

        val inputDevice = event.device

        // Calculate the horizontal distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat axis, or the right control stick.
        var x: Float = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        if (x == 0f) {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)
        }
        if (x == 0f) {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos)
        }

        // Calculate the vertical distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat switch, or the right control stick.
        var y: Float = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos)
        if (y == 0f) {
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y, historyPos)
        }
        if (y == 0f) {
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
        }

        // Update the ship object based on the new x and y values
        Log.d("fuck", "x = $x, y = $y")
    }

    private fun getCenteredAxis(
        event: MotionEvent,
        device: InputDevice,
        axis: Int,
        historyPos: Int
    ): Float {
        val range: InputDevice.MotionRange? = device.getMotionRange(axis, event.source)

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        range?.apply {
            val value: Float = if (historyPos < 0) {
                event.getAxisValue(axis)
            } else {
                event.getHistoricalAxisValue(axis, historyPos)
            }

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value
            }
        }
        return 0f
    }
}