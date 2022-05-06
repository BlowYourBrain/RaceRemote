package com.simple.raceremote.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simple.raceremote.utils.bluetooth.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val bluetoothHelper: BluetoothHelper by inject()
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted -> //todo create implementation later
    }

    private val gameController = GameController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        hideSystemBars()
        bluetoothHelper.bind(this)
        setContent { App() }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (gameController.dispatchGenericMotionEvent(ev)){
            return true
        }

        return super.dispatchGenericMotionEvent(ev)
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun requestBluetoothPermissions() {
        permissionLauncher.launch(getBluetoothPermissions())
    }
}