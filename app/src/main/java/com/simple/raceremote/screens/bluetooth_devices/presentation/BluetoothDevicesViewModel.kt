package com.simple.raceremote.screens.bluetooth_devices.presentation

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.data.BluetoothItem
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.data.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

private val source = BluetoothHelper
private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"

class BluetoothDevicesViewModel(application: Application) : AndroidViewModel(application) {

    //TODO провайдить через DI
    private val controller: IBluetoothDevicesDiscoveryController = source
    private val repository: IBluetoothItemsProvider = source
    private val _isRefreshing = MutableStateFlow(false)
    private val uuid = UUID.fromString(UUID_STR)

    val items: Flow<List<BluetoothEntity>> = repository.bluetoothDevices.map { list ->
        list.map { it.mapToBluetoothEntity() }
    }

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

    private fun onItemClick(macAddress: String) {
        getApplication<Application>().toast("item with macAddress $macAddress")
    }

    private fun startFinding() {
        _isRefreshing.tryEmit(true)
        controller.findBluetoothDevices()
    }

    private fun stopFinding() {
        _isRefreshing.tryEmit(false)
        controller.stopFindingBluetoothDevices()
    }

    private fun BluetoothItem.mapToBluetoothEntity(): BluetoothEntity =
        BluetoothEntity(
            name = name,
            macAddress = macAddress,
            isPaired = isPaired,
            onClick = { onItemClick(macAddress = macAddress) }
        )
}