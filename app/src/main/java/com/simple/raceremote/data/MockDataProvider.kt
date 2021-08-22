package com.simple.raceremote.data

import com.simple.raceremote.screens.BluetoothItem

object MockDataProvider : BluetoothItemsProvider {

    override fun getBluetoothDevices(): List<BluetoothItem> {
        val list = mutableListOf<BluetoothItem>()
        repeat(30) {
            list.add(BluetoothItem("item #$it", "mac: $it"))
        }
        return list
    }

}