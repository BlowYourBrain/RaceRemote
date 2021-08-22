package com.simple.raceremote.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding

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
                BluetoothItemCard(item)
            }
        }
    }
}

@Composable
private fun BluetoothItemCard(data: BluetoothItem) {
    Card(
        modifier = Modifier.padding(Padding.Content),
        elevation = Elevation.onSurface,
        shape = CornerShapes.Card,
    ) {
        Text(
            text = data.name
        )
    }

}