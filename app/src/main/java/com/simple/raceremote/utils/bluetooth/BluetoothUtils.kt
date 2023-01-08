package com.simple.raceremote.utils.bluetooth

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import com.simple.raceremote.utils.hasPermission

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

fun Context.hasBluetooth(): Boolean = getBluetoothAdapter() != null
