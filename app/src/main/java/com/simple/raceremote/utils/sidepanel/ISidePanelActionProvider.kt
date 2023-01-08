package com.simple.raceremote.utils.sidepanel

import kotlinx.coroutines.flow.Flow

enum class SidePanelAction {
    Open,
    Close;

    fun isOpenAction(): Boolean = this == Open
}

fun Boolean.toSidePanelAction() = if (this) SidePanelAction.Open else SidePanelAction.Close

interface ISidePanelActionProvider {
    val action: Flow<SidePanelAction>
}

interface ISidePanelActionProducer {
    fun produceAction(action: SidePanelAction)
}
