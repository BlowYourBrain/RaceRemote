package com.simple.raceremote.views

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke.Companion.DefaultMiter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.simple.raceremote.R
import com.simple.raceremote.utils.debug

private const val UNDEFINED = -1f
private const val UNDEFINED_INT = -1
private const val NO_OFFSET = 0f
private const val MIN = -1f
private const val MAX = 1f

sealed class Orientation() {
    object Horizontal : Orientation() {
        @DrawableRes
        val iconLeft = R.drawable.ic_baseline_chevron_left_24

        @DrawableRes
        val iconRight = R.drawable.ic_baseline_chevron_right_24

        override fun toString(): String = "Horizontal"
    }

    object Vertical : Orientation() {
        @DrawableRes
        val iconUp = R.drawable.ic_baseline_keyboard_arrow_up_24

        @DrawableRes
        val iconDown = R.drawable.ic_baseline_keyboard_arrow_down_24

        override fun toString(): String = "Vertical"
    }
}

//TODO отрисовать иконки
/**
 * Вертикальный/горизонтальный слайдер, позволяющий оттягивать пальцев по-горизонтали/по-вертикали
 * ползунок. [onOffsetChange] - лямбда, срабатывающая при изменении положения пальца.
 * При [Orientation.Horizontal] в крайнем левом положении лямбда вернет -1f, в крайнем правом 1f.
 * При [Orientation.Vertical] в самом высоком положении лямбда вернёт 1f, в нижнем -1f.
 * При отпускании пальца вне зависимости от [Orientation] лямбда вернёт 0f.
 *
 * @param modifier - модификатор
 * @param orientation - ориентация экрана
 * @param onOffsetChange - лямбда, срабатывающая при изменении положения пальца. Значения от -1f до 1f.
 * */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Slider(
    orientation: Orientation = Orientation.Horizontal,
    onOffsetChange: ((Float) -> Unit)? = null,
) {
    val x = remember { mutableStateOf(UNDEFINED) }
    val y = remember { mutableStateOf(UNDEFINED) }
    val onBackgroundColor = MaterialTheme.colors.onBackground
    val separatorColor = MaterialTheme.colors.onBackground
    val sliderRect = remember { mutableStateOf(Rect(Offset.Zero, 0f)) }
    val pointerId = remember { mutableStateOf(UNDEFINED_INT) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .onGloballyPositioned { sliderRect.value = it.boundsInRoot() }
            .pointerInteropFilter() {
                it.onPointerInteropFilter(
                    x = x,
                    y = y,
                    pointerId = pointerId,
                    sliderRect = sliderRect,
                    orientation = orientation,
                )

                true
            }
    ) {
        when (orientation) {
            is Orientation.Horizontal -> drawHorizontal(
                x = x,
                onBackgroundColor = onBackgroundColor,
                separatorColor = separatorColor,
                onOffsetChange = onOffsetChange,
            )

            is Orientation.Vertical -> drawVertical(
                y = y,
                onBackgroundColor = onBackgroundColor,
                separatorColor = separatorColor,
                onOffsetChange = onOffsetChange
            )
        }
    }
}

private fun MotionEvent.onPointerInteropFilter(
    x: MutableState<Float>,
    y: MutableState<Float>,
    pointerId: MutableState<Int>,
    sliderRect: MutableState<Rect>,
    orientation: Orientation
){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN -> {
                debug(
                    "type: $orientation \npointerId = ${pointerId.value}\nx = ${getRawX(actionIndex)}, \ny = ${
                        getRawY(actionIndex)
                    }"
                )

                if (pointerId.value != UNDEFINED_INT) {
                    val pointerIndex = findPointerIndex(pointerId.value)
                    x.value = getX(pointerIndex)
                    y.value = getY(pointerIndex)
                }

                if (pointerId.value == UNDEFINED_INT && sliderRect.value.contains(
                        Offset(
                            getRawX(actionIndex),
                            getRawY(actionIndex)
                        )
                    )
                ) {
                    pointerId.value = getPointerId(actionIndex)
                    x.value = getX(actionIndex)
                    y.value = getY(actionIndex)

                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (pointerId.value == getPointerId(actionIndex)) {
                    pointerId.value = UNDEFINED_INT
                    x.value = UNDEFINED
                    y.value = UNDEFINED
                }
            }
        }
    }
}

private fun DrawScope.drawVertical(
    y: MutableState<Float>,
    onBackgroundColor: Color,
    separatorColor: Color,
    onOffsetChange: ((Float) -> Unit)? = null
) {
    val halfHeight = size.height / 2

    val update = if (y.value != UNDEFINED) {
        if (y.value < halfHeight) {
            val pointerOffset = minOf(y.value - halfHeight, NO_OFFSET)

            drawRect(
                color = onBackgroundColor,
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
                color = onBackgroundColor,
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

    onOffsetChange?.invoke(update)

    drawSeparator(
        orientation = Orientation.Vertical,
        separatorColor = separatorColor
    )
}

private fun DrawScope.drawHorizontal(
    x: MutableState<Float>,
    onBackgroundColor: Color,
    separatorColor: Color,
    onOffsetChange: ((Float) -> Unit)? = null
) {
    val halfWidth: Float = size.width / 2

    val update = if (x.value != UNDEFINED) {
        if (x.value < halfWidth) {
            val pointerOffset = maxOf(halfWidth - x.value, NO_OFFSET)

            drawRect(
                color = onBackgroundColor,
                topLeft = Offset(x.value, NO_OFFSET),
                size = Size(pointerOffset, size.height)
            )

            maxOf(-(pointerOffset / halfWidth), MIN)
        } else {
            val pointerOffset = minOf(x.value - halfWidth, halfWidth)

            drawRect(
                color = onBackgroundColor,
                topLeft = Offset(halfWidth, NO_OFFSET),
                size = Size(pointerOffset, size.height)
            )

            minOf(pointerOffset / halfWidth, MAX)
        }
    } else {
        NO_OFFSET
    }

    onOffsetChange?.invoke(update)

    drawSeparator(
        orientation = Orientation.Horizontal,
        separatorColor = separatorColor
    )
}

private fun DrawScope.drawSeparator(
    orientation: Orientation,
    separatorColor: Color,
    separatorWidth: Float = DefaultMiter,
) {
    val (start, end) = when (orientation) {
        is Orientation.Horizontal -> {
            val horizontalCenter = size.width / 2
            Offset(horizontalCenter, NO_OFFSET) to Offset(horizontalCenter, size.height)
        }
        is Orientation.Vertical -> {
            val verticalCenter = size.height / 2
            Offset(NO_OFFSET, verticalCenter) to Offset(size.width, verticalCenter)
        }
    }

    drawLine(
        color = separatorColor,
        start = start,
        end = end,
        strokeWidth = separatorWidth
    )
}