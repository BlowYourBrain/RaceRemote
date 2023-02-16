package com.simple.raceremote.utils.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager

fun Context.getConnectivityManager(): ConnectivityManager? =
    getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

fun Context.isWifiEnabled() = getWiFiManager()?.isWifiEnabled ?: false

fun Context.getWiFiManager(): WifiManager? = getSystemService(Context.WIFI_SERVICE) as? WifiManager