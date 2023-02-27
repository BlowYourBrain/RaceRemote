package com.simple.raceremote.utils.wifi

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.ACTION_PICK_WIFI_NETWORK

fun Context.getConnectivityManager(): ConnectivityManager? =
    getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

fun Context.isWifiEnabled() = getWiFiManager()?.isWifiEnabled ?: false

fun Context.getWiFiManager(): WifiManager? = getSystemService(Context.WIFI_SERVICE) as? WifiManager
fun Context.pickWifiNetwork() = startActivity(Intent(ACTION_PICK_WIFI_NETWORK).apply { addFlags(FLAG_ACTIVITY_NEW_TASK) })