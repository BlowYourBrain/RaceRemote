package com.simple.raceremote.utils

import android.Manifest.permission.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat

fun Context.hasBluetoothPermissions(): Boolean {
    var hasPermissions = true
    val permissions = getBluetoothPermissions()

    for (perm in permissions) {
        if (!hasPermission(perm)) {
            hasPermissions = false
            break
        }
    }
    return hasPermissions
}

fun getBluetoothPermissions() = arrayOf(BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION)


fun isBluetoothEnabled(): Boolean = getBluetoothAdapter().isEnabled

fun enableBluetooth(activity: Activity) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    ActivityCompat.startActivityForResult(
        activity,
        enableBtIntent,
        BluetoothHelper.REQUEST_ENABLE_BT,
        null
    )
}

fun getBluetoothAdapter(): BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()