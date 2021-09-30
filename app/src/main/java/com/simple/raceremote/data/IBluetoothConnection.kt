package com.simple.raceremote.data

import android.bluetooth.BluetoothSocket
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.getBluetoothAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

interface IBluetoothConnection {

    suspend fun sendMessage(bytes: ByteArray)

    fun connectWithDevice(scope: CoroutineScope, macAddress: String, uuid: UUID)

    fun closeConnection()

}

//TODO вынести поля в конструктор и провайдить через DI
object BluetoothConnection : IBluetoothConnection {

    private val buffer = ByteArray(1024)

    private var bluetoothSocket: BluetoothSocket? = null
    private val inStream = bluetoothSocket?.inputStream
    private val outputStream = bluetoothSocket?.outputStream

    private val bluetoothDiscoveryController: IBluetoothDevicesDiscoveryController = BluetoothHelper

    override fun connectWithDevice(scope: CoroutineScope, macAddress: String, uuid: UUID) {
        bluetoothDiscoveryController.stopFindingBluetoothDevices()

        val remoteDevice = getBluetoothAdapter().getRemoteDevice(macAddress)
        bluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(uuid)

        scope.launch {
            kotlin.runCatching {
                bluetoothSocket?.connect()
                debug("bluetooth connection established")
            }.onFailure {
                debug(it.localizedMessage)
                debug("bluetooth connection failed")
            }
        }
    }

    override suspend fun sendMessage(bytes: ByteArray) {
        kotlin.runCatching {
            outputStream?.write(bytes)
        }.onFailure {
            debug(it.toString())
            debug("bluetooth send message failed")
        }
    }

    override fun closeConnection() {
        kotlin.runCatching {
            bluetoothSocket?.close()
        }.onFailure {
            debug(it.toString())
            debug("close bluetooth connection failed")
        }
    }
}