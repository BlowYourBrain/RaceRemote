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
private const val NO_OFFSET = 0f
private const val MIN = -1f
private const val MAX = 1f

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

//TODO отрисовать иконки
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Slider(
    modifier: Modifier,
    orientation: Orientation = Orientation.Horizontal,
    onPointerChange: ((Float) -> Unit)? = null
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
        val separatorColor = Color.White
        val separatorWidth = DefaultMiter
        val halfWidth = size.width / 2
        val halfHeight = size.height / 2

        if (orientation is Orientation.Horizontal) {
            val update = if (x.value != UNDEFINED) {
                if (x.value < halfWidth) {
                    val pointerOffset = maxOf(halfWidth - x.value, NO_OFFSET)

                    drawRect(
                        color = color,
                        topLeft = Offset(x.value, NO_OFFSET),
                        size = Size(pointerOffset, size.height)
                    )

                    maxOf(-(pointerOffset / halfWidth), MIN)
                } else {
                    val pointerOffset = minOf(x.value - halfWidth, halfWidth)

                    drawRect(
                        color = color,
                        topLeft = Offset(halfWidth, NO_OFFSET),
                        size = Size(pointerOffset, size.height)
                    )

                    minOf(pointerOffset / halfWidth, MAX)
                }
            } else {
                NO_OFFSET
            }

            onPointerChange?.invoke(update)

            //separator
            drawLine(
                separatorColor,
                Offset(halfWidth, NO_OFFSET),
                Offset(halfWidth, size.height),
                strokeWidth = separatorWidth
            )
        }

        if (orientation == Orientation.Vertical) {
            val update = if (y.value != UNDEFINED) {
                if (y.value < halfHeight) {
                    val pointerOffset = minOf(y.value - halfHeight, NO_OFFSET)

                    drawRect(
                        color = color,
                        topLeft = Offset(NO_OFFSET, halfHeight),
                        size = Size(
                            size.width,
                            pointerOffset
                        )
                    )

                    minOf(-(pointerOffset / halfHeight), MAX)
                } else {
                    val pointerOffset = maxOf(y.value - halfHeight, MIN)

                    drawRect(
                        color = color,
                        topLeft = Offset(NO_OFFSET, halfHeight),
                        size = Size(
                            size.width,
                            pointerOffset
                        )
                    )

                    maxOf(MIN, -(pointerOffset / halfHeight))
                }
            } else {
                NO_OFFSET
            }

            onPointerChange?.invoke(update)

            //separator
            drawLine(
                separatorColor,
                Offset(NO_OFFSET, halfHeight),
                Offset(size.width, halfHeight),
                strokeWidth = separatorWidth
            )
        }
    }
}