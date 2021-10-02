package com.simple.raceremote.data

import android.content.Context

interface IBluetoothDevicesDiscoveryController {

    fun findBluetoothDevices(context: Context)

    fun stopFindingBluetoothDevices(context: Context)

}