package com.simple.raceremote.app

import com.simple.raceremote.features.remote_control.presentation.IWifiNetwork
import com.simple.raceremote.features.remote_control.presentation.WifiNetwork
import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<IBluetoothConnection> { BluetoothConnection() }
    single<IWifiNetwork> { WifiNetwork(get(), get()) }
}