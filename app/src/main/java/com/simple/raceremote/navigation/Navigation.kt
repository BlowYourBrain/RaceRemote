package com.simple.raceremote.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.statusBarsHeight
import com.simple.raceremote.screens.bluetooth_devices.presentation.BluetoothDevicesScreen
import com.simple.raceremote.screens.BluetoothPermissionRationale
import com.simple.raceremote.screens.no_bluetooth.NoBluetoothScreen
import com.simple.raceremote.screens.remote_control.presentation.RemoteControlScreen
import com.simple.raceremote.ui.views.SidePanel
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.compose.getViewModel

private const val CONTENT_TOP_PADDING = 12
private const val CONTENT_BOTTOM_PADDING = 24

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startScreen: Screens
) {
    val navController = rememberNavController()
    val isSidePanelOpen = remember { mutableStateOf(false) }
    val sidePanelContent: MutableState<@Composable () -> Unit> =
        remember { mutableStateOf(value = {}) }

    Scaffold(modifier = modifier) {
        SidePanel(
            isOpened = isSidePanelOpen,
            itemProvider = sidePanelContent
        ){
            Column {
                Spacer(modifier = Modifier.statusBarsHeight())

                Box(
                    Modifier.padding(
                        top = CONTENT_TOP_PADDING.dp,
                        bottom = CONTENT_BOTTOM_PADDING.dp
                    )
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startScreen.name
                    ) {
                        composable(Screens.NoBluetooth.name) { NoBluetoothScreen(navController = navController) }
                        composable(Screens.RemoteControl.name) {
                            RemoteControlScreen(
                                isSidePanelOpen = isSidePanelOpen,
                                sidePanelContent = sidePanelContent
                            )
                        }
                        composable(Screens.BluetoothDevices.name) {
                            BluetoothDevicesScreen(navController = navController) {
                                navController.popBackStack()
                            }
                        }
                        composable(Screens.BluetoothPermissionsRationale.name) {
                            BluetoothPermissionRationale(
                                onApply = {
                                    navController.navigate(
                                        route = Screens.RemoteControl.name,
                                        navOptions = NavOptions.Builder()
                                            .setLaunchSingleTop(true)
                                            .build()
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.navigationBarsHeight())
            }
        }
    }
}