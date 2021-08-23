package com.simple.raceremote.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import com.simple.raceremote.permissions.hasPermission

fun Context.hasBluetoothPermissions(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_CONNECT) && hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        hasPermission(Manifest.permission.BLUETOOTH) && hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
    }

fun isBluetoothEnabled(): Boolean = getBluetoothAdapter()?.isEnabled == false

fun enableBluetooth(activity: Activity) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    ActivityCompat.startActivityForResult(
        activity,
        enableBtIntent,
        BluetoothHelper.REQUEST_ENABLE_BT,
        null
    )
}

fun getBluetoothAdapter() = BluetoothAdapter.getDefaultAdapter()