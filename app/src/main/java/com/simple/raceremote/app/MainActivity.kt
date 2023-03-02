package com.simple.raceremote.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.RemoteDeviceConnection
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.BluetoothResultHandler
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.HandlerList
import com.simple.raceremote.features.remote_control.utils.activity_result_handler.WiFiActivityResultHandler
import com.simple.raceremote.utils.bluetooth.getBluetoothPermissions
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
                    RemoteDevice.Bluetooth.requestCode
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

        //todo remove it
        addHandler(
            WiFiActivityResultHandler(
                onSuccess = { scanResult ->
                    actionsViewModel.openEnterPasswordDialog(
                        scanResult.wifiSsid.toString().removeSurrounding("\"")
                    )
                }
            )
        )
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityResultHandler.handle(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}