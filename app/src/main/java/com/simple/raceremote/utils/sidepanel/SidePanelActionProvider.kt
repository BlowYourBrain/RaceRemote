package com.simple.raceremote.utils.sidepanel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SidePanelActionProvider: ISidePanelActionProvider, ISidePanelActionProducer {
    private val _action = MutableStateFlow(SidePanelAction.Close)

    override val action: Flow<SidePanelAction> = _action.asStateFlow()

    override fun produceAction(action: SidePanelAction) {
        _action.tryEmit(action)
    }
}