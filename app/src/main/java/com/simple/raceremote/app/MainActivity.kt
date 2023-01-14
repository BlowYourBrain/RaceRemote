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
import com.simple.raceremote.features.remote_control.presentation.IRemoteDeviceFinding
import com.simple.raceremote.utils.bluetooth.enableBluetooth
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 40
        private const val SELECT_DEVICE_REQUEST_CODE = 100500
    }

    private val actionsViewModel: ActionsViewModel by viewModel()
    private val remoteDeviceFinding: IRemoteDeviceFinding by inject<IRemoteDeviceFinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        hideSystemBars()
        setupViewModel()

        setContent {
            App(onEnableBluetoothAction = { enableBluetooth(REQUEST_ENABLE_BT, this) })
        }
    }

    private fun setupWindow() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenResumed {
            actionsViewModel.onActionCLick.filterNotNull().collect { remoteDevice ->
                remoteDeviceFinding.findBluetoothDevices(
                    this@MainActivity,
                    remoteDevice,
                    SELECT_DEVICE_REQUEST_CODE
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
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)

                    debug("found device $deviceToPair", "fuck")
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
