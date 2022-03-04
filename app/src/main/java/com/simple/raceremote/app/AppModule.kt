package com.simple.raceremote.app

import com.simple.raceremote.utils.bluetooth.*
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {
    single { BluetoothHelper() } binds arrayOf(
        IBluetoothItemsProvider::class,
        IBluetoothDevicesDiscoveryController::class,
        BluetoothHelper::class
    )
    single<IBluetoothConnection> { BluetoothConnection(get()) }
}