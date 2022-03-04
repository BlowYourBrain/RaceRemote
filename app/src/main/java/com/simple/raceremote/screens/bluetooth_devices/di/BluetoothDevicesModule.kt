package com.simple.raceremote.screens.bluetooth_devices.di

import com.simple.raceremote.screens.bluetooth_devices.presentation.BluetoothDevicesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val bluetoothDevicesModule = module {
    viewModel { BluetoothDevicesViewModel(get(), get(), get(), get()) }
}