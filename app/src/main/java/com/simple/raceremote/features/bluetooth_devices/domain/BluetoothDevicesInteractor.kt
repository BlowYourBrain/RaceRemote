package com.simple.raceremote.features.bluetooth_devices.domain

import android.app.Application
import androidx.compose.ui.unit.dp
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"
private const val TEXT_SIZE = 5

// todo use interface
class BluetoothDevicesInteractor(
    private val application: Application,
    private val bluetoothConnection: IBluetoothConnection
) {

    private val uuid = UUID.fromString(UUID_STR)
    private val _isRefreshing = MutableStateFlow(false)
    private val _bluetoothConnectionState: MutableStateFlow<DotsState> = MutableStateFlow(DotsState.Idle())

    val isRefreshing: Flow<Boolean> = _isRefreshing.asStateFlow()
    val bluetoothConnectionState: Flow<DotsState> = _bluetoothConnectionState.asStateFlow()

    fun setFinding(isFinding: Boolean) {
        if (isFinding) startFinding() else stopFinding()
    }

    fun toggleRefreshing() = if (_isRefreshing.value) stopFinding() else startFinding()

    suspend fun connectWithDevice(macAddress: String) {
        _bluetoothConnectionState.emit(DotsState.Loading())
        val deviceName = bluetoothConnection.connectWithDevice(application, macAddress, uuid)
        val state = deviceName?.let { DotsState.ShowText(it, TEXT_SIZE.dp) } ?: DotsState.Idle()
        _bluetoothConnectionState.emit(state)
    }

    private fun startFinding() {
        _isRefreshing.tryEmit(true)
    }

    private fun stopFinding() {
        _isRefreshing.tryEmit(false)
    }
}
