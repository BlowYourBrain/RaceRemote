package com.simple.raceremote.screens.bluetooth_devices.presentation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size
import com.simple.raceremote.views.NavigationPanel
import com.simple.raceremote.views.RoundActionButton

private const val ROTATION_DURATION_MS = 2000
private const val INITIAL_ROTATION = 0f
private const val TARGET_ROTATION = 360f
private const val ROWS = 2

data class BluetoothEntity(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean,
    val onClick: (() -> Unit)? = null
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
    val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
    val bluetoothItems by viewModel.items.collectAsState(emptyList())
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
        initialValue = INITIAL_ROTATION,
        targetValue = TARGET_ROTATION,
        animationSpec = infiniteRepeatable(
            animation = tween(ROTATION_DURATION_MS)
        )
    )

    Row {
        Box(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight()
        ) {
            LazyVerticalGrid(cells = GridCells.Fixed(ROWS)) {
                items(items = bluetoothItems) {
                    BluetoothItemCard(
                        modifier = Modifier.padding(Padding.ListSpace),
                        entity = it
                    )
                }
            }

            if (bluetoothItems.isEmpty() && isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.find_bluetooth_devices),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        NavigationPanel(
            modifier = Modifier.weight(1f),
            onBackClick = { onBackClick?.invoke() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                RoundActionButton(
                    modifier = Modifier.rotate(if (isRefreshing) rotation else INITIAL_ROTATION),
                    icon = R.drawable.ic_baseline_refresh_24
                ) { viewModel.toggleRefreshing() }
            }
        }
    }
}

@Preview
@Composable
private fun BluetoothItemCardPreview() {
    BluetoothItemCard(entity = BluetoothEntity("hello", "world", true))
}

@Composable
private fun BluetoothItemCard(modifier: Modifier = Modifier, entity: BluetoothEntity) {
    Card(
        modifier = modifier.clickable { entity.onClick?.invoke() },
        elevation = Elevation.onSurface,
        shape = CornerShapes.SmallItem,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(modifier = Modifier.padding(Padding.Content)) {
                Text(
                    text = entity.name,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = entity.macAddress,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body1
                )
            }
            Box(
                modifier = Modifier
                    .padding(Padding.Content)
                    .size(Size.Icon)
            ) {
                if (entity.isPaired) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_link_24),
                        contentDescription = null
                    )
                }
            }
        }
    }
}