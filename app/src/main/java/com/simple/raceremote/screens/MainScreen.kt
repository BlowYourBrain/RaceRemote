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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.utils.debug
import com.simple.raceremote.views.Orientation
import com.simple.raceremote.views.Slider

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Actions(
            bluetoothOnClick = {
                navController.navigate(Screens.BluetoothDevices.name)
            },
            settingsOnClick = {

            }
        )
        Controllers(
            { debug("horizontal: $it") },
            { debug("vertical: $it") },
        )
    }

}

@Composable
fun Actions(
    modifier: Modifier = Modifier,
    bluetoothOnClick: (() -> Unit)? = null,
    settingsOnClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {

        ActionButton(
            modifier = Modifier.padding(Padding.Content),
            icon = R.drawable.ic_baseline_bluetooth_searching_24,
            onClick = bluetoothOnClick
        )

        ActionButton(
            modifier = Modifier.padding(Padding.Content),
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
            .size(Size.ButtonAsIcon),
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),

        shape = CornerShapes.HugeItem,
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
    val shape = CornerShapes.HugeItem

    Row {
        Box(
            Modifier
                .weight(1f)
                .padding(Padding.Content)
                .clip(shape)
        ) {
            Slider() {
                horizontalSlider?.invoke(it)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .padding(Padding.Content)
                .clip(shape)
        ) {
            Slider(orientation = Orientation.Vertical) {
                verticalSlider?.invoke(it)
            }
        }

    }
}