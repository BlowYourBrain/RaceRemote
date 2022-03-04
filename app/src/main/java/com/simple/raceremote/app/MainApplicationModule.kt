package com.simple.raceremote.app

import android.app.Application
import com.simple.raceremote.BuildConfig
import com.simple.raceremote.screens.bluetooth_devices.di.bluetoothDevicesModule
import com.simple.raceremote.screens.remote_control.di.remoteControlModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplicationModule : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MainApplicationModule)
            modules(appModule, remoteControlModule, bluetoothDevicesModule)
        }
    }

}