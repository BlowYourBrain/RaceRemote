package com.simple.raceremote.screens.remote_control.presentation.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simple.raceremote.R
import com.simple.raceremote.screens.bluetooth_devices.presentation.BluetoothEntity
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.views.BluetoothItemCard
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.ui.views.FlatLoadingWithContent

@Composable
fun BluetoothContentSidePanel(
    isRefreshing: State<Boolean>,
    entities: State<List<BluetoothEntity>>
) {
    val height = 14.dp
    val idleState = remember { DotsState.Idle(height) }
    val loadingState = remember {
        DotsState.Loading(height = height)
    }
    val dotsState = remember {
        mutableStateOf(if (isRefreshing.value) loadingState else idleState)
    }

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