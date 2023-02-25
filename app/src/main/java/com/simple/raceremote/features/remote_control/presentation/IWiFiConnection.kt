package com.simple.raceremote.features.remote_control.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.wifi.getConnectivityManager


interface IWiFiConnection {
    fun connect(ssid: String, password: String)
}

class WifiConnection(private val context: Context) : IWiFiConnection {

    companion object {
        private const val WIFI_CONNECTION_TAG = "WIFI_CONNECTION_TAG"
    }

    override fun connect(ssid: String, password: String) {
        val networkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        context.getConnectivityManager()
            ?.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        debug("onAvailable", WIFI_CONNECTION_TAG)
                    }

                    override fun onLosing(network: Network, maxMsToLive: Int) {
                        super.onLosing(network, maxMsToLive)
                        debug("onLosing", WIFI_CONNECTION_TAG)
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        debug("onLost", WIFI_CONNECTION_TAG)
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        debug("onUnavailable", WIFI_CONNECTION_TAG)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        debug("onCapabilitiesChanged", WIFI_CONNECTION_TAG)
                    }

                    override fun onLinkPropertiesChanged(
                        network: Network,
                        linkProperties: LinkProperties
                    ) {
                        super.onLinkPropertiesChanged(network, linkProperties)
                        debug("onLinkPropertiesChanged", WIFI_CONNECTION_TAG)
                    }

                    override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                        super.onBlockedStatusChanged(network, blocked)
                        debug("onBlockedStatusChanged to $blocked", WIFI_CONNECTION_TAG)
                    }
                })

    }

}
