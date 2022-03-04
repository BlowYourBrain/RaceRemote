package com.simple.raceremote.utils.sidepanel

import kotlinx.coroutines.flow.Flow

enum class SidePanelAction {
    Open,
    Close
}

interface ISidePanelActionProvider {
    val action: Flow<SidePanelAction>
}

interface ISidePanelActionProducer {
    fun produceAction(action: SidePanelAction)
}