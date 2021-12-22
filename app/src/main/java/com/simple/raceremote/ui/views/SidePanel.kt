package com.simple.raceremote.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsHeight
import com.simple.raceremote.utils.pxToDp

private const val ANIMATION_DURATION = 300
private const val VIEW_WIDTH_RATIO = 0.4F
private const val CORNER_RADIUS = 16
private const val ELEVATION = 16

@Composable
fun SidePanel(
    isOpened: State<Boolean>,
    itemProvider: State<@Composable () -> Unit>
) {
    val viewWidth = remember { mutableStateOf(0) }
    val offset by animateDpAsState(
        targetValue = if (isOpened.value) 0.dp else viewWidth.value.pxToDp(),
        animationSpec = tween(ANIMATION_DURATION)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            modifier = Modifier
                .offset(x = offset)
                .fillMaxHeight()
                .fillMaxWidth(VIEW_WIDTH_RATIO)
                .onGloballyPositioned { viewWidth.value = it.size.width },
            elevation = ELEVATION.dp,
            shape = RoundedCornerShape(
                topStart = CORNER_RADIUS.dp,
                bottomStart = CORNER_RADIUS.dp,
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.statusBarsHeight())
                itemProvider.value.invoke()
            }
        }
    }

}