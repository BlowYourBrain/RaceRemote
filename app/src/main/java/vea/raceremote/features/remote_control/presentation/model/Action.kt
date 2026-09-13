package vea.raceremote.features.remote_control.presentation.model

import androidx.annotation.DrawableRes
import vea.raceremote.ui.views.DotsState


data class Action(
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val state: DotsState
)