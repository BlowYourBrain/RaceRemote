package com.simple.raceremote.ui.views

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.simple.raceremote.utils.dpToSp
import com.simple.raceremote.utils.pxToDp
import kotlin.math.absoluteValue

private const val DEFAULT_ANIMATION_DURATION = 1500
private const val DEFAULT_DOTS_COUNT = 3
private const val DEFAULT_HEIGHT = 6

private const val TEXT_MAX_LENGTH = 32
private const val TEXT_MAX_LINES = 1
private const val TEXT_SPEED = 0.024f

sealed class DotsState(val height: Dp) {

    class Idle(height: Dp = DEFAULT_HEIGHT.dp) : DotsState(height)

    class Loading(
        height: Dp = DEFAULT_HEIGHT.dp,
        val dotsCount: Int = DEFAULT_DOTS_COUNT,
        val dotsColor: Color? = null,
        val dotsDividerSpace: Dp = 4.dp,
        val dotsAnimationDuration: Int = DEFAULT_ANIMATION_DURATION
    ) : DotsState(height)

    class ShowText(
        val text: String,
        val textSize: Dp,
        val textMaxLength: Int = TEXT_MAX_LENGTH,
        /**Dp per ms*/
        val textVelocity: Dp = TEXT_SPEED.dp,
        height: Dp = DEFAULT_HEIGHT.dp
    ) : DotsState(height) {
        val truncatedText: String
            get() = if (text.length <= textMaxLength) text else "${text.take(textMaxLength)}..."
    }
}

/**
 *
 * @param state could be one of [DotsState].
 * [DotsState.Idle] do not show anything, just fill same height space as other states.
 * [DotsState.Loading] show loading animation
 * [DotsState.ShowText] try to show text, if it is impossible then show it as ticker
 *
 * @param modifier Modifier of container
 * @param dotsCount count of dots in animation.
 * @param dotsColor dot color.
 * @param dotsAnimationDuration duration of scaleIn/scaleDown dot animation.
 * @param dotsDiameter diameter of every dot.
 * @param dotsDividerSpace space between dots.
 * */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlatLoadingWithContent(
    modifier: Modifier = Modifier,
    state: DotsState,
) {
    val transition = updateTransition(state)

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        transition.Crossfade { targetState ->
            when (targetState) {
                is DotsState.Idle -> addEmptySpace(targetState)
                is DotsState.Loading -> addLoadingDots(targetState)
                is DotsState.ShowText -> addText(targetState)
            }
        }
    }
}

@Composable
private fun addEmptySpace(state: DotsState.Idle) {
    Spacer(modifier = Modifier.height(state.height))
}

@Composable
private fun addLoadingDots(
    state: DotsState.Loading
) = state.run {
    val transition = rememberInfiniteTransition()
    val totalAnimationDuration = dotsAnimationDuration * dotsCount / 2
    val color = dotsColor ?: MaterialTheme.colors.primary

    Row(
        modifier = Modifier.height(state.height),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(dotsCount) { position ->
            val scale by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = totalAnimationDuration
                        0.0f at 0 with LinearEasing
                        1.0f at (dotsAnimationDuration / 2) with LinearEasing
                        0.0f at dotsAnimationDuration with LinearEasing
                        0.0f at totalAnimationDuration
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(position * dotsAnimationDuration / 2)
                )
            )

            Box(
                Modifier
                    .size(height)
                    .scale(scale)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(height / 2)
                    )
            ) {}

            if (position < dotsCount - 1) Spacer(modifier = Modifier.width(dotsDividerSpace))
        }
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
private fun addText(state: DotsState.ShowText) {

    val transition = rememberInfiniteTransition()

    val offset = remember {
        mutableStateOf(
            ViewOffset(
                initialOffset = Int.MAX_VALUE,
                targetOffset = Int.MAX_VALUE
            )
        )
    }

    val animationDuration = if (offset.value.targetOffset == Int.MAX_VALUE) {
        Int.MAX_VALUE
    } else {
        state.run { offset.value.targetOffset.absoluteValue.pxToDp() / textVelocity }.toInt()
    }

    val animationOffset by transition.animateValue(
        initialValue = offset.value.initialOffset.pxToDp(),
        targetValue = offset.value.targetOffset.pxToDp(),
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            )
        )
    )

    Text(
        modifier = Modifier
            .offset(animationOffset)
            .height(state.height)
            .wrapContentWidth(unbounded = true, align = Alignment.Start)
            .onGloballyPositioned { layoutCoordinates ->
                val viewWidth = layoutCoordinates.size.width
                val parentWidth = layoutCoordinates.parentCoordinates?.size?.width ?: 0

                offset.value = if (parentWidth >= viewWidth) {
                    ViewOffset()
                } else {
                    ViewOffset(
                        initialOffset = parentWidth,
                        targetOffset = -viewWidth
                    )
                }
            },
        fontSize = dpToSp(dp = state.textSize),
        textAlign = TextAlign.Center,
        maxLines = TEXT_MAX_LINES,
        text = state.truncatedText,
        lineHeight = TextUnit(value = state.textSize.value, type = TextUnitType.Unspecified)
    )
}

private class ViewOffset(
    val initialOffset: Int = 0,
    val targetOffset: Int = 0
)
