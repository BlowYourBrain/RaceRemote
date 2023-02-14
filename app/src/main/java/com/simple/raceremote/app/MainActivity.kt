package com.simple.raceremote.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel.Companion.REQUEST_BLUETOOTH_PERMISSIONS
import com.simple.raceremote.features.remote_control.presentation.RemoteDeviceConnection
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.BluetoothResultHandler
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.HandlerList
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.WiFiActivityResultHandler
import com.simple.raceremote.utils.bluetooth.getBluetoothPermissions
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val activityResultHandler = HandlerList()
    private val actionsViewModel: ActionsViewModel by viewModel()
    private val remoteDeviceConnection: RemoteDeviceConnection by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        hideSystemBars()
        setupViewModel()
        setupActivityHandlerList()

        setContent { App() }
    }

    private fun setupWindow() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenResumed {
            actionsViewModel.remoteDevice.collect { remoteDevice ->
                remoteDeviceConnection.chooseRemoteDevice(
                    this@MainActivity,
                    remoteDevice,
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            actionsViewModel.requestBluetoothPermissions.collect {
                requestPermissions(
                    getBluetoothPermissions().toTypedArray(),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    private fun setupActivityHandlerList() = with(activityResultHandler) {
        addHandler(
            BluetoothResultHandler(
                onSuccess = { bluetoothDevice ->
                    actionsViewModel.connectBluetooth(bluetoothDevice.address)
                }
            )
        )

        addHandler(
            WiFiActivityResultHandler(
                onSuccess = { scanResult ->
                    actionsViewModel.connectWifi(scanResult.wifiSsid.toString())
                }
            )
        )
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityResultHandler.handle(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}