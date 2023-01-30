package com.simple.raceremote.features.remote_control.presentation.model

enum class RemoteDevice(val requestCode: Int){
    WIFI(100_000),
    Bluetooth(100_001)
}