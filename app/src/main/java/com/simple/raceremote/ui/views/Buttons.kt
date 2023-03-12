package com.simple.raceremote.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.ui.theme.getPalette

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    @DrawableRes
    icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Button(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),
        shape = CornerShapes.SmallItem,
        contentPadding = PaddingValues(Padding.Content)
    ) {
        icon?.let { Image(painter = painterResource(id = it), contentDescription = null) }
        text?.let { Text(text = text) }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: (() -> Unit)? = null,
) {
    val palette = remember { getPalette() }
    val iconColor = remember { palette.colors.onSurface }
    val backgroundColor = remember { palette.colors.surface }
    val contentColor = remember { palette.colors.onSurface }

    Button(
        modifier = modifier.size(Size.ButtonAsIcon),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = CornerShapes.HugeItem,
        onClick = { onClick?.invoke() }
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Preview
@Composable
private fun PreviewActionButton() {
    ActionButton(icon = R.drawable.ic_baseline_wifi_find_24)
}

@Composable
fun RoundActionButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: (() -> Unit)? = null
) {
    Button(
        modifier = modifier.size(Size.HugeButtonAsIcon),
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),
        shape = CornerShapes.Round,
        onClick = { onClick?.invoke() }
    ) {
        Image(painter = painterResource(icon), contentDescription = null)
    }
}
