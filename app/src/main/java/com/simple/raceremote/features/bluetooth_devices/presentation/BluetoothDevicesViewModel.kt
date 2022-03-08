package com.simple.raceremote.features.bluetooth_devices.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.features.bluetooth_devices.domain.BluetoothDevicesInteractor
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.bluetooth.BluetoothItem
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.utils.sidepanel.SidePanelAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BluetoothDevicesViewModel(
    private val interactor: BluetoothDevicesInteractor,
    private val sidePanelActionProducer: ISidePanelActionProducer
) : ViewModel() {

    val isRefreshing: Flow<Boolean> = interactor.isRefreshing
    val items: Flow<List<BluetoothEntity>> = interactor.items.map { list ->
        list.map { it.mapToBluetoothEntity() }
    }
    val bluetoothConnectionState: Flow<DotsState> = interactor.bluetoothConnectionState

    init {
        interactor.setFinding(true)
    }

    override fun onCleared() {
        super.onCleared()
        interactor.setFinding(false)
    }

    private fun onItemClick(macAddress: String) {
        viewModelScope.launch {
            interactor.connectWithDevice(macAddress = macAddress)
        }
        sidePanelActionProducer.produceAction(SidePanelAction.Close)
    }

    private fun BluetoothItem.mapToBluetoothEntity(): BluetoothEntity =
        BluetoothEntity(
            name = name,
            macAddress = macAddress,
            isPaired = isPaired,
            onClick = { onItemClick(macAddress = macAddress) }
        )
}