package com.simple.raceremote.features.bluetooth_devices.di

import com.simple.raceremote.features.bluetooth_devices.presentation.BluetoothDevicesViewModel
import org.koin.dsl.module

val bluetoothDevicesModule = module {
    single { BluetoothDevicesViewModel(get(), get()) }
}
