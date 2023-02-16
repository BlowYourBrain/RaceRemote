package com.simple.raceremote.app

import com.simple.raceremote.features.remote_control.presentation.IWiFiConnection
import com.simple.raceremote.features.remote_control.presentation.WifiConnection
import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<IBluetoothConnection> { BluetoothConnection() }
    single<IWiFiConnection> { WifiConnection(get()) }
}