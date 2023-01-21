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
import com.simple.raceremote.features.remote_control.presentation.BluetoothDevicesViewModel
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.utils.bluetooth.enableBluetooth
import com.simple.raceremote.utils.bluetooth.getBluetoothPermissions
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 40
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 50
        private const val SELECT_DEVICE_REQUEST_CODE = 100500
    }

    private val actionsViewModel: ActionsViewModel by viewModel()
    private val bluetoothViewModel: BluetoothDevicesViewModel by viewModel()

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
            actionsViewModel.onActionCLick.collect { remoteDevice ->
                bluetoothViewModel.findBluetoothDevices(
                    this@MainActivity, remoteDevice, SELECT_DEVICE_REQUEST_CODE
                )
            }
        }

        lifecycleScope.launchWhenResumed {
            bluetoothViewModel.requestBluetoothPermissions.collect {
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
            SELECT_DEVICE_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    val macAddress = retrieveBluetoothMacAddress(data) ?: kotlin.run {
                        debug("device macAddress is null")
                        return
                    }

                    debug("found device with address $macAddress")

                    bluetoothViewModel.connect(this, REQUEST_ENABLE_BT, macAddress)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun retrieveBluetoothMacAddress(data: Intent?): String? {
        return data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)?.address
    }
}