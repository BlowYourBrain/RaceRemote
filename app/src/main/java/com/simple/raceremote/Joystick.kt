package com.simple.raceremote

import android.util.Log
import android.view.MotionEvent.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class Center(val x: Int, val y: Int) {
    val radius = x
}

private fun Center.getAngle(
    xTouch: Float,
    yTouch: Float,
    a: Float,
    hypotenuse: Float
): Float {
    val triangleAngle = asin(a / hypotenuse)
    return when {
        xTouch >= x && yTouch <= y -> 90f.toRadian() - triangleAngle
        xTouch < x && yTouch < y -> 90f.toRadian() + triangleAngle
        xTouch < x && yTouch > y -> 270f.toRadian() - triangleAngle
        else -> 270f.toRadian() + triangleAngle
    }
}

private fun Float.toRadian() = (this / 180 * Math.PI).toFloat()

/**
 *
 *
 *      *\
 *      * \
 *    b *  \ hypotenuse
 *      *   \
 *      *____\
 *         a
 * */
private fun Center.changeJoystickPosition2(
    offsetX: MutableState<Float>,
    offsetY: MutableState<Float>,
    xTouch: Float,
    yTouch: Float
) {
    val a = abs(x - xTouch)
    val b = abs(y - yTouch)
    val hypotenuse = sqrt(a.pow(2) + b.pow(2))
    val angle = getAngle(xTouch, yTouch, a, hypotenuse)

    val (aX, aY) = if (hypotenuse > radius) {
        val newX = radius * cos(angle)
        val newY = radius * sin(angle)

        newX to -newY
    } else {
        (xTouch - x) to (yTouch - y)
    }

    Log.d("fuck", "offsetX = $aX, offsetY = $aY")
    offsetX.value = aX
    offsetY.value = aY
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Joystick(modifier: Modifier = Modifier) {
    val smallCircle = 1f / 5f
    val circle = RoundedCornerShape(50)
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    var center = Center(0, 0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .background(Color.Blue, shape = circle)
            .onGloballyPositioned {
                val radius = it.size.height / 2
                center = Center(radius, radius)
            }
            .pointerInteropFilter() {
                when (it.action) {
                    ACTION_DOWN -> {
                        center.changeJoystickPosition2(offsetX, offsetY, it.x, it.y)
                    }
                    ACTION_MOVE -> {
                        center.changeJoystickPosition2(offsetX, offsetY, it.x, it.y)
                    }
                    ACTION_UP, ACTION_POINTER_UP, ACTION_CANCEL -> {
                        offsetX.value = 0f
                        offsetY.value = 0f
                    }
                }

                true
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .fillMaxSize(smallCircle)
                .aspectRatio(1f)
                .background(Color.Red, shape = circle)
        )
    }
}