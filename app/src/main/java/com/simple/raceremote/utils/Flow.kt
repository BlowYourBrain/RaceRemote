package com.simple.raceremote.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/**
 * Creates single event channel with default policy [BufferOverflow.DROP_OLDEST]
 * @param onBufferOverflow default policy, one of [BufferOverflow].
 * */
fun <T> singleEventChannel(
    initial: T? = null,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST
): Channel<T> {
    return Channel<T>(onBufferOverflow = onBufferOverflow).also {
        if (initial != null) {
            it.trySend(initial)
        }
    }
}