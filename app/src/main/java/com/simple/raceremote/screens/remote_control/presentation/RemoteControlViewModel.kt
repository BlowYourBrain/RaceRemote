package com.simple.raceremote.screens.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.data.BluetoothConnection
import com.simple.raceremote.data.IBluetoothConnection
import com.simple.raceremote.screens.remote_control.CompoundCommandCreator
import com.simple.raceremote.screens.remote_control.ICompoundCommandCreator
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.charset.Charset

//TODO вынести зависимости и провайдить через конструктор.
class RemoteControlViewModel : ViewModel() {

    private val mapper: ISteeringWheelMapper = SteeringWheelMapper()
    private val bluetoothFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    private val bluetoothConnection: IBluetoothConnection = BluetoothConnection
    private val compoundCommandCreator: ICompoundCommandCreator = CompoundCommandCreator()

    init {
        viewModelScope.launch {
            bluetoothFlow.collect { value ->

                val msg = (if (value > 0f) 1 else 0).toByte()

                debug("send bytes = $msg")
                bluetoothConnection.sendMessage(
                    byteArrayOf(msg)
                )
            }
        }
    }

    fun updateSteeringWheel(value: Float) {
        debug("horizontal: $value")
        bluetoothFlow.tryEmit(
            compoundCommandCreator.createSteeringWheelCommand(
                mapper.map(value)
            )
        )
    }

    fun updateMovement(value: Float) {
        debug("vertical: $value")
//        bluetoothFlow.tryEmit(value)
    }

}
