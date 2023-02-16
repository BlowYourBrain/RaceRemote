package com.simple.raceremote.features.remote_control.presentation.model

import androidx.annotation.DrawableRes
import com.simple.raceremote.ui.views.DotsState


data class Action(
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val state: DotsState
)