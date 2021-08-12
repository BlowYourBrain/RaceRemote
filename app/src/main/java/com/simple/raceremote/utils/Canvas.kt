package com.simple.raceremote.utils

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset


fun DrawScope.inset(
    left: Float = 0f,
    top: Float = 0f,
    right: Float = 0f,
    bottom: Float = 0f,
    block: DrawScope.() -> Unit
) {
    inset(
        left, top, right, bottom, block
    )
}