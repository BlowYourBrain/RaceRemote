package com.simple.raceremote.screens.remote_control.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.ui.views.*
import org.koin.androidx.compose.getViewModel

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val viewModel = getViewModel<RemoteControlViewModel>()

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
    val height = 6.dp
    val states = listOf(
        DotsState.ShowText(
            text = "ABCDEFGHIGKLMNOPQRSTUVWXYZ",
            textSize = height - 1.dp,
            height = height
        ),
        DotsState.Loading(),
        DotsState.Idle(height)
    )

    val settingsState: MutableState<DotsState> =
        remember { mutableStateOf<DotsState>(states.first()) }
    val bluetoothState: MutableState<DotsState> = remember { mutableStateOf(states[1]) }

    var count = remember { 0 }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        ButtonWrapper(
            modifier = Modifier
                .padding(Padding.Content)
                .width(Size.ButtonAsIcon),
            state = bluetoothState
        ) {
            ActionButton(
                icon = R.drawable.ic_baseline_bluetooth_searching_24,
                onClick = bluetoothOnClick
            )
        }

        ButtonWrapper(
            modifier = Modifier
                .padding(Padding.Content)
                .width(Size.ButtonAsIcon),
            state = settingsState
        ) {
            ActionButton(
                icon = R.drawable.ic_baseline_settings_24,
                onClick = {
                    settingsState.value = states[count % 3]
                    count++
                    settingsOnClick?.invoke()
                }
            )
        }
    }
}

@Composable
private fun ButtonWrapper(
    modifier: Modifier,
    state: State<DotsState>,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlatLoadingWithContent(state = state)

        Spacer(modifier = Modifier.height(4.dp))

        content()
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