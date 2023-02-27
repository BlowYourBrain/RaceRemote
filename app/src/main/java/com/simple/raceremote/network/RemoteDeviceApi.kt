package com.simple.raceremote.network

import retrofit2.http.GET

interface RemoteDeviceApi {

    @GET("remote_device/")
    suspend fun isRemoteDevice()

}