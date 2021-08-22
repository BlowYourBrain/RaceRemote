package com.simple.raceremote.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class BluetoothItem(
    val name: String,
    val macAddress: String
)

@Composable
fun BluetoothDevicesScreen(data: List<BluetoothItem>) {
    Box() {
        LazyColumn(
            state = LazyListState(),
            modifier = Modifier.fillMaxSize()
        ) {
            items(data) { item ->
                Card(item.name)
            }
        }
    }
}

@Composable
private fun Card(name: String) {
    Text(name)
}