package com.simple.raceremote.app

import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<IBluetoothConnection> { BluetoothConnection() }
}