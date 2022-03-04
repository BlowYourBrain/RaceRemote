package com.simple.raceremote.screens.bluetooth_devices.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.simple.raceremote.data.BluetoothItem
import com.simple.raceremote.data.IBluetoothConnection
import com.simple.raceremote.data.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.data.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.data.sidepanel.SidePanelActionProvider
import com.simple.raceremote.ui.views.DotsState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.function.DoubleToIntFunction

private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"

class BluetoothDevicesViewModel(
    application: Application,
    repository: IBluetoothItemsProvider,
    private val bluetoothConnection: IBluetoothConnection,
    private val controller: IBluetoothDevicesDiscoveryController
) : AndroidViewModel(application) {

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
        //todo change scope
        _bluetoothConnectionState.tryEmit(DotsState.Loading())
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