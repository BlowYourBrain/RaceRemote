package com.simple.raceremote.screens.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.screens.remote_control.utils.ICompoundCommandCreator
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RemoteControlViewModel(
    private val engineMapper: IEngineMapper,
    private val steeringWheelMapper: ISteeringWheelMapper,
    private val bluetoothConnection: IBluetoothConnection,
    private val compoundCommandCreator: ICompoundCommandCreator,
) : ViewModel() {

    private val bluetoothFlow: MutableStateFlow<Int> = MutableStateFlow(0)

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
