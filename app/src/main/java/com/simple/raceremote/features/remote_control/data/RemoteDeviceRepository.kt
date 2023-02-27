package com.simple.raceremote.features.remote_control.data

import com.simple.raceremote.network.RemoteDeviceApi

interface IRemoteDeviceRepository {
    suspend fun isRemoteDeviceAvailable(): Boolean
}

class RemoteDeviceRepository(private val remoteDeviceApi: RemoteDeviceApi) :
    IRemoteDeviceRepository {

    override suspend fun isRemoteDeviceAvailable(): Boolean {
        val result = kotlin.runCatching { remoteDeviceApi.isRemoteDevice() }
        return result.isSuccess
    }
}