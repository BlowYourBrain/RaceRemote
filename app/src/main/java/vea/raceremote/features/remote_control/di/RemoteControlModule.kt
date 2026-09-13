package vea.raceremote.features.remote_control.di

import vea.raceremote.features.remote_control.data.IRemoteDeviceRepository
import vea.raceremote.features.remote_control.data.RemoteDeviceRepository
import vea.raceremote.features.remote_control.presentation.ActionsViewModel
import vea.raceremote.features.remote_control.presentation.RemoteControlViewModel
import vea.raceremote.features.remote_control.presentation.RemoteDeviceConnection
import vea.raceremote.features.remote_control.presentation.mapper.EngineMapper
import vea.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import vea.raceremote.features.remote_control.presentation.mapper.ISteeringWheelMapper
import vea.raceremote.features.remote_control.presentation.mapper.SteeringWheelMapper
import vea.raceremote.features.remote_control.utils.CompoundCommandCreator
import vea.raceremote.features.remote_control.utils.CompoundCommandExtractor
import vea.raceremote.features.remote_control.utils.ICompoundCommandCreator
import vea.raceremote.features.remote_control.utils.ICompoundCommandExtractor
import vea.raceremote.utils.sidepanel.ISidePanelActionProducer
import vea.raceremote.utils.sidepanel.ISidePanelActionProvider
import vea.raceremote.utils.sidepanel.SidePanelActionProvider
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