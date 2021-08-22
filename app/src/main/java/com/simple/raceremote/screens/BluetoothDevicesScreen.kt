package com.simple.raceremote.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding

private const val ROWS = 2

data class BluetoothItem(
    val name: String,
    val macAddress: String
)

@Composable
fun BluetoothDevicesScreen(navController: NavHostController) {
    Content(viewModel())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    viewModel: BluetoothDevicesViewModel
) {
    val bluetoothItems by viewModel.items.collectAsState(emptyList())

    LazyVerticalGrid(cells = GridCells.Fixed(ROWS)) {
        items(items = bluetoothItems) {
            BluetoothItemCard(
                modifier = Modifier.padding(Padding.ListSpace),
                data = it
            )
        }

    }
}

@Composable
private fun BluetoothItemCard(modifier: Modifier = Modifier, data: BluetoothItem) {
    Card(
        modifier = modifier,
        elevation = Elevation.onSurface,
        shape = CornerShapes.SmallItem,
    ) {
        Text(
            modifier = Modifier.padding(Padding.Content),
            text = data.name,
            style = MaterialTheme.typography.body1
        )
    }
}