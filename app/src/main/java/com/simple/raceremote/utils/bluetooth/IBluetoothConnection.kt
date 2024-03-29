package com.simple.raceremote.utils.bluetooth

import android.bluetooth.BluetoothSocket
import android.content.Context
import com.simple.raceremote.features.remote_control.utils.ICompoundCommandExtractor
import com.simple.raceremote.utils.debug
import java.util.UUID

interface IBluetoothConnection {

    sealed class Connection() {
        class Success(val name: String) : Connection()
        object Error : Connection()
    }

    suspend fun sendMessage(bytes: Int)

    /**
     * @return name of connected bluetooth device, null otherwise
     * */
    suspend fun connectWithDevice(context: Context, macAddress: String, uuid: UUID): Connection

    fun closeConnection()
}

class BluetoothConnection(private val compoundCommandExtractor: ICompoundCommandExtractor) :
    IBluetoothConnection {

    private var bluetoothSocket: BluetoothSocket? = null
    private val outputStream get() = bluetoothSocket?.outputStream

    override suspend fun connectWithDevice(
        context: Context,
        macAddress: String,
        uuid: UUID
    ): IBluetoothConnection.Connection {
        val remoteDevice = context.getBluetoothAdapter()?.getRemoteDevice(macAddress)
            ?: return IBluetoothConnection.Connection.Error

        // ******************************************************************************************
        // bluetooth connection solution -> https://www.it1228.com/74366.html
        bluetoothSocket = remoteDevice.javaClass.getMethod(
            "createRfcommSocket",
            *arrayOf<Class<*>?>(
                Int::class.javaPrimitiveType
            )
        ).invoke(remoteDevice, 1) as BluetoothSocket
        // ******************************************************************************************

        return kotlin.runCatching {
            bluetoothSocket?.connect()
            debug("bluetooth connection established")

            return@runCatching IBluetoothConnection.Connection.Success(remoteDevice.name)
        }.onFailure {
            debug(it.localizedMessage.orEmpty())
            debug("bluetooth connection failed")
        }.getOrElse { IBluetoothConnection.Connection.Error }
    }

    override suspend fun sendMessage(bytes: Int) {
        kotlin.runCatching {
            outputStream?.write(compoundCommandExtractor.extractStartCommand(bytes).toInt())
            outputStream?.write(compoundCommandExtractor.extractEndCommand(bytes).toInt())
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
