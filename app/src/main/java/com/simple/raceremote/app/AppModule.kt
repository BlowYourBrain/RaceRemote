package com.simple.raceremote.app

import com.simple.raceremote.features.remote_control.data.MockRemoteDeviceApi
import com.simple.raceremote.features.remote_control.data.RemoteDeviceApi
import com.simple.raceremote.features.remote_control.presentation.IWiFiConnection
import com.simple.raceremote.features.remote_control.presentation.IWifiNetwork
import com.simple.raceremote.features.remote_control.presentation.WifiNetwork
import com.simple.raceremote.features.remote_control.presentation.WifiConnection
import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<RemoteDeviceApi> { MockRemoteDeviceApi }
    single<IWiFiConnection> { WifiConnection(get()) }
    single<IBluetoothConnection> { BluetoothConnection() }
    single<IWifiNetwork> { WifiNetwork(get(), get()) }
}