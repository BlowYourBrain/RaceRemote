package com.simple.raceremote.features.remote_control.presentation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.raceremote.R
import com.simple.raceremote.features.remote_control.presentation.model.RemoteDevice
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.utils.bluetooth.hasBluetooth
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProvider
import com.simple.raceremote.utils.sidepanel.toSidePanelAction
import com.simple.raceremote.utils.singleEventChannel
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ActionsViewModel(
    private val context: Context,
    private val bluetoothConnection: IBluetoothConnection,
    private val sidePanelActionProvider: ISidePanelActionProvider,
    private val sidePanelActionProducer: ISidePanelActionProducer,
) : ViewModel() {
    companion object {
        const val REQUEST_ENABLE_BT = 40
        const val REQUEST_BLUETOOTH_PERMISSIONS = 50
        const val SELECT_DEVICE_REQUEST_CODE = 100500


        private const val UUID_STR = "4ab19e4e-e6c1-43ba-b9cd-0b19777da670"

        private const val DOTS_HEIGHT = 14
        private const val DOTS_TEXT_HEIGHT = 12

        private const val WIFI_POSITION = 1
        private const val BLUETOOTH_POSITION = 0
    }

    private val loadingDotsState = DotsState.Loading(DOTS_HEIGHT.dp)
    private val idleDotsState = DotsState.Idle(DOTS_HEIGHT.dp)

    private val defaultActions = listOf(
        Action(
            icon = R.drawable.ic_baseline_bluetooth_searching_24,
            onClick = ::onBluetoothClick,
            state = idleDotsState
        ),
        Action(
            icon = R.drawable.ic_baseline_wifi_find_24,
            onClick = ::onWifiClick,
            state = idleDotsState
        ),
        Action(
            icon = R.drawable.ic_baseline_settings_24,
            onClick = ::onSettingsClick,
            state = idleDotsState
        )
    )

    private val uuid = UUID.fromString(UUID_STR)
    private val _actions = MutableStateFlow(defaultActions)
    private val _remoteDevice = singleEventChannel<RemoteDevice>()
    private val _bluetoothPermissions = singleEventChannel<Unit>()

    private var connection: IBluetoothConnection.Connection? = null

    val remoteDevice: Flow<RemoteDevice> = _remoteDevice.receiveAsFlow()
    val requestBluetoothPermissions: Flow<Unit> = _bluetoothPermissions.receiveAsFlow()

    val actions: Flow<List<Action>> = _actions.asStateFlow()
    val isPanelOpen: Flow<Boolean> = sidePanelActionProvider.action.map { it.isOpenAction() }

    /**
     * @param macAddress - bluetooth device macAddress
     * */
    fun connect(macAddress: String) {
        if (!context.hasBluetoothPermissions()) return

        viewModelScope.launch {
            updateState(RemoteDevice.Bluetooth, loadingDotsState)
            val connection = bluetoothConnection.connectWithDevice(context, macAddress, uuid)

            when (connection) {
                is IBluetoothConnection.Connection.Success -> {
                    val deviceName = connection.name
                    this@ActionsViewModel.connection = connection
                    updateState(RemoteDevice.Bluetooth, createDotsTextState(deviceName))
                }
                is IBluetoothConnection.Connection.Error -> {
                    // TODO: notify user
                    debug("connection failed")
                    updateState(RemoteDevice.Bluetooth, idleDotsState)
                }
            }

        }
    }

    private fun createDotsTextState(text: String): DotsState {
        return DotsState.ShowText(
            text = text,
            textSize = DOTS_TEXT_HEIGHT.dp,
            height = DOTS_HEIGHT.dp
        )
    }

    private fun updateState(device: RemoteDevice, dotsState: DotsState) {
        val position = when (device) {
            RemoteDevice.Bluetooth -> BLUETOOTH_POSITION
            RemoteDevice.WIFI -> WIFI_POSITION
        }

        val newState = defaultActions.toMutableList()
        val newAction = newState[position].copy(state = dotsState)
        newState[position] = newAction

        _actions.tryEmit(newState)
    }

    /**
     * @return false if has any problems with bluetooth
     * */
    private fun checkBluetooth(): Boolean = with(context) {
        if (!hasBluetooth()) {
            //todo notify device has not bluetooth
            debug("device has no bluetooth")
            return false
        }

        if (!hasBluetoothPermissions()) {
            _bluetoothPermissions.trySend(Unit)
            return false
        }

        //todo change bluetooth condition
//        if (!isBluetoothEnabled()) {
//            enableBluetooth(requestCode, activity)
//            return false
//        }

        return true
    }

    fun isOpened(value: Boolean) {
        sidePanelActionProducer.produceAction(value.toSidePanelAction())
    }

    private fun onBluetoothClick() {
        if (hasBluetoothConnection()) {
            bluetoothConnection.closeConnection()
            connection = null
            updateState(RemoteDevice.Bluetooth, idleDotsState)
            return
        }

        if (!checkBluetooth()) return

        _remoteDevice.trySend(RemoteDevice.Bluetooth)
    }

    private fun onWifiClick() {
        _remoteDevice.trySend(RemoteDevice.WIFI)
    }

    private fun onSettingsClick() {
        //todo Do I really need it?
    }

    private fun hasBluetoothConnection(): Boolean {
        return _actions.value[BLUETOOTH_POSITION].state is DotsState.ShowText
    }
}

data class Action(
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val state: DotsState
)