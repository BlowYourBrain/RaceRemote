package com.simple.raceremote.features.bluetooth_devices.presentation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.views.BluetoothItemCard
import com.simple.raceremote.ui.views.NavigationPanel
import com.simple.raceremote.ui.views.RoundActionButton
import org.koin.androidx.compose.getViewModel

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
    val viewModel = getViewModel<BluetoothDevicesViewModel>()
    Content(viewModel, onBackClick = onBackClick)
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