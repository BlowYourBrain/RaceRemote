package com.simple.raceremote.utils.bluetooth

import android.bluetooth.BluetoothSocket
import android.content.Context
import com.simple.raceremote.utils.debug
import java.util.UUID

interface IBluetoothConnection {

    suspend fun sendMessage(bytes: Int)

    /**
     * @return name of connected bluetooth device, null otherwise
     * */
    suspend fun connectWithDevice(context: Context, macAddress: String, uuid: UUID): String?

    fun closeConnection()
}

class BluetoothConnection(
    private val bluetoothDiscoveryController: IBluetoothDevicesDiscoveryController
) : IBluetoothConnection {
    private companion object {
        const val clearFirst8Bytes = 0b0000_0000_1111_1111
    }

    private var bluetoothSocket: BluetoothSocket? = null

    private val inStream get() = bluetoothSocket?.inputStream
    private val outputStream get() = bluetoothSocket?.outputStream

    override suspend fun connectWithDevice(
        context: Context,
        macAddress: String,
        uuid: UUID
    ): String? {
        bluetoothDiscoveryController.stopFindingBluetoothDevices(context)

        val remoteDevice = context.getBluetoothAdapter()?.getRemoteDevice(macAddress) ?: return null

        // ******************************************************************************************
        // bluetooth connection solution -> https://www.it1228.com/74366.html
        bluetoothSocket = remoteDevice.javaClass.getMethod(
            "createRfcommSocket",
            *arrayOf<Class<*>?>(
                Int::class.javaPrimitiveType
            )
        ).invoke(remoteDevice, 1) as BluetoothSocket
        // ******************************************************************************************

        kotlin.runCatching {
            bluetoothSocket?.connect()
            debug("bluetooth connection established")
        }.onFailure {
            debug(it.localizedMessage.orEmpty())
            debug("bluetooth connection failed")
        }
        return remoteDevice.name
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
