package com.simple.raceremote.ui.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.simple.raceremote.utils.dpToSp

sealed class DotsState() {
    object Loading : DotsState()
    object Idle : DotsState()
    class ShowText(val text: String) : DotsState()
}

@Composable
fun DotsLoadingWrapper(
    modifier: Modifier = Modifier,
    state: State<DotsState>,
    dotsCount: Int = 3,
    dotsColor: Color = Color.Red,
    dotsDiameter: Dp = 6.dp,
    dotsChangeDuration: Long = 100L,
    content: @Composable () -> Unit
) {
    //todo calculate somehow
    val dotsDividerSpace = 4.dp

    val infiniteTransition = rememberInfiniteTransition()
    val visibility = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = dotsChangeDuration.toInt())
        )
    )
    val calculatedHeight = dotsDividerSpace + dotsDiameter

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.alpha(
                    if (state.value is DotsState.Loading) 1f else 0f
                ),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                addLoadingDots(
                    dotsCount = dotsCount,
                    dotsColor = dotsColor,
                    dotsDiameter = dotsDiameter,
                    dotsDividerSpace = dotsDividerSpace
                )
            }

            Text(
                fontSize = dpToSp(dp = calculatedHeight),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(if (state.value is DotsState.ShowText) 1f else 0f)
                    .height(calculatedHeight),
                text = state.value.let { (it as? DotsState.ShowText)?.text ?: "" },
            )

            Spacer(
                modifier = Modifier
                    .height(calculatedHeight)
                    .alpha(if (state.value is DotsState.Idle) 1f else 0f)
            )
        }

        Spacer(modifier = Modifier.height(dotsDividerSpace))

        content()
    }
}

@Composable
private fun addLoadingDots(
    dotsCount: Int,
    dotsDiameter: Dp,
    dotsColor: Color,
    dotsDividerSpace: Dp
) {
    repeat(dotsCount) {
        Box(
            Modifier
                .size(dotsDiameter)
                .background(
                    color = dotsColor,
                    shape = RoundedCornerShape(dotsDiameter / 2)
                )
        ) {}

        if (it < dotsCount - 1) Spacer(modifier = Modifier.width(dotsDividerSpace))
    }
}