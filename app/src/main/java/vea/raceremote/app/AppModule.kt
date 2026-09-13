package vea.raceremote.app

import vea.raceremote.features.remote_control.presentation.IWifiNetworkFinder
import vea.raceremote.features.remote_control.presentation.WifiNetworkFinder
import vea.raceremote.utils.bluetooth.BluetoothConnection
import vea.raceremote.utils.bluetooth.IBluetoothConnection
import org.koin.dsl.module

val appModule = module {
    single<IBluetoothConnection> { BluetoothConnection(get()) }
    single<IWifiNetworkFinder> { WifiNetworkFinder(get(), get()) }
}