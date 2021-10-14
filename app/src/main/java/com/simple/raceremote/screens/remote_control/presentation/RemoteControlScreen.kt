package com.simple.raceremote.screens.remote_control.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.views.ActionButton
import com.simple.raceremote.views.Orientation
import com.simple.raceremote.views.Slider

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val viewModel: RemoteControlViewModel = viewModel()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Actions(
            bluetoothOnClick = {
                navController.navigate(Screens.BluetoothDevices.name)
            },
            settingsOnClick = {

            }
        )
        Controllers(
            { viewModel.updateSteeringWheel(it) },
            { viewModel.updateMovement(it) },
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