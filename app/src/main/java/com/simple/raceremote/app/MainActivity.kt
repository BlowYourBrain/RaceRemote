package com.simple.raceremote.app

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.CompanionDeviceManager
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
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice
import com.simple.raceremote.utils.bluetooth.getBluetoothPermissions
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val actionsViewModel: ActionsViewModel by viewModel()
    private val remoteDeviceConnection: RemoteDeviceConnection by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        hideSystemBars()
        setupViewModel()

        setContent {
            App()
        }
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

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RemoteDevice.Bluetooth.requestCode -> when (resultCode) {
                Activity.RESULT_OK -> {
                    val macAddress = retrieveBluetoothMacAddress(data) ?: kotlin.run {
                        debug("device macAddress is null")
                        return
                    }

                    debug("found device with address $macAddress")
                    actionsViewModel.connectBluetooth(macAddress)
                }
            }

            RemoteDevice.WIFI.requestCode -> {
                debug("found wifi device")
                actionsViewModel.connectWifi()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun retrieveBluetoothMacAddress(data: Intent?): String? {
        return data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)?.address
    }
}