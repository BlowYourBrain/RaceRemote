package com.simple.raceremote.app

import com.simple.raceremote.features.remote_control.presentation.IRemoteDeviceFinding
import com.simple.raceremote.features.remote_control.presentation.RemoteDeviceFinding
import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    factory<IRemoteDeviceFinding> { RemoteDeviceFinding() }
    single<IBluetoothConnection> { BluetoothConnection() }
}
