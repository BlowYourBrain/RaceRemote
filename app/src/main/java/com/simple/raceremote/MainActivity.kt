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
import com.simple.raceremote.navigation.AppNavHost
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.screens.remote_control.presentation.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val bluetoothHelper: BluetoothHelper by inject()
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted -> //todo create implementation later
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        bluetoothHelper.bind(this)
        setContent { App() }
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

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun requestBluetoothPermissions() {
        permissionLauncher.launch(getBluetoothPermissions())
    }
}