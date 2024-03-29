package com.simple.raceremote.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke.Companion.DefaultMiter
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.Size.HugeIcon
import com.simple.raceremote.ui.theme.getPalette
import com.simple.raceremote.ui.views.Orientation.Horizontal.iconLeft
import com.simple.raceremote.ui.views.Orientation.Horizontal.iconRight
import com.simple.raceremote.ui.views.Orientation.Vertical.iconDown
import com.simple.raceremote.ui.views.Orientation.Vertical.iconUp
import com.simple.raceremote.utils.awaitPointerEventInfinitely
import com.simple.raceremote.utils.debug

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
@Composable
fun Slider(
    orientation: Orientation = Orientation.Horizontal,
    onOffsetChange: ((Float) -> Unit)? = null,
) {
    val x = remember { mutableStateOf(UNDEFINED) }
    val y = remember { mutableStateOf(UNDEFINED) }
    
    val palette = remember { getPalette() }
    val selectionColor = remember { palette.colors.surface }
    val iconColor = remember { palette.colors.onSurface }
    val separatorColor = remember { palette.colors.onSurface }
    val surfaceColor = remember { palette.customColors.surfaceVariant }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    awaitPointerEventInfinitely(PointerEventPass.Final) {
                        onPointerInput(x, y)
                    }
                }
            }
    ) {
        when (orientation) {
            is Orientation.Horizontal -> drawHorizontal(
                x = x,
                selectionColor = selectionColor,
                separatorColor = separatorColor,
                onOffsetChange = onOffsetChange,
            )

            is Orientation.Vertical -> drawVertical(
                y = y,
                onBackgroundColor = selectionColor,
                separatorColor = separatorColor,
                onOffsetChange = onOffsetChange
            )
        }
    }

    orientation.paintIcons(iconColor)
}

private fun PointerEvent.onPointerInput(
    x: MutableState<Float>,
    y: MutableState<Float>
): PointerEvent = apply {
    changes.firstOrNull()?.run {
        debug(toString())

        if (!isConsumed) {
            if (pressed) {
                x.value = position.x
                y.value = position.y
            } else {
                x.value = UNDEFINED
                y.value = UNDEFINED
            }

            consumeDownChange()
        }
    }
}

@Composable
@Preview
private fun previewPaintIcons() {
    Orientation.Horizontal.paintIcons(MaterialTheme.colors.onBackground)
}

@Composable
private fun Orientation.paintIcons(
    iconColor: Color
) {
    val containerModifier = Modifier.fillMaxSize()
    val iconModifier = Modifier.size(HugeIcon)

    when (this) {
        is Orientation.Vertical -> {
            Column(
                modifier = containerModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                paintContent(
                    modifier = iconModifier.weight(1f),
                    first = iconUp,
                    second = iconDown,
                    paintColor = iconColor
                )
            }
        }

        is Orientation.Horizontal -> {
            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                paintContent(
                    modifier = iconModifier.weight(1f),
                    first = iconLeft,
                    second = iconRight,
                    paintColor = iconColor
                )
            }
        }
    }
}

@Composable
private fun paintContent(
    modifier: Modifier,
    @DrawableRes first: Int,
    @DrawableRes second: Int,
    paintColor: Color
) {
    val color = ColorFilter.tint(paintColor)

    Image(
        modifier = modifier,
        painter = painterResource(id = first),
        contentDescription = null,
        colorFilter = color
    )

    Image(
        modifier = modifier,
        painter = painterResource(id = second),
        contentDescription = null,
        colorFilter = color
    )
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
    selectionColor: Color,
    separatorColor: Color,
    onOffsetChange: ((Float) -> Unit)? = null
) {
    val halfWidth: Float = size.width / 2

    val update = if (x.value != UNDEFINED) {
        if (x.value < halfWidth) {
            val pointerOffset = maxOf(halfWidth - x.value, NO_OFFSET)

            drawRect(
                color = selectionColor,
                topLeft = Offset(x.value, NO_OFFSET),
                size = Size(pointerOffset, size.height)
            )

            maxOf(-(pointerOffset / halfWidth), MIN)
        } else {
            val pointerOffset = minOf(x.value - halfWidth, halfWidth)

            drawRect(
                color = selectionColor,
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
