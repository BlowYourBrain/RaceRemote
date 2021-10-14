package com.simple.raceremote.screens.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.data.BluetoothConnection
import com.simple.raceremote.data.IBluetoothConnection
import com.simple.raceremote.screens.remote_control.CompoundCommandCreator
import com.simple.raceremote.screens.remote_control.ICompoundCommandCreator
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

//TODO вынести зависимости и провайдить через конструктор.
class RemoteControlViewModel : ViewModel() {

    private val engineMapper: IEngineMapper = EngineMapper()
    private val steeringWheelMapper: ISteeringWheelMapper = SteeringWheelMapper()

    private val bluetoothConnection: IBluetoothConnection = BluetoothConnection
    private val bluetoothFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    private val compoundCommandCreator: ICompoundCommandCreator = CompoundCommandCreator()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothFlow.collect { value ->
                bluetoothConnection.sendMessage(value)
            }
        }
    }

    fun updateSteeringWheel(value: Float) {
        debug("horizontal: $value")
        bluetoothFlow.tryEmit(
            compoundCommandCreator.createSteeringWheelCommand(
                steeringWheelMapper.mapToSteeringWheel(value)
            )
        )
    }

    fun updateMovement(value: Float) {
        debug("vertical: $value")
        bluetoothFlow.tryEmit(
            compoundCommandCreator.createEngineCommand(
                engineMapper.mapToEngineValue(value)
            )
        )
    }

}
