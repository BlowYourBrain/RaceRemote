package com.simple.raceremote.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.simple.raceremote.data.BluetoothItem
import com.simple.raceremote.data.IBluetoothBroadcastReceiver
import com.simple.raceremote.data.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.data.IBluetoothItemsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class BluetoothHelper : IBluetoothItemsProvider, IBluetoothBroadcastReceiver,
    IBluetoothDevicesDiscoveryController {

    private companion object {
        const val UNKNOWN_DEVICE = "UNKNOWN_DEVICE"
        const val UNKNOWN_ADDRESS = "UNKNOWN_ADDRESS"
    }

    private val bluetoothDevicesSet = mutableSetOf<BluetoothItem>()
    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOUND) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.toBluetoothItem(context)?.let { emitBluetoothItem(it) }
            }
        }
    }

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothItem>>(emptyList())

    override val bluetoothDevices: Flow<List<BluetoothItem>>
        get() = _bluetoothDevices.asSharedFlow()

    override fun findBluetoothDevices(context: Context): Unit = context.run {
        bluetoothDevicesSet.clear()
        _bluetoothDevices.tryEmit(emptyList())
        getBluetoothAdapter()?.startDiscovery()
    }

    override fun stopFindingBluetoothDevices(context: Context): Unit = context.run {
        getBluetoothAdapter()?.cancelDiscovery()
    }

    override fun registerReceiver(activity: ComponentActivity) {
        activity.registerReceiver(
            bluetoothBroadcastReceiver,
            intentFilterOf(ACTION_FOUND)
        )
    }

    override fun unregisterReceiver(activity: ComponentActivity) {
        activity.unregisterReceiver(bluetoothBroadcastReceiver)
    }

    private fun BluetoothDevice.toBluetoothItem(context: Context): BluetoothItem =
        BluetoothItem(
            name = name ?: UNKNOWN_DEVICE,
            macAddress = address ?: UNKNOWN_ADDRESS,
            isPaired = context.getBluetoothAdapter()
                ?.bondedDevices
                ?.contains(this) ?: false
        )

    private fun emitBluetoothItem(item: BluetoothItem) {
        bluetoothDevicesSet.add(item)
        _bluetoothDevices.tryEmit(bluetoothDevicesSet.toList())
    }
}