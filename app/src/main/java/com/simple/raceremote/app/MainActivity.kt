package com.simple.raceremote.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simple.raceremote.features.remote_control.presentation.RemoteControlViewModel
import com.simple.raceremote.utils.bluetooth.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted -> //todo create implementation later
    }
    private val remoteControlViewModel: RemoteControlViewModel by inject()
    private val bluetoothHelper: BluetoothHelper by inject()
    private val gameController: GameController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        hideSystemBars()
        bluetoothHelper.bind(this)
        setupGameController()
        setContent { App() }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (gameController.dispatchGenericMotionEvent(ev)) {
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

    private fun setupGameController() = gameController.run {
        movementUpdateCallback = { remoteControlViewModel.updateMovement(it) }
        steeringWheelUpdateCallback = { remoteControlViewModel.updateSteeringWheel(it) }
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