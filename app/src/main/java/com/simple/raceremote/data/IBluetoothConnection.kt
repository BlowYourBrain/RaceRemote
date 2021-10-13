package com.simple.raceremote.data

import android.bluetooth.BluetoothSocket
import android.content.Context
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.getBluetoothAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

interface IBluetoothConnection {

    suspend fun sendMessage(bytes: Int)

    fun connectWithDevice(scope: CoroutineScope, context: Context, macAddress: String, uuid: UUID)

    fun closeConnection()

}

//TODO вынести поля в конструктор и провайдить через DI
object BluetoothConnection : IBluetoothConnection {

    private var bluetoothSocket: BluetoothSocket? = null
    private val inStream get() = bluetoothSocket?.inputStream
    private val outputStream get() = bluetoothSocket?.outputStream

    private val bluetoothDiscoveryController: IBluetoothDevicesDiscoveryController = BluetoothHelper

    override fun connectWithDevice(
        scope: CoroutineScope,
        context: Context,
        macAddress: String,
        uuid: UUID
    ): Unit = context.run {
        bluetoothDiscoveryController.stopFindingBluetoothDevices(context)

        val remoteDevice = getBluetoothAdapter()?.getRemoteDevice(macAddress) ?: return

        //******************************************************************************************
        //https://www.it1228.com/74366.html
        bluetoothSocket = remoteDevice.javaClass.getMethod(
            "createRfcommSocket", *arrayOf<Class<*>?>(
                Int::class.javaPrimitiveType
            )
        ).invoke(remoteDevice, 1) as BluetoothSocket
        //******************************************************************************************

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

    override suspend fun sendMessage(bytes: Int) {
        kotlin.runCatching {
            debug("outputStream is ${if (outputStream == null) "" else "not"} null")
            debug("send command: ${bytes.toString(2)}")
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