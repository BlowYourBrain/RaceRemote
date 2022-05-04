package com.simple.raceremote.features.remote_control.presentation

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simple.raceremote.R
import com.simple.raceremote.ui.views.DotsState
import com.simple.raceremote.utils.sidepanel.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class ActionsViewModel(
    private val sidePanelActionProvider: ISidePanelActionProvider,
    private val sidePanelActionProducer: ISidePanelActionProducer,
) : ViewModel() {

    private val defaultActions = listOf(
        Action(
            icon = R.drawable.ic_baseline_bluetooth_searching_24,
            onClick = ::onBluetoothClick,
            state = DotsState.Idle()
        ),
        Action(
            icon = R.drawable.ic_baseline_settings_24,
            onClick = ::onSettingsClick,
            state = DotsState.Idle()
        )
    )
    private val _actions = MutableStateFlow<List<Action>>(defaultActions)

    val isPanelOpen: Flow<Boolean> = sidePanelActionProvider.action.map { it.isOpenAction() }
    val actions: Flow<List<Action>> = _actions

    fun isOpened(value: Boolean) {
        sidePanelActionProducer.produceAction(value.toSidePanelAction())
    }

    private fun onBluetoothClick() {
        sidePanelActionProducer.produceAction(SidePanelAction.Open)
    }

    private fun onSettingsClick() {

    }

}

class Action(
    @DrawableRes val icon: Int,
    val onClick: (() -> Unit),
    val state: DotsState
)