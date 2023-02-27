package com.simple.raceremote.features.remote_control.data

import kotlinx.coroutines.delay

object MockRemoteDeviceApi : RemoteDeviceApi {

    override suspend fun isRemoteDevice(): Boolean {
        delay(1000)
        return true
    }
}