package com.simple.raceremote.screens.remote_control.di

import com.simple.raceremote.screens.remote_control.utils.CompoundCommandCreator
import com.simple.raceremote.screens.remote_control.utils.ICompoundCommandCreator
import com.simple.raceremote.screens.remote_control.presentation.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val remoteControlModule = module {
    factory<IEngineMapper> { EngineMapper() }
    factory<ISteeringWheelMapper> { SteeringWheelMapper() }
    factory<ICompoundCommandCreator> { CompoundCommandCreator() }
    viewModel { RemoteControlViewModel(get(), get(), get(), get()) }
}