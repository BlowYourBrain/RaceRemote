package com.simple.raceremote.features.remote_control.presentation.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simple.raceremote.R
import com.simple.raceremote.features.bluetooth_devices.presentation.BluetoothEntity
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.views.BluetoothItemCard
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.ui.views.FlatLoadingWithContent

private const val DOTS_LOADING_HEIGHT = 14

@Composable
fun BluetoothContentSidePanel(
    isRefreshing: State<Boolean>,
    entities: State<List<BluetoothEntity>>
) {
    val height = DOTS_LOADING_HEIGHT.dp
    val idleState = remember { DotsState.Idle(height) }
    val loadingState = remember { DotsState.Loading(height = height) }

    val dotsState = remember { mutableStateOf(if (isRefreshing.value) loadingState else idleState) }

    Column() {
        Header()
        FlatLoadingWithContent(state = dotsState)
        LazyColumn() {
            items(items = entities.value) {
                BluetoothItemCard(
                    modifier = Modifier.padding(Padding.ListSpace),
                    entity = it
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Text(
        modifier = Modifier.padding(Padding.Content),
        text = stringResource(id = R.string.find_bluetooth_devices),
        style = MaterialTheme.typography.h6
    )
}