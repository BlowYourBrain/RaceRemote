package vea.raceremote.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

/** Relative drag, owned by the first finger. Release/cancel never transfers input. */
@Composable
fun DrivingPad(
    label: String,
    vertical: Boolean,
    value: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    val change by rememberUpdatedState(onValueChange)
    val accent = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = .3f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .background(MaterialTheme.colors.onSurface.copy(alpha = .06f), RoundedCornerShape(16.dp))
            .testTag(if (vertical) "drive_throttle" else "drive_steering")
            .semantics {
                contentDescription = "$label: удерживай и двигай палец; отпусти для нейтрали"
                stateDescription = "${(value * 100).toInt()}%"
                if (!enabled) disabled()
            }
            .pointerInput(enabled, vertical) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        down.consume()
                        val origin = down.position
                        val travel = ((if (vertical) size.height else size.width) / 2f - 24.dp.toPx()).coerceAtLeast(1f)
                        val deadZone = viewConfiguration.touchSlop.coerceAtMost(travel / 3)
                        try {
                            change(0f)
                            while (true) {
                                val event = awaitPointerEvent()
                                val finger = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!finger.pressed || finger.isConsumed ||
                                    finger.position.x !in 0f..size.width.toFloat() ||
                                    finger.position.y !in 0f..size.height.toFloat()) break
                                val distance = if (vertical) origin.y - finger.position.y else finger.position.x - origin.x
                                val amount = if (abs(distance) <= deadZone) 0f else
                                    sign(distance) * ((abs(distance) - deadZone) / (travel - deadZone)).coerceIn(0f, 1f)
                                finger.consume()
                                change(amount)
                            }
                        } finally {
                            // Includes ACTION_CANCEL, loss of connection and removal from composition.
                            change(0f)
                        }
                        // A second finger already held in this pad cannot take over the first.
                        while (currentEvent.changes.any { it.pressed }) awaitPointerEvent()
                    }
                }
            }) {
            val radius = 20.dp.toPx()
            val travel = ((if (vertical) size.height else size.width) / 2 - radius - 4.dp.toPx()).coerceAtLeast(0f)
            val delta = if (vertical) Offset(0f, -travel) else Offset(travel, 0f)
            drawLine(accent.copy(alpha = .3f), center - delta, center + delta, 4.dp.toPx())
            drawCircle(accent.copy(alpha = .3f), 5.dp.toPx(), center)
            drawCircle(accent, radius, center + delta * value)
        }
        Text(if (vertical) "↑ вперёд · ↓ назад" else "← влево · вправо →", style = MaterialTheme.typography.caption)
    }
}
