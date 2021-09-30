package com.simple.raceremote.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.simple.raceremote.data.BluetoothItem
import com.simple.raceremote.data.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.data.IBluetoothItemsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

//TODO после внедрения DI привести к обычному классу
object BluetoothHelper : IBluetoothItemsProvider, IBluetoothDevicesDiscoveryController {
    private const val UNKNOWN_DEVICE = "UNKNOWN_DEVICE"
    private const val UNKNOWN_ADDRESS = "UNKNOWN_ADDRESS"

    private val bluetoothDevicesSet = mutableSetOf<BluetoothItem>()

    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOUND) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.toBluetoothItem()?.let { emitBluetoothItem(it) }
            }
        }
    }

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothItem>>(emptyList())

    override val bluetoothDevices: Flow<List<BluetoothItem>>
        get() = _bluetoothDevices.asSharedFlow()

    override fun findBluetoothDevices() {
        bluetoothDevicesSet.clear()
        getBluetoothAdapter().startDiscovery()
    }

    override fun stopFindingBluetoothDevices() {
        getBluetoothAdapter().cancelDiscovery()
    }

    fun registerReceiver(activity: ComponentActivity) {
        activity.registerReceiver(
            bluetoothBroadcastReceiver,
            intentFilterOf(ACTION_FOUND)
        )
    }

    fun unregisterReceiver(activity: ComponentActivity) {
        activity.unregisterReceiver(bluetoothBroadcastReceiver)
    }

    private fun BluetoothDevice.toBluetoothItem(): BluetoothItem =
        BluetoothItem(
            name = name ?: UNKNOWN_DEVICE,
            macAddress = address ?: UNKNOWN_ADDRESS,
            isPaired = getBluetoothAdapter()
                .bondedDevices
                ?.contains(this) ?: false
        )

    private fun emitBluetoothItem(item: BluetoothItem) {
        bluetoothDevicesSet.add(item)
        _bluetoothDevices.tryEmit(bluetoothDevicesSet.toList())
    }
}