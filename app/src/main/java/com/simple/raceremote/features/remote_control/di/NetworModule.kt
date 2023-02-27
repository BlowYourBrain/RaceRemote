package com.simple.raceremote.features.remote_control.di

import com.simple.raceremote.network.RemoteApiProvider
import com.simple.raceremote.network.RemoteDeviceApi
import org.koin.dsl.module
import retrofit2.Retrofit


val networkModule = module {
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://192.168.1.100:80/")
            .build()
    }
    single<RemoteDeviceApi> { RemoteApiProvider(get()).provideRemoteDeviceApi() }
}
