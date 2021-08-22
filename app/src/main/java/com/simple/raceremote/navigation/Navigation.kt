package com.simple.raceremote.navigation

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.simple.raceremote.data.MockDataProvider
import com.simple.raceremote.screens.BluetoothDevicesScreen
import com.simple.raceremote.screens.RemoteControlScreen


@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startScreen: Screens
) {
    Scaffold(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = startScreen.name
        ) {
            composable(Screens.RemoteControl.name) {
                RemoteControlScreen()
            }

            composable(Screens.BluetoothDevices.name) {
                BluetoothDevicesScreen(data = MockDataProvider.getBluetoothDevices())
            }
        }
    }
}
