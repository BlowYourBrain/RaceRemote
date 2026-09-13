package vea.raceremote.control

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.Collections
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveryState(val searching: Boolean = false, val cars: List<DiscoveredCar> = emptyList(), val message: String = "")

/** Three-second, request/response LAN discovery. No control connection is opened. */
class CarDiscovery {
    private val worker = Executors.newSingleThreadExecutor()
    private val mutableState = MutableStateFlow(DiscoveryState())
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var socket: DatagramSocket? = null

    @Synchronized
    fun search(targets: List<InetAddress> = broadcastTargets(), configure: (DatagramSocket) -> Unit = {}) {
        cancel()
        if (targets.isEmpty()) { mutableState.value = DiscoveryState(message = "Нет доступной локальной сети"); return }
        val current = generation
        mutableState.value = DiscoveryState(searching = true, message = "Ищу машинки…")
        worker.execute {
            val found = linkedMapOf<String, DiscoveredCar>()
            try {
                DatagramSocket().use { udp ->
                    synchronized(this) {
                        if (generation != current) return@execute
                        socket = udp
                    }
                    configure(udp)
                    udp.broadcast = true; udp.soTimeout = 100
                    var nonce: Long
                    do { nonce = SecureRandom().nextInt().toLong() and 0xffffffffL } while (nonce == 0L)
                    val query = DiscoveryPacket.query(nonce)
                    val started = System.nanoTime()
                    var lastSend = -500L
                    while (System.nanoTime() - started < 3_000_000_000L) {
                        synchronized(this) { if (generation != current) return@execute }
                        val elapsed = (System.nanoTime() - started) / 1_000_000
                        if (elapsed - lastSend >= 500) {
                            for (target in targets) runCatching { udp.send(DatagramPacket(query, query.size, target, DiscoveryPacket.PORT)) }
                            lastSend = elapsed
                        }
                        val packet = DatagramPacket(ByteArray(25), 25)
                        try { udp.receive(packet) } catch (_: SocketTimeoutException) { continue }
                        if (packet.port != DiscoveryPacket.PORT || !localAddress(packet.address)) continue
                        val car = DiscoveryPacket.reply(packet.data.copyOf(packet.length), nonce, packet.address.hostAddress ?: continue) ?: continue
                        // Keep distinct addresses visible even if IDs are duplicated/spoofed.
                        found["${car.chipId}/${car.host}"] = car
                        if (found.size > 32) found.remove(found.keys.first())
                        synchronized(this) {
                            if (generation == current) mutableState.value = DiscoveryState(true, found.values.toList(), "Ищу машинки…")
                        }
                    }
                    synchronized(this) {
                        if (generation == current) mutableState.value = DiscoveryState(false, found.values.toList(),
                            if (found.isEmpty()) "Машинки не найдены. Можно ввести адрес вручную." else "Выбери машинку")
                    }
                }
            } catch (_: Exception) {
                synchronized(this) {
                    if (generation == current) mutableState.value = DiscoveryState(false, found.values.toList(), "Не удалось выполнить поиск")
                }
            } finally { synchronized(this) { if (generation == current) socket = null } }
        }
    }

    @Synchronized fun cancel() { generation++; socket?.close(); socket = null; mutableState.value = DiscoveryState() }
    @Synchronized fun close() { cancel(); worker.shutdownNow() }

    companion object {
        private fun localAddress(address: InetAddress): Boolean =
            address is Inet4Address && (address.isSiteLocalAddress || address.isLoopbackAddress)

        fun broadcastTargets(): List<InetAddress> = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces()).filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }
                .filter { localAddress(it.address) }
                .mapNotNull { it.broadcast }.distinct()
        }.getOrDefault(emptyList())
    }
}
