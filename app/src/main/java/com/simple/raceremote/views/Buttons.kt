package com.simple.raceremote.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size

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
    onClick: (() -> Unit)? = null
) {
    Button(
        modifier = modifier.size(Size.ButtonAsIcon),
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),
        shape = CornerShapes.HugeItem,
        onClick = { onClick?.invoke() }
    ) {
        Image(painter = painterResource(icon), contentDescription = null)
    }
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