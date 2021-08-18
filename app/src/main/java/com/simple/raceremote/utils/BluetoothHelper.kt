package com.simple.raceremote.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class BluetoothHelper(private val activity: ComponentActivity) : LifecycleObserver {

    init {
        activity.lifecycle.addObserver(this)
    }


    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                }
            }
        }
    }


    fun onFound() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreated() {
        activity.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        activity.unregisterReceiver(receiver)
    }

}