package com.simple.raceremote.features.remote_control.utils.activity_result_handler

import android.app.Activity
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.net.wifi.ScanResult
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice

class WiFiActivityResultHandler(
    onSuccess: (ScanResult) -> Unit = {},
    onFailure: () -> Unit = {}
) : ActivityResultHandler<ScanResult>(onSuccess, onFailure) {

    override fun handle(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != RemoteDevice.WIFI.requestCode) return false

        if (resultCode == Activity.RESULT_OK) {
            val scanResult = retrieveScanResult(data) ?: return false

            onSuccess.invoke(scanResult)
            return true
        }

        return false
    }

    private fun retrieveScanResult(data: Intent?): ScanResult? {
        return data?.getParcelableExtra<ScanResult>(CompanionDeviceManager.EXTRA_DEVICE)
    }
}