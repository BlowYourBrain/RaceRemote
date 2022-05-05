package com.simple.raceremote.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.simple.raceremote.utils.awaitPointerEventInfinitely
import com.simple.raceremote.utils.pxToDp

private const val ANIMATION_DURATION = 300
private const val ALPHA_TRANSPARENT = 0.0f
private const val ALPHA_TARGET = 0.7f
private const val VIEW_WIDTH_RATIO = 0.4F
private const val CORNER_RADIUS = 16
private const val ELEVATION = 16

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SidePanel(
    isOpened: State<Boolean>,
    updateIsOpened: (Boolean) -> Unit,
    itemProvider: State<@Composable () -> Unit>,
    block: @Composable () -> Unit
) {
    val color = Color.Black
    val sliderWidth = remember { mutableStateOf(0) }
    val containerWidth = remember { mutableStateOf(0) }
    val offset by animateDpAsState(
        targetValue = if (isOpened.value) 0.dp else sliderWidth.value.pxToDp(),
        animationSpec = tween(ANIMATION_DURATION)
    )
    val alpha by animateFloatAsState(
        targetValue = if (isOpened.value) ALPHA_TARGET else ALPHA_TRANSPARENT,
        animationSpec = tween(ANIMATION_DURATION)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    awaitPointerEventInfinitely(PointerEventPass.Initial) {
                        consumePointer(
                            isOpened = isOpened,
                            sliderWidth = sliderWidth.value,
                            updateIsOpened = updateIsOpened,
                            containerWidth = containerWidth.value,
                        )
                    }
                }
            }
            .onGloballyPositioned {
                containerWidth.value = it.size.width
            },
        contentAlignment = Alignment.TopEnd
    ) {
        block.invoke()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(color = color)
        ) {}

        Surface(
            modifier = Modifier
                .offset(x = offset)
                .fillMaxHeight()
                .fillMaxWidth(VIEW_WIDTH_RATIO)
                .onGloballyPositioned { sliderWidth.value = it.size.width },
            elevation = ELEVATION.dp,
            shape = RoundedCornerShape(
                topStart = CORNER_RADIUS.dp,
                bottomStart = CORNER_RADIUS.dp,
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                itemProvider.value.invoke()
            }
        }
    }
}

private fun shouldCloseSlider(
    pointerX: Float,
    sliderWidth: Int,
    containerWidth: Int,
): Boolean = pointerX <= (containerWidth - sliderWidth)

private fun PointerEvent.consumePointer(
    sliderWidth: Int,
    containerWidth: Int,
    isOpened: State<Boolean>,
    updateIsOpened: (Boolean) -> Unit,
) {
    changes.firstOrNull()?.let { pointerInputChange ->
        if (
            isOpened.value &&
            shouldCloseSlider(
                sliderWidth = sliderWidth,
                containerWidth = containerWidth,
                pointerX = pointerInputChange.position.x
            )
        ) {
            //do not provide down events
            if (pointerInputChange.changedToUpIgnoreConsumed()) {
                updateIsOpened.invoke(false)
            }

            pointerInputChange.consumeAllChanges()
        }
    }
}
