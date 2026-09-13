package vea.raceremote.features.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import vea.raceremote.features.remote_control.data.IRemoteDeviceRepository
import vea.raceremote.features.remote_control.data.RemoteDeviceRepository
import vea.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import vea.raceremote.features.remote_control.presentation.mapper.ISteeringWheelMapper
import vea.raceremote.features.remote_control.presentation.model.RemoteDevice
import vea.raceremote.features.remote_control.utils.ICompoundCommandCreator
import vea.raceremote.network.WebSocketApi
import vea.raceremote.utils.bluetooth.IBluetoothConnection
import vea.raceremote.utils.debug
import vea.raceremote.utils.singleEventChannel
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
        return compoundCommandCreator.createEngineCommand(
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
