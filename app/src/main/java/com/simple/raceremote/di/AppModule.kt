package com.simple.raceremote.di

import com.simple.raceremote.data.*
import com.simple.raceremote.screens.bluetooth_devices.presentation.BluetoothDevicesViewModel
import com.simple.raceremote.screens.remote_control.CompoundCommandCreator
import com.simple.raceremote.screens.remote_control.ICompoundCommandCreator
import com.simple.raceremote.screens.remote_control.presentation.*
import com.simple.raceremote.utils.BluetoothHelper
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {
    single { BluetoothHelper() } binds arrayOf(
        IBluetoothItemsProvider::class,
        IBluetoothBroadcastReceiver::class,
        IBluetoothDevicesDiscoveryController::class,
    )
    single<IBluetoothConnection> { BluetoothConnection(get()) }
}

val remoteControlModule = module {
    factory<IEngineMapper> { EngineMapper() }
    factory<ISteeringWheelMapper> { SteeringWheelMapper() }
    factory<ICompoundCommandCreator> { CompoundCommandCreator() }
    viewModel { RemoteControlViewModel(get(), get(), get(), get()) }
}

val bluetoothDevicesModule = module {
    viewModel { BluetoothDevicesViewModel(get(), get(), get(), get()) }
}