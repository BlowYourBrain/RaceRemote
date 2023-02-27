package com.simple.raceremote.features.remote_control.data

import com.simple.raceremote.network.RemoteDeviceApi
import com.simple.raceremote.utils.debug

interface IRemoteDeviceRepository {
    suspend fun isRemoteDeviceAvailable(): Boolean
}

class RemoteDeviceRepository(private val remoteDeviceApi: RemoteDeviceApi) :
    IRemoteDeviceRepository {

    override suspend fun isRemoteDeviceAvailable(): Boolean {
        val result = kotlin.runCatching { remoteDeviceApi.isRemoteDevice() }
            .onFailure {
                debug(it.message ?: "unknown failure", "WIFI_CONNECTION_TAG")
                debug(it.stackTraceToString(), "WIFI_CONNECTION_TAG")
            }
        return result.isSuccess
    }
}