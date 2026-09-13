package vea.raceremote.features.remote_control.data

import vea.raceremote.features.remote_control.utils.ICompoundCommandExtractor
import vea.raceremote.network.RemoteDeviceApi
import vea.raceremote.network.WebSocketApi
import vea.raceremote.utils.debug
import okio.ByteString

interface IRemoteDeviceRepository {
    enum class Destination {
        Bluetooth,
        WiFi
    }

    //TODO support bluetooth destination
    suspend fun isRemoteDeviceAvailable(): Boolean

    fun sendData(destination: Destination, command: Int)
}

class RemoteDeviceRepository(
    private val extractor: ICompoundCommandExtractor,
    private val remoteDeviceApi: RemoteDeviceApi,
    private val webSocketApi: WebSocketApi,
) : IRemoteDeviceRepository {

    override suspend fun isRemoteDeviceAvailable(): Boolean {
        val result = kotlin.runCatching { remoteDeviceApi.isRemoteDevice() }
            .onFailure {
                debug(it.message ?: "unknown failure", "WIFI_CONNECTION_TAG")
                debug(it.stackTraceToString(), "WIFI_CONNECTION_TAG")
            }
        return result.isSuccess
    }

    override fun sendData(destination: IRemoteDeviceRepository.Destination, command: Int) {
        when (destination) {
            IRemoteDeviceRepository.Destination.WiFi -> {
                webSocketApi.sendMessage(
                    ByteString.of(
                        extractor.extractStartCommand(command),
                        extractor.extractEndCommand(command)
                    )
                )
            }

            IRemoteDeviceRepository.Destination.Bluetooth -> {
                //todo support bluetooth
            }
        }
    }
}