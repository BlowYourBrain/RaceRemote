package com.simple.raceremote.utils

import android.Manifest.permission.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService

private const val REQUEST_ENABLE_BT = 40

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

fun enableBluetooth(activity: Activity) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    ActivityCompat.startActivityForResult(
        activity,
        enableBtIntent,
        REQUEST_ENABLE_BT,
        null
    )
}

fun getBluetoothPermissions() = arrayOf(BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION)

fun Context.isBluetoothEnabled(): Boolean = getBluetoothAdapter()?.isEnabled ?: false

fun Context.getBluetoothAdapter(): BluetoothAdapter? =
    (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter