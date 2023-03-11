package com.simple.raceremote.features.remote_control.presentation.view

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.RemoteControlViewModel
import com.simple.raceremote.features.remote_control.presentation.model.Action
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.ui.theme.getPalette
import com.simple.raceremote.ui.views.ActionButton
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.ui.views.FlatLoadingWithContent
import com.simple.raceremote.ui.views.Orientation
import com.simple.raceremote.ui.views.Slider
import org.koin.androidx.compose.getViewModel

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier
) {
    val actionsViewModel = getViewModel<ActionsViewModel>()
    val remoteControlViewModel = getViewModel<RemoteControlViewModel>()

    val actions = actionsViewModel.actions.collectAsState(initial = emptyList())

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
                    onClick = { action.onClick() },
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
    Row {
        ControllerWrapper(modifier = Modifier.weight(1f)) {
            Slider() {
                horizontalSlider?.invoke(it)
            }
        }

        ControllerWrapper(modifier = Modifier.weight(1f)) {
            Slider(orientation = Orientation.Vertical) {
                verticalSlider?.invoke(it)
            }
        }
    }
}

@Composable
private fun ControllerWrapper(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = remember { CornerShapes.HugeItem }
    val border = remember { BorderStroke(2.dp, getPalette(true).customColors.outline) }

    Box(
        modifier
            .padding(Padding.Content)
            .border(border, shape)
            .clip(shape)
    ) {
        content.invoke()
    }
}
