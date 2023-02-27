package com.simple.raceremote.network

import retrofit2.Retrofit

class RemoteApiProvider(private val retrofit: Retrofit) {

    fun provideRemoteDeviceApi(): RemoteDeviceApi {
        return retrofit.create(RemoteDeviceApi::class.java)
    }

}