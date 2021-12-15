package com.simple.raceremote.ui.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DotsLoadingWrapper(
    modifier: Modifier = Modifier,
    dotsCount: Int = 3,
    dotsColor: Color = Color.Red,
    dotsDiameter: Dp = 6.dp,
    dotsChangeDuration: Long = 100L,
    content: @Composable () -> Unit
) {
    //todo calculate somehow
    val dotsDividerSpace = 2.dp

    val infiniteTransition = rememberInfiniteTransition()
    val visibility = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = dotsChangeDuration.toInt())
        )
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(all = dotsDividerSpace),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(dotsCount) {
                Box(
                    Modifier
                        .size(dotsDiameter)
                        .background(
                            color = dotsColor,
                            shape = RoundedCornerShape(dotsDiameter / 2)
                        )
                        .alpha(visibility.value)
                ) {}
            }
        }

        content()
    }
}