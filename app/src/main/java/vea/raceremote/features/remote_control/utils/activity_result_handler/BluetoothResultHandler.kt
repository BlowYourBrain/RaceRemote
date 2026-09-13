package vea.raceremote.features.remote_control.utils.activity_result_handler

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.CompanionDeviceManager
import android.content.Intent
import vea.raceremote.features.remote_control.presentation.model.RemoteDevice
import vea.raceremote.utils.debug

class BluetoothResultHandler(
    onSuccess: (BluetoothDevice) -> Unit = {},
    onFailure: () -> Unit = {}
) : ActivityResultHandler<BluetoothDevice>(onSuccess, onFailure) {

    override fun handle(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != RemoteDevice.Bluetooth.requestCode) return false

        if (requestCode == Activity.RESULT_OK) {
            val bluetoothDevice = retrieveBluetoothDevice(data) ?: return false

            onSuccess.invoke(bluetoothDevice)
        } else {
            onFailure.invoke()
        }

        return true
    }

    private fun retrieveBluetoothDevice(data: Intent?): BluetoothDevice? {
        return data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)
    }
}