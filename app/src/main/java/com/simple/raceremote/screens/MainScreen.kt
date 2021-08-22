package com.simple.raceremote.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ProvideWindowInsets
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.debug
import com.simple.raceremote.views.Orientation
import com.simple.raceremote.views.Slider


@Composable
fun RemoteControlScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Actions(Modifier)
        Controllers(
            { debug("horizontal: $it") },
            { debug("vertical: $it") },
        )
    }

}

@Composable
fun Actions(
    modifier: Modifier,
    bluetoothOnClick: (() -> Unit)? = null,
    settingsOnClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {

        ActionButton(
            modifier = Modifier.padding(12.dp),
            icon = R.drawable.ic_baseline_bluetooth_searching_24,
            onClick = bluetoothOnClick
        )

        ActionButton(
            modifier = Modifier.padding(12.dp),
            icon = R.drawable.ic_baseline_settings_24,
            onClick = settingsOnClick
        )
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier,
    @DrawableRes icon: Int,
    onClick: (() -> Unit)? = null
) {
    Button(
        modifier = modifier
            .size(48.dp),
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),

        shape = CornerShapes._16dp,
        onClick = { onClick?.invoke() }
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}

@Composable
private fun Controllers(
    horizontalSlider: ((Float) -> Unit)? = null,
    verticalSlider: ((Float) -> Unit)? = null
) {
    val shape = CornerShapes._16dp

    Row {
        Box(
            Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape)
        ) {
            Slider() {
                horizontalSlider?.invoke(it)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape)
        ) {
            Slider(orientation = Orientation.Vertical) {
                verticalSlider?.invoke(it)
            }
        }

    }
}