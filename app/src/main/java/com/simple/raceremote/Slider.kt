package com.simple.raceremote

import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke.Companion.DefaultMiter
import androidx.compose.ui.input.pointer.pointerInteropFilter

private const val UNDEFINED = -1f

sealed class Orientation() {
    object Horizontal : Orientation() {
        @DrawableRes
        val iconLeft = R.drawable.ic_baseline_chevron_left_24

        @DrawableRes
        val iconRight = R.drawable.ic_baseline_chevron_right_24
    }

    object Vertical : Orientation() {
        @DrawableRes
        val iconUp = R.drawable.ic_baseline_keyboard_arrow_up_24

        @DrawableRes
        val iconDown = R.drawable.ic_baseline_keyboard_arrow_down_24
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Slider(
    modifier: Modifier,
    orientation: Orientation = Orientation.Horizontal
) {
    val x = remember { mutableStateOf(UNDEFINED) }
    val y = remember { mutableStateOf(UNDEFINED) }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Cyan)
            .pointerInteropFilter() {
                when (it.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        x.value = it.x
                        y.value = it.y
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        x.value = UNDEFINED
                        y.value = UNDEFINED
                    }
                }

                true
            }
    ) {
        val color = Color.Red
        if (orientation is Orientation.Horizontal && x.value != UNDEFINED) {
            if (x.value < size.width / 2) {
                drawRect(
                    color = color,
                    topLeft = Offset(x.value, 0f),
                    size = Size(size.width / 2 - x.value, size.height)
                )
            } else {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width / 2, 0f),
                    size = Size(minOf(x.value - size.width / 2, size.width / 2), size.height)
                )
            }
//            drawImage(
//                image = ImageBitmap.imageResource(
//                    res = resources,
//                    id = orientation.iconLeft
//                )
//            )


        }

        if (orientation == Orientation.Vertical && y.value != UNDEFINED) {

        }

        //separator
        drawLine(
            Color.White,
            Offset(size.width / 2, 0f),
            Offset(size.width / 2, size.height),
            strokeWidth = DefaultMiter
        )
    }
}