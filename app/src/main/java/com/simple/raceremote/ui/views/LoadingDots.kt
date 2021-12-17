package com.simple.raceremote.ui.views

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simple.raceremote.utils.dpToSp

sealed class DotsState() {
    object Loading : DotsState()
    object Idle : DotsState()
    class ShowText(val text: String) : DotsState()
}

@OptIn(ExperimentalAnimationApi::class)
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

    val transition = updateTransition(state)

    val calculatedHeight = dotsDividerSpace + dotsDiameter

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            transition.Crossfade { targetState ->
                val _state = targetState.value
                when (_state) {
                    is DotsState.Idle -> {
                        Spacer(modifier = Modifier.height(calculatedHeight))
                    }

                    is DotsState.Loading -> {
                        Row(
                            modifier = Modifier.height(calculatedHeight),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            addLoadingDots(
                                dotsCount = dotsCount,
                                dotsColor = dotsColor,
                                dotsDiameter = dotsDiameter,
                                dotsDividerSpace = dotsDividerSpace
                            )
                        }
                    }

                    is DotsState.ShowText -> {
                        Text(
                            fontSize = dpToSp(dp = calculatedHeight),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(calculatedHeight),
                            text = _state.text
                        )
                    }
                }
            }
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