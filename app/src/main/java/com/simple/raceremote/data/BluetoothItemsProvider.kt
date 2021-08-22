package com.simple.raceremote.data

import com.simple.raceremote.screens.BluetoothItem

interface BluetoothItemsProvider {

    fun getBluetoothDevices(): List<BluetoothItem>
}