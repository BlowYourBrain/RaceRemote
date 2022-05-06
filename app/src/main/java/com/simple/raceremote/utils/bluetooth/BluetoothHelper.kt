package com.simple.raceremote.utils.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.intentFilterOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Must be registered in [ComponentActivity] lifecycle.
 * */
class BluetoothHelper(private val context: Context) : IBluetoothDevicesProvider,
    IBluetoothDevicesDiscoveryController,
    LifecycleEventObserver {

    private companion object {
        const val UNKNOWN_DEVICE = "UNKNOWN_DEVICE"
        const val UNKNOWN_ADDRESS = "UNKNOWN_ADDRESS"
    }

    private val bluetoothBroadcastReceiver = BluetoothDevicesBroadcastReceiver().apply {
        val bluetoothAdapter: BluetoothAdapter? by lazy { context.getBluetoothAdapter() }
        onBluetoothDeviceFound = { bluetoothDevice ->
            bluetoothAdapter?.let { emitBluetoothItem(bluetoothDevice.toBluetoothDevice(it)) }
                ?: debug("bluetooth adapter is missing")
        }
        onBluetoothDevicesDiscovery = {

        }
    }

    private val uniqueBluetoothDevicesCollector = UniqueBluetoothDevicesCollector()
    private val _bluetoothDevices =
        MutableStateFlow<List<BluetoothDevice>>(emptyList())

    override val bluetoothDevices: Flow<List<BluetoothDevice>> =
        _bluetoothDevices.asSharedFlow()

    override val bluetoothDevicesDiscovery: Flow<Boolean>
        get() = TODO("Not yet implemented")


    @SuppressLint("MissingPermission")
    override fun findBluetoothDevices(context: Context): Unit = context.run {
        uniqueBluetoothDevicesCollector.clear()
        _bluetoothDevices.tryEmit(emptyList())
        if (hasBluetoothPermissions()) {
            getBluetoothAdapter()?.startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopFindingBluetoothDevices(context: Context): Unit = context.run {
        if (hasBluetoothPermissions()) {
            getBluetoothAdapter()?.cancelDiscovery()
        }
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

    fun bind(activity: ComponentActivity) {
        activity.lifecycle.addObserver(this)
    }

    private fun LifecycleOwner.toComponentActivity() = this as? ComponentActivity

    private fun registerReceiver(activity: ComponentActivity) {
        activity.registerReceiver(
            bluetoothBroadcastReceiver,
            intentFilterOf(
                ACTION_FOUND,
                BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            )
        )
    }

    private fun unregisterReceiver(activity: ComponentActivity) {
        activity.unregisterReceiver(bluetoothBroadcastReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun AndroidBluetoothDevice.toBluetoothDevice(
        bluetoothAdapter: BluetoothAdapter
    ): BluetoothDevice =
        BluetoothDevice(
            name = name ?: UNKNOWN_DEVICE,
            macAddress = address ?: UNKNOWN_ADDRESS,
            isPaired = bluetoothAdapter
                .bondedDevices
                ?.contains(this)
                ?: false
        )

    private fun emitBluetoothItem(item: BluetoothDevice) {
        uniqueBluetoothDevicesCollector.tryAdd(item)
        _bluetoothDevices.tryEmit(
            uniqueBluetoothDevicesCollector.getCollection()
        )
    }
}

internal class BluetoothDevicesBroadcastReceiver : BroadcastReceiver() {

    enum class BluetoothDeviceDiscovery {
        Start,
        Finished
    }

    var onBluetoothDeviceFound: ((AndroidBluetoothDevice) -> Unit)? = null
    var onBluetoothDevicesDiscovery: ((BluetoothDeviceDiscovery) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                onBluetoothDevicesDiscovery?.invoke(BluetoothDeviceDiscovery.Start)
                debug("discovery started")
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                onBluetoothDevicesDiscovery?.invoke(BluetoothDeviceDiscovery.Finished)
                debug("discovery finished")
            }
            ACTION_FOUND -> {
                val device: AndroidBluetoothDevice? = intent.getParcelableExtra(
                    AndroidBluetoothDevice.EXTRA_DEVICE
                )

                if (device != null) {
                    onBluetoothDeviceFound?.invoke(device)
                }
            }
        }
    }
}

private class UniqueBluetoothDevicesCollector() {
    private var bluetoothDevicesMacAdresses = mutableSetOf<String>()
    private var bluetoothDevicesList =
        mutableListOf<BluetoothDevice>()

    fun getCollection(): List<BluetoothDevice> {
        //creates new collection to make internal state immutable from outer source
        return bluetoothDevicesList.toList()
    }

    fun tryAdd(item: BluetoothDevice) {
        if (!bluetoothDevicesMacAdresses.contains(item.macAddress)) {
            bluetoothDevicesMacAdresses.add(item.macAddress)
            bluetoothDevicesList.add(item)
        }
    }

    fun clear() {
        bluetoothDevicesMacAdresses = mutableSetOf()
        bluetoothDevicesList = mutableListOf()
    }
}