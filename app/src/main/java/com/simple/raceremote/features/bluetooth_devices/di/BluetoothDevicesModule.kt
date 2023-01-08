package com.simple.raceremote.features.bluetooth_devices.di

import com.simple.raceremote.features.bluetooth_devices.domain.BluetoothDevicesInteractor
import com.simple.raceremote.features.bluetooth_devices.presentation.BluetoothDevicesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bluetoothDevicesModule = module {
    viewModel { BluetoothDevicesViewModel(get(), get()) }
    single { BluetoothDevicesInteractor(get(), get(), get(), get()) }
}
