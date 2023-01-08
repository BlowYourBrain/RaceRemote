package com.simple.raceremote.utils

import android.util.Log

private const val TAG = "DEBUG_APP"

fun debug(value: String, tag: String? = null) {
    Log.d(tag ?: TAG, value)
}
