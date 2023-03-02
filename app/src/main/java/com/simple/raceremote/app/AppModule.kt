package com.simple.raceremote.app

import com.simple.raceremote.features.remote_control.presentation.IWifiNetworkFinder
import com.simple.raceremote.features.remote_control.presentation.WifiNetworkFinder
import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<IBluetoothConnection> { BluetoothConnection(get()) }
    single<IWifiNetworkFinder> { WifiNetworkFinder(get(), get()) }
}