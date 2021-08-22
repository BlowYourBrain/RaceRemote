package com.simple.raceremote.navigation

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simple.raceremote.data.MockDataProvider
import com.simple.raceremote.screens.BluetoothDevicesScreen
import com.simple.raceremote.screens.RemoteControlScreen
import com.simple.raceremote.utils.map

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startScreen: Screens
) {
    val navController = rememberNavController()

    Scaffold(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = startScreen.name
        ) {
            composable(Screens.RemoteControl.name) {
                RemoteControlScreen(navController = navController)
            }

            composable(Screens.BluetoothDevices.name) {
                BluetoothDevicesScreen(
                    data = MockDataProvider.getBluetoothDevices().map()
                )
            }
        }
    }
}
