package com.simple.raceremote.features.remote_control.presentation.model

sealed class WifiEnterPasswordDialog {
    class Open(val title: String) : WifiEnterPasswordDialog()
    object Close : WifiEnterPasswordDialog()
}