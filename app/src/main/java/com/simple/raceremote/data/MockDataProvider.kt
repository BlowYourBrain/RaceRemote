package com.simple.raceremote.data

import com.simple.raceremote.screens.BluetoothItem
import com.simple.raceremote.utils.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

private const val ITEMS_COUNT = 21

object MockDataProvider : IBluetoothItemsProvider {

    override val bluetoothDevices: Flow<List<BluetoothItem>>
        get() = createBluetoothDevices()

    private fun createBluetoothDevices(): Flow<List<BluetoothItem>> = flow {
        if (ITEMS_COUNT - 1 > 0) {
            var items = listOf(createBluetoothItem(0))
            repeat(ITEMS_COUNT - 1) {
                items = emitListAndDelay(items, it + 1)
            }
        }
    }

    private fun createBluetoothItem(index: Int) = BluetoothItem("item #$index", "mac: $index", index % 2 == 0)

    private suspend fun FlowCollector<List<BluetoothItem>>.emitListAndDelay(
        list: List<BluetoothItem>,
        index: Int
    ): List<BluetoothItem> {
        return (list + createBluetoothItem(index)).apply {
            debug("list size = ${this.size}")
            emit(this)
            delay(1000)
        }
    }
}