package com.simple.raceremote.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.simple.raceremote.data.BluetoothItem
import com.simple.raceremote.data.IBluetoothDevicesDiscoveryController
import com.simple.raceremote.data.IBluetoothItemsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Must be registered in [ComponentActivity] lifecycle.
 * */
class BluetoothHelper : IBluetoothItemsProvider,
    IBluetoothDevicesDiscoveryController,
    LifecycleEventObserver {

    private companion object {
        const val UNKNOWN_DEVICE = "UNKNOWN_DEVICE"
        const val UNKNOWN_ADDRESS = "UNKNOWN_ADDRESS"
    }

    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE
                )
                device?.toBluetoothItem(context)?.let { emitBluetoothItem(it) }
            }
        }
    }
    private val uniqueBluetoothDevicesCollector = UniqueBluetoothDevicesCollector()
    private val _bluetoothDevices = MutableStateFlow<List<BluetoothItem>>(emptyList())

    override val bluetoothDevices: Flow<List<BluetoothItem>> = _bluetoothDevices.asSharedFlow()

    override fun findBluetoothDevices(context: Context): Unit = context.run {
        uniqueBluetoothDevicesCollector.clear()
        _bluetoothDevices.tryEmit(emptyList())
        getBluetoothAdapter()?.startDiscovery()
    }

    override fun stopFindingBluetoothDevices(context: Context): Unit = context.run {
        getBluetoothAdapter()?.cancelDiscovery()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event.targetState) {
            Lifecycle.State.CREATED -> {
                source.toComponentActivity()?.let { registerReceiver(it) }
            }
            Lifecycle.State.DESTROYED -> {
                source.toComponentActivity()?.let { unregisterReceiver(it) }
            }
        }
    }

    fun bind(activity: ComponentActivity){
        activity.lifecycle.addObserver(this)
    }

    private fun LifecycleOwner.toComponentActivity() = this as? ComponentActivity

    private fun registerReceiver(activity: ComponentActivity) {
        activity.registerReceiver(
            bluetoothBroadcastReceiver,
            intentFilterOf(ACTION_FOUND)
        )
    }

    private fun unregisterReceiver(activity: ComponentActivity) {
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
        uniqueBluetoothDevicesCollector.tryAdd(item)
        _bluetoothDevices.tryEmit(
            uniqueBluetoothDevicesCollector.getCollection()
        )
    }
}

private class UniqueBluetoothDevicesCollector(){
    private var bluetoothDevicesSet = mutableSetOf<BluetoothItem>()
    private var bluetoothDevicesList = mutableListOf<BluetoothItem>()

    fun getCollection(): List<BluetoothItem> {
        //creates new collection to make internal state immutable from outer source
        return bluetoothDevicesList.toList()
    }
    fun tryAdd(item: BluetoothItem){
        if (!bluetoothDevicesSet.contains(item)){
            bluetoothDevicesList.add(item)
        }
    }

    fun clear(){
        bluetoothDevicesSet = mutableSetOf()
        bluetoothDevicesList = mutableListOf()
    }
}