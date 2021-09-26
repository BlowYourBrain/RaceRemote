package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import com.simple.raceremote.navigation.AppNavHost
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.screens.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.enableBluetooth
import com.simple.raceremote.utils.getBluetoothPermissions
import com.simple.raceremote.utils.hasBluetoothPermissions
import com.simple.raceremote.utils.isBluetoothEnabled

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BluetoothHelper.registerReceiver(this)

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
        BluetoothHelper.unregisterReceiver(this)
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
    if (hasBluetoothPermissions()) {
        Screens.RemoteControl
    } else {
        Screens.BluetoothPermissionsRationale
    }
}

@Preview
@Composable
private fun ActionsPreview() {
    Actions(Modifier, null)
}