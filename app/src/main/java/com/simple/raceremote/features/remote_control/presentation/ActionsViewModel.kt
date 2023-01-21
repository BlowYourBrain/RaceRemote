package com.simple.raceremote.features.remote_control.presentation

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.simple.raceremote.R
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProvider
import com.simple.raceremote.utils.sidepanel.toSidePanelAction
import com.simple.raceremote.utils.singleEventChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class ActionsViewModel(
    private val sidePanelActionProvider: ISidePanelActionProvider,
    private val sidePanelActionProducer: ISidePanelActionProducer,
) : ViewModel() {

    enum class RemoteDevice {
        WIFI,
        Bluetooth
    }

    private val defaultActions = listOf(
        Action(
            icon = R.drawable.ic_baseline_bluetooth_searching_24,
            onClick = ::onBluetoothClick,
            state = DotsState.Idle()
        ),
        Action(
            icon = R.drawable.ic_baseline_wifi_find_24,
            onClick = ::onWifiClick,
            state = DotsState.Idle()
        ),
        Action(
            icon = R.drawable.ic_baseline_settings_24,
            onClick = ::onSettingsClick,
            state = DotsState.Idle()
        )
    )
    private val _actions = singleEventChannel(defaultActions)
    private val _enableBluetoothAction = singleEventChannel<Unit>()
    private val _onActionCLick = singleEventChannel<RemoteDevice>()

    val actions: Flow<List<Action>> = _actions.receiveAsFlow()
    val onActionCLick: Flow<RemoteDevice> = _onActionCLick.receiveAsFlow()
    val enableBluetoothAction: Flow<Unit> = _enableBluetoothAction.receiveAsFlow()
    val isPanelOpen: Flow<Boolean> = sidePanelActionProvider.action.map { it.isOpenAction() }

    fun isOpened(value: Boolean) {
        sidePanelActionProducer.produceAction(value.toSidePanelAction())
    }

    private fun onBluetoothClick() {
        _onActionCLick.trySend(RemoteDevice.Bluetooth)
    }

    private fun onWifiClick() {
        _onActionCLick.trySend(RemoteDevice.WIFI)
    }

    private fun onSettingsClick() {

    }
}

class Action(
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val state: DotsState
)