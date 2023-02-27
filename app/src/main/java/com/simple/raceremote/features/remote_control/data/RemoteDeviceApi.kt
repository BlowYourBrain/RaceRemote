package com.simple.raceremote.features.remote_control.data

interface RemoteDeviceApi {

    suspend fun isRemoteDevice(): Boolean

}