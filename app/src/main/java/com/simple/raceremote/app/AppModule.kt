package com.simple.raceremote.app

import com.simple.raceremote.utils.bluetooth.BluetoothConnection
import com.simple.raceremote.utils.bluetooth.BluetoothHelper
import com.simple.raceremote.utils.bluetooth.IBluetoothConnection
import com.simple.raceremote.utils.bluetooth.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.utils.bluetooth.IBluetoothItemsProvider
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
