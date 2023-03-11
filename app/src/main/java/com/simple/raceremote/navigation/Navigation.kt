package com.simple.raceremote.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simple.raceremote.features.BluetoothPermissionRationale
import com.simple.raceremote.features.remote_control.presentation.view.RemoteControlScreen
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions

@Composable
fun AppScaffold() {
    AppScaffold(startScreen = getStartScreen())
}

@Composable
fun AppScaffold(
    startScreen: Screens,
) {
    val navController = rememberNavController()

    Surface(

    ) {
        NavHost(
            navController = navController,
            startDestination = startScreen.name,
            modifier = Modifier.padding(WindowInsets.safeContent.asPaddingValues()),
        ) {
            composable(Screens.RemoteControl.name) {
                RemoteControlScreen()
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
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun getStartScreen(): Screens = with(LocalContext.current) {
    when {
        hasBluetoothPermissions() -> Screens.RemoteControl
        else -> Screens.BluetoothPermissionsRationale
    }
}