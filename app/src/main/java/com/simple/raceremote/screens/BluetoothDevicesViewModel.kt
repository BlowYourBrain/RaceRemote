package com.simple.raceremote.screens

import androidx.lifecycle.ViewModel
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.data.MockDataProvider
import com.simple.raceremote.utils.BluetoothHelper
import kotlinx.coroutines.flow.Flow

class BluetoothDevicesViewModel() : ViewModel() {

    init {
        BluetoothHelper.findBluetoothDevices()
    }

    //TODO провайдить через DI
    private val repository: IBluetoothItemsProvider = BluetoothHelper

    val items: Flow<List<BluetoothItem>> = repository.bluetoothDevices

    override fun onCleared() {
        super.onCleared()
        BluetoothHelper.stopFindingBluetoothDevices()
    }
}