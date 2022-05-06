package com.simple.raceremote.utils.bluetooth

import kotlinx.coroutines.flow.Flow

interface IBluetoothDevicesProvider {

    val bluetoothDevicesDiscovery: Flow<Boolean>
    val bluetoothDevices: Flow<List<BluetoothDevice>>
}

data class BluetoothDevice(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean
)
