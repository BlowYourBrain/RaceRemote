package com.simple.raceremote

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
import kotlin.math.pow
import kotlin.math.roundToInt

data class Center(val x: Int, val y: Int)

private fun Center.isAxesInCircle(axes: Float): Boolean {
    return (axes - this.x).pow(2) < this.x * this.x
}

private fun Center.changeJoystickPosition(
    offsetX: MutableState<Float>,
    offsetY: MutableState<Float>,
    xTouch: Float,
    yTouch: Float
) {
    when {
        isAxesInCircle(xTouch) -> offsetX.value = xTouch - this.x
        else -> offsetX.value = this.x * if (xTouch > this.x) 1f else -1f
    }

    when {
        isAxesInCircle(yTouch) -> offsetY.value = yTouch - this.y
        else -> offsetY.value = this.y * if (yTouch > this.y) 1f else -1f
    }
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
                        center.changeJoystickPosition(offsetX, offsetY, it.x, it.y)
                    }
                    ACTION_MOVE -> {
                        center.changeJoystickPosition(offsetX, offsetY, it.x, it.y)
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