package com.simple.raceremote

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke.Companion.DefaultMiter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

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
    modifier: Modifier,
    orientation: Orientation = Orientation.Horizontal,
    onOffsetChange: ((Float) -> Unit)? = null,
) {
    val x = remember { mutableStateOf(UNDEFINED) }
    val y = remember { mutableStateOf(UNDEFINED) }
    val onBackground = MaterialTheme.colors.onBackground
    val separatorColor = MaterialTheme.colors.onBackground
    var sliderRect by remember { mutableStateOf(Rect(Offset.Zero, 0f)) }
    var pointerId by remember { mutableStateOf(UNDEFINED_INT) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .onGloballyPositioned { sliderRect = it.boundsInWindow() }
            .pointerInteropFilter() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    when (it.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            if (pointerId != UNDEFINED_INT) {
                                val pointerIndex = it.findPointerIndex(pointerId)
                                x.value = it.getX(pointerIndex)
                                y.value = it.getY(pointerIndex)
                            }

                            if (pointerId == UNDEFINED_INT && sliderRect.contains(
                                    Offset(
                                        it.getRawX(
                                            it.actionIndex
                                        ), it.getRawY(it.actionIndex)
                                    )
                                )
                            ) {
                                pointerId = it.getPointerId(it.actionIndex)
                                x.value = it.getX(it.actionIndex)
                                y.value = it.getX(it.actionIndex)
                            }
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            if (pointerId == it.getPointerId(it.actionIndex)) {
                                pointerId = UNDEFINED_INT
                                x.value = UNDEFINED
                                y.value = UNDEFINED
                            }
                        }
                    }
                }

                true
            }
    ) {
        val separatorWidth = DefaultMiter
        val halfWidth = size.width / 2
        val halfHeight = size.height / 2

        if (orientation is Orientation.Horizontal) {
            val update = if (x.value != UNDEFINED) {
                if (x.value < halfWidth) {
                    val pointerOffset = maxOf(halfWidth - x.value, NO_OFFSET)

                    drawRect(
                        color = onBackground,
                        topLeft = Offset(x.value, NO_OFFSET),
                        size = Size(pointerOffset, size.height)
                    )

                    maxOf(-(pointerOffset / halfWidth), MIN)
                } else {
                    val pointerOffset = minOf(x.value - halfWidth, halfWidth)

                    drawRect(
                        color = onBackground,
                        topLeft = Offset(halfWidth, NO_OFFSET),
                        size = Size(pointerOffset, size.height)
                    )

                    minOf(pointerOffset / halfWidth, MAX)
                }
            } else {
                NO_OFFSET
            }

            onOffsetChange?.invoke(update)

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
                        color = onBackground,
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
                        color = onBackground,
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

@RequiresApi(Build.VERSION_CODES.Q)
private fun Rect.isPointerInsideRect(event: MotionEvent): Boolean =
    contains(Offset(event.rawX, event.rawY))