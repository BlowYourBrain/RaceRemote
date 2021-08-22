package com.simple.raceremote.screens

import androidx.lifecycle.ViewModel
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.data.MockDataProvider
import kotlinx.coroutines.flow.Flow

class BluetoothDevicesViewModel() : ViewModel() {

    //TODO провайдить через DI
    private val repository: IBluetoothItemsProvider = MockDataProvider

    val items: Flow<List<BluetoothItem>> = repository.bluetoothDevices
}