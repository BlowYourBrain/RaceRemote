package com.simple.raceremote.features.bluetooth_devices.di

import com.simple.raceremote.features.bluetooth_devices.domain.BluetoothDevicesInteractor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bluetoothDevicesModule = module {
    single { BluetoothDevicesInteractor(get(), get()) }
}
