package com.simple.raceremote.screens.bluetooth_devices.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.simple.raceremote.data.*
import com.simple.raceremote.utils.BluetoothHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.*

private val source = BluetoothHelper
private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"

class BluetoothDevicesViewModel(application: Application) : AndroidViewModel(application) {

    //TODO провайдить через DI
    private val bluetoothConnection: IBluetoothConnection = BluetoothConnection
    private val controller: IBluetoothDevicesDiscoveryController = source
    private val repository: IBluetoothItemsProvider = source
    private val _isRefreshing = MutableStateFlow(false)
    private val uuid = UUID.fromString(UUID_STR)

    val items: Flow<List<BluetoothEntity>> = repository.bluetoothDevices.map { list ->
        list.map { it.mapToBluetoothEntity() }
            .sortedBy { it.isPaired }
    }

    val isRefreshing: Flow<Boolean> = _isRefreshing.asStateFlow()

    init {
        startFinding()
    }

    fun toggleRefreshing() = if (_isRefreshing.value) stopFinding() else startFinding()

    override fun onCleared() {
        super.onCleared()
        stopFinding()
    }

    private fun onItemClick(macAddress: String) {
        bluetoothConnection.connectWithDevice(GlobalScope, getApplication(), macAddress, uuid)
    }

    private fun startFinding() {
        _isRefreshing.tryEmit(true)
        controller.findBluetoothDevices(getApplication())
    }

    private fun stopFinding() {
        _isRefreshing.tryEmit(false)
        controller.stopFindingBluetoothDevices(getApplication())
    }

    private fun BluetoothItem.mapToBluetoothEntity(): BluetoothEntity =
        BluetoothEntity(
            name = name,
            macAddress = macAddress,
            isPaired = isPaired,
            onClick = { onItemClick(macAddress = macAddress) }
        )
}