package com.simple.raceremote

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.insets.ProvideWindowInsets
import com.simple.raceremote.navigation.AppNavHost
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.screens.remote_control.presentation.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.bluetooth.hasBluetooth
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions


@Preview
@Composable
fun AppPreview() {
    App()
}

@Composable
fun App() {
    ProvideWindowInsets {
        RaceRemoteTheme(darkTheme = true) {
            AppNavHost(startScreen = getStartScreen())
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

@Preview
@Composable
private fun ActionsPreview() {
    Actions(Modifier, null)
}