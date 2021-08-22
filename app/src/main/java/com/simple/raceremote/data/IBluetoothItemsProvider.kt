package com.simple.raceremote.data

import com.simple.raceremote.screens.BluetoothItem
import kotlinx.coroutines.flow.Flow

interface IBluetoothItemsProvider {

    val bluetoothDevices: Flow<List<BluetoothItem>>
}