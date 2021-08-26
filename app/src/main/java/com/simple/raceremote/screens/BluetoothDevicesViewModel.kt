package com.simple.raceremote.screens

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.data.MockDataProvider
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.getBluetoothAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

private val source = BluetoothHelper

class BluetoothDevicesViewModel : ViewModel() {

    //TODO провайдить через DI
    private val controller: IBluetoothDevicesDiscoveryController = source
    private val repository: IBluetoothItemsProvider = source
    private val _isRefreshing = MutableStateFlow(false)
    private val uuid = UUID.fromString("some_uid")

    val items: Flow<List<BluetoothItem>> = repository.bluetoothDevices
    val isRefreshing: Flow<Boolean> = _isRefreshing.asStateFlow()

    init {
        startFinding()
    }

    fun toggleRefreshing() = if (_isRefreshing.value) stopFinding() else startFinding()

    fun connectWithDevice(item: BluetoothDevice) {

        controller.stopFindingBluetoothDevices()
        val bluetoothSocket = item.createRfcommSocketToServiceRecord(uuid)
        viewModelScope.launch {
            kotlin.runCatching {
                bluetoothSocket?.connect()
            }.onFailure {
                debug(it.localizedMessage)
            }
        }
    }

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