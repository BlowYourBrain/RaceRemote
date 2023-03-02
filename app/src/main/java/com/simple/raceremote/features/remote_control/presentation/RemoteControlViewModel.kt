package com.simple.raceremote.features.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.features.remote_control.data.IRemoteDeviceRepository
import com.simple.raceremote.features.remote_control.data.RemoteDeviceRepository
import com.simple.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.ISteeringWheelMapper
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice
import com.simple.raceremote.features.remote_control.utils.ICompoundCommandCreator
import com.simple.raceremote.network.WebSocketApi
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.singleEventChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RemoteControlViewModel(
    private val engineMapper: IEngineMapper,
    private val steeringWheelMapper: ISteeringWheelMapper,
    private val bluetoothConnection: IBluetoothConnection,
    private val compoundCommandCreator: ICompoundCommandCreator,
    private val remoteDeviceRepository: IRemoteDeviceRepository,
) : ViewModel() {

    private val bluetoothFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    private val wifiFlow = singleEventChannel<Int>()

    //todo find active remote device
    private val activeRemoteDevice: RemoteDevice = RemoteDevice.WIFI

    init {
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothFlow.collect { value ->
                bluetoothConnection.sendMessage(value)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            wifiFlow.receiveAsFlow().collect { value ->
                remoteDeviceRepository.sendData(IRemoteDeviceRepository.Destination.WiFi, value)
            }
        }
    }

    fun updateSteeringWheel(value: Float) {
        debug("horizontal: $value")
        sendValue(value, ::mapSteeringWheelValue)
    }


    fun updateMovement(value: Float) {
        debug("vertical: $value")
        sendValue(value, ::mapEngineValue)
    }

    private fun mapSteeringWheelValue(value: Float): Int {
        return compoundCommandCreator.createSteeringWheelCommand(
            steeringWheelMapper.mapToSteeringWheel(value)
        )
    }

    private fun mapEngineValue(value: Float): Int {
        return compoundCommandCreator.createSteeringWheelCommand(
            engineMapper.mapToEngineValue(value)
        )
    }

    private fun sendValue(value: Float, mapper: (Float) -> Int) {
        when (activeRemoteDevice) {
            RemoteDevice.WIFI -> wifiFlow.trySend(mapper.invoke(value))
            RemoteDevice.Bluetooth -> bluetoothFlow.tryEmit(mapper.invoke(value))
        }
    }
}
