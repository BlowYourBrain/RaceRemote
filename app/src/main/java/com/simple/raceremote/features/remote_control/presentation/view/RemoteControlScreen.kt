package com.simple.raceremote.features.remote_control.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.simple.raceremote.features.bluetooth_devices.presentation.BluetoothDevicesViewModel
import com.simple.raceremote.features.remote_control.presentation.Action
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.RemoteControlViewModel
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.ui.views.ActionButton
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.ui.views.FlatLoadingWithContent
import com.simple.raceremote.ui.views.Orientation
import com.simple.raceremote.ui.views.Slider
import org.koin.androidx.compose.getViewModel

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    sidePanelContent: MutableState<@Composable () -> Unit>
) {
    val actionsViewModel = getViewModel<ActionsViewModel>()
    val remoteControlViewModel = getViewModel<RemoteControlViewModel>()
    val bluetoothDevicesViewModel = getViewModel<BluetoothDevicesViewModel>()

    val actions = actionsViewModel.actions.collectAsState(initial = emptyList())
    val entities = bluetoothDevicesViewModel.items.collectAsState(initial = emptyList())
    val isRefreshing = bluetoothDevicesViewModel.isRefreshing.collectAsState(initial = false)

    sidePanelContent.value = @Composable { BluetoothContentSidePanel(isRefreshing, entities) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Actions(actions = actions.value)
        Controllers(
            { remoteControlViewModel.updateSteeringWheel(it) },
            { remoteControlViewModel.updateMovement(it) },
        )
    }
}

@Composable
fun Actions(
    modifier: Modifier = Modifier,
    actions: List<Action>,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        actions.forEach { action ->
            ButtonWrapper(
                modifier = Modifier
                    .padding(Padding.Content)
                    .width(Size.ButtonAsIcon),
                state = action.state
            ) {
                ActionButton(
                    icon = action.icon,
                    onClick = action.onClick
                )
            }
        }
    }
}

@Composable
private fun ButtonWrapper(
    modifier: Modifier,
    state: DotsState,
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
