package com.simple.raceremote.utils.bluetooth

import android.bluetooth.BluetoothSocket
import android.content.Context
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

interface IBluetoothConnection {

    suspend fun sendMessage(bytes: Int)

    fun connectWithDevice(scope: CoroutineScope, context: Context, macAddress: String, uuid: UUID)

    fun closeConnection()

}

class BluetoothConnection(
    private val bluetoothDiscoveryController: IBluetoothDevicesDiscoveryController
) : IBluetoothConnection {
    private companion object{
        const val clearFirst8Bytes = 0b0000_0000_1111_1111
    }

    private var bluetoothSocket: BluetoothSocket? = null

    private val inStream get() = bluetoothSocket?.inputStream
    private val outputStream get() = bluetoothSocket?.outputStream

    override fun connectWithDevice(
        scope: CoroutineScope,
        context: Context,
        macAddress: String,
        uuid: UUID
    ): Unit = context.run {
        bluetoothDiscoveryController.stopFindingBluetoothDevices(context)

        val remoteDevice = getBluetoothAdapter()?.getRemoteDevice(macAddress) ?: return

        //******************************************************************************************
        //bluetooth connection solution -> https://www.it1228.com/74366.html
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
                debug(it.localizedMessage.orEmpty())
                debug("bluetooth connection failed")
            }
        }
    }

    override suspend fun sendMessage(bytes: Int) {
        kotlin.runCatching {
            debug("outputStream is ${if (outputStream == null) "" else "not"} null")
            debug("send command: ${bytes.toString(2)}")

            with(commandToByteSequence(bytes)) {
                outputStream?.write(startCommand)
                outputStream?.write(endCommand)
            }
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

    private fun commandToByteSequence(command: Int) = CompoundCommand(
        startCommand = command shr 8,
        endCommand = command and clearFirst8Bytes
    )

    data class CompoundCommand(
        val startCommand: Int,
        val endCommand: Int
    )
}