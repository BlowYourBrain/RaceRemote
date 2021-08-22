package com.simple.raceremote.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding

data class TwoColumnRows(
    val left: BluetoothItem,
    val right: BluetoothItem? = null
)

data class BluetoothItem(
    val name: String,
    val macAddress: String
)

private const val HALF = 0.5f

@Composable
fun BluetoothDevicesScreen(data: List<TwoColumnRows>) {
    LazyColumn(
        state = LazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Padding.Content)
    ) {
        itemsIndexed(data) { pos, item ->
            Row(Modifier.fillMaxSize()) {
                BluetoothItemCard(
                    modifier = Modifier
                        .fillMaxWidth(HALF)
                        .padding(Padding.ListSpace),
                    data = item.left
                )
                item.right?.let {
                    BluetoothItemCard(
                        modifier = Modifier
                            .fillMaxWidth(HALF)
                            .weight(1f)
                            .padding(Padding.ListSpace),
                        data = it
                    )
                }
            }
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