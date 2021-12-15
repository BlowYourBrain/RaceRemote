package com.simple.raceremote

import android.app.Application
import com.simple.raceremote.di.appModule
import com.simple.raceremote.di.bluetoothDevicesModule
import com.simple.raceremote.di.remoteControlModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MainApplication)
            modules(appModule, remoteControlModule, bluetoothDevicesModule)
        }
    }

}