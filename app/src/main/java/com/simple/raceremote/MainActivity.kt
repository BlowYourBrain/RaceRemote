package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.simple.raceremote.data.IBluetoothBroadcastReceiver
import com.simple.raceremote.navigation.AppNavHost
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.screens.remote_control.presentation.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val bluetoothBroadcastReceiver: IBluetoothBroadcastReceiver by inject()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted -> //todo create implementation later
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothBroadcastReceiver.registerReceiver(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isBluetoothEnabled()) {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
            }
        } else {
            //todo сделать пояснение для пользователя, а не открывать принудительно
            enableBluetooth(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothBroadcastReceiver.unregisterReceiver(this)
    }

    private fun requestBluetoothPermissions() {
        permissionLauncher.launch(getBluetoothPermissions())
    }
}

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