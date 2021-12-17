package com.simple.raceremote.ui.views

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simple.raceremote.utils.dpToSp

private const val DEFAULT_ANIMATION_DURATION = 1500

sealed class DotsState(val height: Dp) {

    class Idle(height: Dp) : DotsState(height)

    class Loading(
        val dotsCount: Int = 3,
        val dotsColor: Color? = null,
        val dotsDiameter: Dp = 6.dp,
        val dotsDividerSpace: Dp = 4.dp,
        val dotsAnimationDuration: Int = DEFAULT_ANIMATION_DURATION
    ) : DotsState(dotsDiameter)

    class ShowText(
        val text: String,
        val textSize: Dp,
        height: Dp
    ) : DotsState(height)
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
    state: State<DotsState>,
) {
    val transition = updateTransition(state)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            transition.Crossfade { targetState ->
                when (val _state = targetState.value) {
                    is DotsState.Idle -> {
                        Spacer(modifier = Modifier.height(_state.height))
                    }

                    is DotsState.Loading -> {
                        Row(
                            modifier = Modifier.height(_state.height),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            addLoadingDots(
                                dotsCount = _state.dotsCount,
                                dotsColor = _state.dotsColor ?: MaterialTheme.colors.primary,
                                dotsDiameter = _state.dotsDiameter,
                                dotsDividerSpace = _state.dotsDividerSpace,
                                animationDuration = _state.dotsAnimationDuration
                            )
                        }
                    }

                    is DotsState.ShowText -> {
                        Text(
                            fontSize = dpToSp(dp = _state.textSize),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(_state.height),
                            text = _state.text
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun addLoadingDots(
    dotsCount: Int,
    dotsDiameter: Dp,
    dotsColor: Color,
    dotsDividerSpace: Dp,
    animationDuration: Int
) {
    val transition = rememberInfiniteTransition()
    val totalAnimationDuration = animationDuration * dotsCount / 2

    repeat(dotsCount) { position ->
        val scale by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = totalAnimationDuration
                    0.0f at 0 with LinearEasing
                    1.0f at (animationDuration / 2) with LinearEasing
                    0.0f at animationDuration with LinearEasing
                    0.0f at totalAnimationDuration
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(position * animationDuration / 2)
            )
        )

        Box(
            Modifier
                .size(dotsDiameter)
                .scale(scale)
                .background(
                    color = dotsColor,
                    shape = RoundedCornerShape(dotsDiameter / 2)
                )
        ) {}

        if (position < dotsCount - 1) Spacer(modifier = Modifier.width(dotsDividerSpace))
    }
}