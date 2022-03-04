package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.simple.raceremote.utils.bluetooth.*
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