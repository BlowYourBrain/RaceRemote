package com.simple.raceremote.utils.bluetooth

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.simple.raceremote.utils.hasPermission

private val BLUETOOTH_PERMISSIONS = listOf(BLUETOOTH, BLUETOOTH_ADMIN)

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

fun enableBluetooth(requestCode: Int, activity: Activity) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    ActivityCompat.startActivityForResult(
        activity,
        enableBtIntent,
        requestCode,
        null
    )
}

fun getBluetoothPermissions(): List<String> = BLUETOOTH_PERMISSIONS

fun Context.isBluetoothEnabled(): Boolean = getBluetoothAdapter()?.isEnabled ?: false

fun Context.hasBluetooth(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

@SuppressLint("MissingPermission")
fun Context.getBluetoothAdapter(): BluetoothAdapter? =
    if (hasBluetoothPermissions())
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    else
        null