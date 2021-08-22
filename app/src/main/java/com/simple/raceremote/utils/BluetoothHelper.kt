package com.simple.raceremote.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.simple.raceremote.data.IBluetoothItemsProvider
import com.simple.raceremote.screens.BluetoothItem
import com.simple.raceremote.utils.BluetoothHelper.toBluetoothItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

//TODO после внедрения DI привести к обычному классу
object BluetoothHelper : IBluetoothItemsProvider {

    private val bluetoothDevicesList = mutableListOf<BluetoothItem>()
    override val bluetoothDevices: Flow<List<BluetoothItem>>
        get() = _bluetoothDevice.asStateFlow()


    private val _bluetoothDevice = MutableStateFlow<List<BluetoothItem>>(emptyList())

    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.toBluetoothItem()?.let { emitBluetoothItem(it) }
                }
            }
        }

    }

    private fun BluetoothDevice.toBluetoothItem(): BluetoothItem =
        BluetoothItem(
            name = name,
            macAddress = address,
            isPaired = BluetoothAdapter
                .getDefaultAdapter()
                .bondedDevices
                .contains(this)
        )

    private fun emitBluetoothItem(item: BluetoothItem) {
        bluetoothDevicesList.add(item)
        _bluetoothDevice.tryEmit(bluetoothDevicesList)
    }

    fun findBluetoothDevices() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery()
    }

    fun stopFindingBluetoothDevices() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

    fun registerReceiver(activity: ComponentActivity) {
        activity.registerReceiver(
            bluetoothBroadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
    }

    fun unregisterReceiver(activity: ComponentActivity) {
        activity.unregisterReceiver(bluetoothBroadcastReceiver)
    }

}