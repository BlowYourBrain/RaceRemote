package com.simple.raceremote.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.views.NavigationPanel

private const val ROWS = 2

data class BluetoothItem(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean
)

@Composable
fun BluetoothDevicesScreen(
    navController: NavHostController,
    onBackClick: (() -> Unit)? = null
) {
    Content(viewModel(), onBackClick = onBackClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    viewModel: BluetoothDevicesViewModel,
    onBackClick: (() -> Unit)?
) {
    val bluetoothItems by viewModel.items.collectAsState(emptyList())
    Row() {
        LazyVerticalGrid(
            modifier = Modifier.weight(3f),
            cells = GridCells.Fixed(ROWS)
        ) {
            items(items = bluetoothItems) {
                BluetoothItemCard(
                    modifier = Modifier.padding(Padding.ListSpace),
                    entity = it
                )
            }

        }
        NavigationPanel(
            modifier = Modifier.weight(1f),
            onBackClick = { onBackClick?.invoke() }
        )
    }
}

@Preview
@Composable
private fun BluetoothItemCardPreview() {
    BluetoothItemCard(entity = BluetoothItem("hello", "world", true))
}

@Composable
private fun BluetoothItemCard(modifier: Modifier = Modifier, entity: BluetoothItem) {
    Card(
        modifier = modifier,
        elevation = Elevation.onSurface,
        shape = CornerShapes.SmallItem,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                modifier = Modifier.padding(Padding.Content),
                text = entity.name,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.body1
            )
            if (entity.isPaired) {
                Image(
                    modifier = Modifier.padding(Padding.Content),
                    painter = painterResource(id = R.drawable.ic_baseline_link_24),
                    contentDescription = null
                )
            }
        }

    }
}