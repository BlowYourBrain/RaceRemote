package vea.raceremote.utils

import android.content.IntentFilter

fun intentFilterOf(vararg filter: String): IntentFilter =
    IntentFilter().apply {
        filter.forEach { addAction(it) }
    }
