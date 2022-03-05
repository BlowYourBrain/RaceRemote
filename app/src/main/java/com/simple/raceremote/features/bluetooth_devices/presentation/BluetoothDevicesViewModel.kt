package com.simple.raceremote.features.bluetooth_devices.presentation

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.utils.bluetooth.BluetoothItem
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.utils.bluetooth.IBluetoothItemsProvider
import com.simple.raceremote.ui.views.DotsState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"

class BluetoothDevicesViewModel(
    application: Application,
    repository: IBluetoothItemsProvider,
    private val bluetoothConnection: IBluetoothConnection,
    private val controller: IBluetoothDevicesDiscoveryController
) : AndroidViewModel(application) {

    companion object {
        private const val TEXT_SIZE = 5
    }

    private val uuid = UUID.fromString(UUID_STR)
    private val _isRefreshing = MutableStateFlow(false)
    private val _bluetoothConnectionState: MutableStateFlow<DotsState> = MutableStateFlow(DotsState.Idle())

    val items: Flow<List<BluetoothEntity>> = repository.bluetoothDevices.map { list ->
        list.map { it.mapToBluetoothEntity() }
    }
    val bluetoothConnectionState: Flow<DotsState> = _bluetoothConnectionState.asStateFlow()
    val isRefreshing: Flow<Boolean> = _isRefreshing.asStateFlow()

    init {
        startFinding()
    }

    fun setFinding(isFinding: Boolean) {
        if (isFinding) startFinding() else stopFinding()
    }

    fun toggleRefreshing() = if (_isRefreshing.value) stopFinding() else startFinding()

    override fun onCleared() {
        super.onCleared()
        stopFinding()
    }

    private fun onItemClick(macAddress: String) {
        _bluetoothConnectionState.tryEmit(DotsState.Loading())
        viewModelScope.launch {
            val deviceName = bluetoothConnection.connectWithDevice(getApplication(), macAddress, uuid)
            val state = deviceName?.let { DotsState.ShowText(it, TEXT_SIZE.dp) } ?: DotsState.Idle()
            _bluetoothConnectionState.tryEmit(state)
        }
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