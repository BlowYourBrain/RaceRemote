package com.simple.raceremote.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

@Composable
fun Int.pxToDp(): Dp = (this / LocalDensity.current.density).dp

suspend fun AwaitPointerEventScope.awaitPointerEventInfinitely(
    pass: PointerEventPass = PointerEventPass.Main,
    block: PointerEvent.() -> Unit
){
    while (true){
        awaitPointerEvent(pass).apply {
            block.invoke(this)
        }
    }
}