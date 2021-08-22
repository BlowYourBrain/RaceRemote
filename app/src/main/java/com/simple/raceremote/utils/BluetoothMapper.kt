package com.simple.raceremote.utils

import com.simple.raceremote.screens.BluetoothItem
import com.simple.raceremote.screens.TwoColumnRows


fun List<BluetoothItem>.map(): List<TwoColumnRows> =
    List((size + 1) / 2) {
        val indexInData = it * 2
        TwoColumnRows(
            get(indexInData),
            if (indexInData + 1 < size) get(indexInData + 1) else null
        )
    }

