package com.simple.raceremote.features.remote_control.di

import com.simple.raceremote.features.remote_control.utils.CompoundCommandCreator
import com.simple.raceremote.features.remote_control.utils.ICompoundCommandCreator
import com.simple.raceremote.features.remote_control.presentation.*
import com.simple.raceremote.features.remote_control.presentation.mapper.EngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.ISteeringWheelMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.SteeringWheelMapper
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProvider
import com.simple.raceremote.utils.sidepanel.SidePanelActionProvider
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

val remoteControlModule = module {
    factory<IEngineMapper> { EngineMapper() }
    factory<ISteeringWheelMapper> { SteeringWheelMapper() }
    factory<ICompoundCommandCreator> { CompoundCommandCreator() }
    factory { SidePanelActionProvider() } binds arrayOf(
        ISidePanelActionProducer::class,
        ISidePanelActionProvider::class
    )
    viewModel { RemoteControlViewModel(get(), get(), get(), get()) }
    viewModel { ActionsViewModel(get(), get()) }
}