package com.simple.raceremote.features.bluetooth_devices.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.WifiDeviceFilter
import android.content.Context
import android.content.IntentSender
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.utils.bluetooth.enableBluetooth
import com.simple.raceremote.utils.bluetooth.hasBluetooth
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions
import com.simple.raceremote.utils.bluetooth.isBluetoothEnabled
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.singleEventChannel
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
class BluetoothDevicesViewModel(
    private val context: Context,
    private val bluetoothConnection: IBluetoothConnection
) : ViewModel() {

    companion object {
        private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"
        private const val TEXT_SIZE = 5
    }

    private val bluetoothDeviceFilter: BluetoothDeviceFilter by lazy {
        BluetoothDeviceFilter.Builder()
            .build()
    }

    private val wifiDeviceFilter: WifiDeviceFilter by lazy {
        WifiDeviceFilter.Builder()
            .build()
    }

    private val uuid = UUID.fromString(UUID_STR)
    private val _bluetoothPermissions = singleEventChannel<Unit>()
    private val _bluetoothConnectionState = singleEventChannel<DotsState>(DotsState.Idle())

    val requestBluetoothPermissions: Flow<Unit> = _bluetoothPermissions.receiveAsFlow()
    val bluetoothConnectionState: Flow<DotsState> = _bluetoothConnectionState.receiveAsFlow()

    fun findBluetoothDevices(
        activity: Activity,
        remoteDevice: ActionsViewModel.RemoteDevice,
        requestCode: Int
    ) {
        if (!checkBluetooth(activity, requestCode)) return

        val deviceManager =
            activity.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
                ?: return

        val filter = when (remoteDevice) {
            ActionsViewModel.RemoteDevice.WIFI -> wifiDeviceFilter
            ActionsViewModel.RemoteDevice.Bluetooth -> bluetoothDeviceFilter
        }

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(filter)
            .setSingleDevice(false)
            .build()

        val callback = object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(intentSender: IntentSender) = Unit

            // Called when a device is found. Launch the IntentSender so the user
            // can select the device they want to pair with.
            override fun onAssociationPending(intentSender: IntentSender) {
                intentSender ?: return

                debug(
                    "send intent finding bluetooth",
                    "fuck"
                )
                ActivityCompat.startIntentSenderForResult(
                    activity,
                    intentSender,
                    requestCode,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                debug("onAssociationCreated", "fuck")
                // The association is created.
            }

            override fun onFailure(errorMessage: CharSequence?) {
                // Handle the failure.
                debug("onFailure", "fuck")
            }
        }

        val executor: Executor = Executor { it.run() }
        deviceManager.associate(pairingRequest, executor, callback)
    }

    /**
     * @param activity - activity
     * @param requestCode - respond to enable bluetooth event if bluetooth is disabled on device
     * @param macAddress - bluetooth device macAddress
     * */
    fun connect(activity: Activity, requestCode: Int, macAddress: String) {
        if (!activity.hasBluetoothPermissions()) return

        viewModelScope.launch {
            val connection = bluetoothConnection.connectWithDevice(context, macAddress, uuid)

            when (connection) {
                is IBluetoothConnection.Connection.Success -> {
                    val deviceName = connection.name
                    val state =
                        deviceName?.let { DotsState.ShowText(it, TEXT_SIZE.dp) } ?: DotsState.Idle()

                    _bluetoothConnectionState.send(state)
                }
                is IBluetoothConnection.Connection.Error -> {
                    // TODO: notify user
                    debug("connection failed")
                }
            }

        }
    }

    /**
     * @return false if has any problems with bluetooth
     * */
    private fun checkBluetooth(activity: Activity, requestCode: Int): Boolean = with(context) {
        if (!hasBluetooth()) {
            //todo notify device has not bluetooth
            debug("device has no bluetooth")
            return false
        }

        if (!hasBluetoothPermissions()) {
            _bluetoothPermissions.trySend(Unit)
            return false
        }

        if (!isBluetoothEnabled()) {
            enableBluetooth(requestCode, activity)
            return false
        }

        return true
    }
}