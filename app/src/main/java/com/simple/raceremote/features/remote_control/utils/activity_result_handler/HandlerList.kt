package com.simple.raceremote.features.remote_control.utils.activity_result_handler

import android.content.Intent

class HandlerList {

    private val innerList = mutableListOf<ActivityResultHandler<*>>()

    fun addHandler(handler: ActivityResultHandler<*>) {
        innerList.add(handler)
    }

    fun handle(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        for (handler in innerList) {
            if (handler.handle(requestCode, resultCode, data)) {
                break
            }
        }
    }
}