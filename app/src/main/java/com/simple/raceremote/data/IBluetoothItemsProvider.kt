package com.simple.raceremote.data

import kotlinx.coroutines.flow.Flow

interface IBluetoothItemsProvider {

    val bluetoothDevices: Flow<List<BluetoothItem>>
}

data class BluetoothItem(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean
)
