package com.simple.raceremote.screens.remote_control.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.data.BluetoothConnection
import com.simple.raceremote.data.IBluetoothConnection
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RemoteControlViewModel : ViewModel() {

    private val bluetoothFlow: MutableStateFlow<Float> = MutableStateFlow(0f)
    private val bluetoothConnection: IBluetoothConnection = BluetoothConnection

    init {
        viewModelScope.launch {
            bluetoothFlow.collect { value ->
                bluetoothConnection.sendMessage(
                    byteArrayOf(if (value > 0f) 1 else 0)
                )
            }
        }
    }

    fun updateSteeringWheel(value: Float) {
        debug("horizontal: $value")
    }

    fun updateMovement(value: Float) {
        debug("vertical: $value")
        bluetoothFlow.tryEmit(value)
    }

}
