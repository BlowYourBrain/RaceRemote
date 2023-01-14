package com.simple.raceremote.features.remote_control.presentation

import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.WifiDeviceFilter
import android.content.Context
import android.content.IntentSender
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.simple.raceremote.utils.debug
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

interface IRemoteDeviceFinding {

    fun findBluetoothDevices(
        activity: Activity,
        remoteDevice: ActionsViewModel.RemoteDevice,
        requestCode: Int
    )
}

class RemoteDeviceFinding() : IRemoteDeviceFinding {

    private val bluetoothDeviceFilter: BluetoothDeviceFilter by lazy {
        BluetoothDeviceFilter.Builder()
            .build()
    }

    private val wifiDeviceFilter: WifiDeviceFilter by lazy {
        WifiDeviceFilter.Builder()
            .build()
    }

    override fun findBluetoothDevices(
        activity: Activity, remoteDevice: ActionsViewModel.RemoteDevice,
        requestCode: Int
    ) {
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
                startIntentSenderForResult(
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
}