package com.simple.raceremote.features.remote_control.presentation

import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.WifiDeviceFilter
import android.content.Context
import android.content.IntentSender
import androidx.core.app.ActivityCompat
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice
import com.simple.raceremote.utils.debug
import java.util.concurrent.Executor

class RemoteDeviceConnection {

    private val bluetoothDeviceFilter: BluetoothDeviceFilter by lazy {
        BluetoothDeviceFilter.Builder()
            .build()
    }

    private val wifiDeviceFilter: WifiDeviceFilter by lazy {
        WifiDeviceFilter.Builder()
            .build()
    }

    fun chooseRemoteDevice(
        activity: Activity,
        remoteDevice: RemoteDevice,
        requestCode: Int
    ) {
        val deviceManager =
            activity.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
                ?: return

        val filter = when (remoteDevice) {
            RemoteDevice.WIFI -> wifiDeviceFilter
            RemoteDevice.Bluetooth -> bluetoothDeviceFilter
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

}