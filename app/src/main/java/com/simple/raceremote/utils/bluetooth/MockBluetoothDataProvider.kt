package com.simple.raceremote.utils.bluetooth

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ITEMS_COUNT = 21

object MockBluetoothDataProvider : IBluetoothItemsProvider, IBluetoothDevicesDiscoveryController {

    override val bluetoothDevices: Flow<List<BluetoothItem>>
        get() = emitter.flow

    private val emitter = Emitter<List<BluetoothItem>>(
        initial = emptyList(),
        ITEMS_COUNT
    ) { emitterIndex ->
        List(emitterIndex + 1) {
            createBluetoothItem(it)
        }
    }

    override fun findBluetoothDevices(context: Context) {
        emitter.start()
    }

    override fun stopFindingBluetoothDevices(context: Context) {
        emitter.stop()
    }

    private fun createBluetoothItem(index: Int) =
        BluetoothItem("item #$index", "mac: $index", index % 2 == 0)
}

private class Emitter<T>(
    private val initial: T,
    private val itemCount: Int,
    private val delay: Long = 1000,
    emitAtCreation: Boolean = true,
    private val source: (index: Int) -> T,
) {
    private val _flow: MutableStateFlow<T> = MutableStateFlow(initial)
    private var index = 0

    var isEmitting = emitAtCreation
        private set

    val flow: Flow<T>
        get() = _flow.asStateFlow()

    init {
        //TODO создать свой scope
        GlobalScope.launch {
            while (true) {
                delay(delay)
                if (index < itemCount && isEmitting) {
                    _flow.emit(source.invoke(index))
                    index++
                }
            }
        }
    }

    fun start() {
        _flow.tryEmit(initial)
        isEmitting = true
    }

    fun stop() {
        isEmitting = false
        index = 0
    }
}