package com.simple.raceremote.data

import androidx.activity.ComponentActivity

interface IBluetoothBroadcastReceiver {

    fun registerReceiver(activity: ComponentActivity)

    fun unregisterReceiver(activity: ComponentActivity)
}