package com.simple.raceremote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

object CornerShapes {
    val HugeItem = RoundedCornerShape(16.dp)
    val SmallItem = RoundedCornerShape(8.dp)
    val Round = RoundedCornerShape(50)
}
