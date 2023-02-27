package com.simple.raceremote.features.remote_control.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import com.simple.raceremote.features.remote_control.data.IRemoteDeviceRepository
import com.simple.raceremote.features.remote_control.presentation.IWifiNetwork.NetworkState
import com.simple.raceremote.utils.debug
import com.simple.raceremote.utils.wifi.getConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface IWifiNetwork {
    /**
     * Emit [NetworkState.Match] if it's possible to connect remote device.
     * If it's impossible emit [NetworkState.Mismatch].
     * Emit [NetworkState.Loading] when it's trying to connect remote device.
     * */
    val networkState: Flow<NetworkState?>

    enum class NetworkState {
        Loading,
        Mismatch,
        Match,
    }
}

private const val WIFI_CONNECTION_TAG = "WIFI_CONNECTION_TAG"

class WifiNetwork(
    private val context: Context,
    private val remoteDeviceRepository: IRemoteDeviceRepository
) : IWifiNetwork {

    private var currentJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Mismatch)

    override val networkState = _networkState.asStateFlow()

    init {
        initial()
    }

    private fun initial() {
        setDefaultCallback()
        cancelJobAndCheckNetwork()
    }

    private fun setDefaultCallback() {
        val connectivityManager = context.getConnectivityManager()

        connectivityManager?.registerDefaultNetworkCallback(

            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    cancelJobAndCheckNetwork()
                    debug("onAvailable", WIFI_CONNECTION_TAG)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    onLostNetwork()
                    debug("onLost", WIFI_CONNECTION_TAG)
                }

                override fun onLosing(network: Network, maxMsToLive: Int) = Unit

                override fun onUnavailable() = Unit

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) = Unit

                override fun onLinkPropertiesChanged(
                    network: Network,
                    linkProperties: LinkProperties
                ) = Unit

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) = Unit
            }
        )
    }

    private fun cancelJobAndCheckNetwork() {
        currentJob?.cancel()

        currentJob = coroutineScope.launch {
            _networkState.emit(NetworkState.Loading)
            val isRemoteDeviceAvailable = remoteDeviceRepository.isRemoteDeviceAvailable()
            val networkState = if (isRemoteDeviceAvailable) NetworkState.Match else NetworkState.Mismatch

            _networkState.emit(networkState)
        }
    }

    private fun onLostNetwork() {
        _networkState.tryEmit(NetworkState.Mismatch)
    }
}