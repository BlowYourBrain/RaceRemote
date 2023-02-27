package com.simple.raceremote.network

import kotlinx.coroutines.delay

object MockRemoteDeviceApi : RemoteDeviceApi {

    override suspend fun isRemoteDevice() {
        delay(1000)
    }
}