package com.simple.raceremote.utils.bluetooth

import android.content.Context

interface IBluetoothDevicesDiscoveryController {

    fun findBluetoothDevices(context: Context)

    fun stopFindingBluetoothDevices(context: Context)
}