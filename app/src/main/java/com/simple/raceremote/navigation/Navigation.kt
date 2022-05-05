package com.simple.raceremote.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simple.raceremote.features.BluetoothPermissionRationale
import com.simple.raceremote.features.bluetooth_devices.presentation.BluetoothDevicesScreen
import com.simple.raceremote.features.no_bluetooth.NoBluetoothScreen
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.view.RemoteControlScreen
import com.simple.raceremote.ui.views.SidePanel
import com.simple.raceremote.utils.bluetooth.hasBluetooth
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions
import org.koin.androidx.compose.getViewModel

private const val CONTENT_TOP_PADDING = 12
private const val CONTENT_BOTTOM_PADDING = 24

@Composable
fun AppScaffold() {
    val actionsViewModel = getViewModel<ActionsViewModel>()
    val sidePanelContent: MutableState<@Composable () -> Unit> = remember {
        mutableStateOf(value = {})
    }
    val isOpened: State<Boolean> = actionsViewModel.isPanelOpen.collectAsState(initial = false)

    Scaffold() {
        SidePanel(
            isOpened = isOpened,
            updateIsOpened = { actionsViewModel.isOpened(it) },
            itemProvider = sidePanelContent
        ) {
            Column {
                Box(
                    Modifier.padding(
                        top = CONTENT_TOP_PADDING.dp,
                        bottom = CONTENT_BOTTOM_PADDING.dp
                    )
                ) {
                    AppScaffold(
                        startScreen = getStartScreen(),
                        sidePanelContent = sidePanelContent
                    )
                }
            }
        }
    }
}

@Composable
fun AppScaffold(
    startScreen: Screens,
    sidePanelContent: MutableState<@Composable () -> Unit>
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startScreen.name
    ) {
        composable(Screens.NoBluetooth.name) { NoBluetoothScreen(navController = navController) }
        composable(Screens.RemoteControl.name) {
            RemoteControlScreen(
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


@Composable
fun getStartScreen(): Screens = with(LocalContext.current) {
    when {
        !hasBluetooth() -> Screens.NoBluetooth
        hasBluetoothPermissions() -> Screens.RemoteControl
        else -> Screens.BluetoothPermissionsRationale
    }
}