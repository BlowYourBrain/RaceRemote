package com.simple.raceremote.features.remote_control.di

import com.simple.raceremote.features.remote_control.data.IRemoteDeviceRepository
import com.simple.raceremote.features.remote_control.data.RemoteDeviceRepository
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.RemoteControlViewModel
import com.simple.raceremote.features.remote_control.presentation.RemoteDeviceConnection
import com.simple.raceremote.features.remote_control.presentation.mapper.EngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.ISteeringWheelMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.SteeringWheelMapper
import com.simple.raceremote.features.remote_control.utils.CompoundCommandCreator
import com.simple.raceremote.features.remote_control.utils.CompoundCommandExtractor
import com.simple.raceremote.features.remote_control.utils.ICompoundCommandCreator
import com.simple.raceremote.features.remote_control.utils.ICompoundCommandExtractor
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProducer
import com.simple.raceremote.utils.sidepanel.ISidePanelActionProvider
import com.simple.raceremote.utils.sidepanel.SidePanelActionProvider
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

val remoteControlModule = module {
    factory { RemoteDeviceConnection() }
    factory<IEngineMapper> { EngineMapper() }
    factory<ISteeringWheelMapper> { SteeringWheelMapper() }
    factory<ICompoundCommandCreator> { CompoundCommandCreator() }
    factory<ICompoundCommandExtractor> { CompoundCommandExtractor() }

    factory<IRemoteDeviceRepository> { RemoteDeviceRepository(get(), get(), get()) }

    single { SidePanelActionProvider() } binds arrayOf(
        ISidePanelActionProducer::class,
        ISidePanelActionProvider::class
    )
    single { ActionsViewModel(get(), get(), get(), get(), get()) }

    viewModel { RemoteControlViewModel(get(), get(), get(), get(), get()) }
}