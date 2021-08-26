package com.simple.raceremote.screens

import androidx.lifecycle.ViewModel
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.data.MockDataProvider
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.IBluetoothDevicesDiscoveryController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private val source = BluetoothHelper

class BluetoothDevicesViewModel : ViewModel() {

    //TODO провайдить через DI
    private val controller: IBluetoothDevicesDiscoveryController = source
    private val repository: IBluetoothItemsProvider = source
    private val _isRefreshing = MutableStateFlow(false)

    val items: Flow<List<BluetoothItem>> = repository.bluetoothDevices
    val isRefreshing: Flow<Boolean> = _isRefreshing.asStateFlow()

    init {
        startFinding()
    }

    fun toggleRefreshing() = if (_isRefreshing.value) stopFinding() else startFinding()

    override fun onCleared() {
        super.onCleared()
        stopFinding()
    }

    private fun startFinding() {
        _isRefreshing.tryEmit(true)
        controller.findBluetoothDevices()
    }

    private fun stopFinding() {
        _isRefreshing.tryEmit(false)
        controller.stopFindingBluetoothDevices()
    }
}