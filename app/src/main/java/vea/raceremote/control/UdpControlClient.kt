package vea.raceremote.control

import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

class UdpControlClient(
    host: String,
    chipId: Long,
    private val bindSocket: (DatagramSocket) -> Unit,
    private val diagnostics: (String) -> Unit = {},
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
) : Closeable {
    private val address: InetAddress
    private val closing = AtomicBoolean(false)
    private val lock = Any()
    private val session: UdpControlSession
    @Volatile private var socket: DatagramSocket? = null
    private val worker: Thread

    init {
        require(isLocalControlAddress(host)) { "Local IPv4 required" }
        address = InetAddress.getByName(host)
        val random = SecureRandom()
        var attempt: Long
        do { attempt = random.nextInt().toLong() and 0xffffffffL } while (attempt == 0L)
        session = UdpControlSession(chipId, attempt, clockMs())
        worker = Thread({ run() }, "rc-udp-control").apply { isDaemon = true; start() }
    }

    fun snapshot() = synchronized(lock) { session.snapshot() }
    fun input(throttle: Int, steering: Int) = synchronized(lock) {
        if (!closing.get()) session.input(throttle, steering)
    }

    private fun run() {
        try {
            DatagramSocket().use { udp ->
                socket = udp
                bindSocket(udp)
                udp.connect(address, UdpControlPacket.PORT)
                udp.soTimeout = 5
                val buffer = ByteArray(256)
                while (true) {
                    val outgoing = synchronized(lock) {
                        if (closing.get()) session.stop()
                        session.next(clockMs())
                    }
                    if (outgoing != null) {
                        val bytes = outgoing.encode()
                        udp.send(DatagramPacket(bytes, bytes.size))
                        diagnostics("sent kind=${outgoing.kind} seq=${outgoing.sequence} basis=${outgoing.basis}")
                    }
                    if (snapshot().phase == UdpControlSession.Phase.CLOSED) break
                    try {
                        val datagram = DatagramPacket(buffer, buffer.size)
                        udp.receive(datagram)
                        val p = UdpControlPacket.decode(datagram.data.copyOf(datagram.length))
                        val accepted = synchronized(lock) { p != null && session.receive(p, clockMs()) }
                        diagnostics("received seq=${p?.sequence} accepted=$accepted")
                    } catch (_: SocketTimeoutException) { }
                }
            }
        } catch (e: Exception) {
            synchronized(lock) { session.stop("Transport failure: ${e.javaClass.simpleName}") }
            diagnostics("UDP failure: ${e.javaClass.simpleName}")
        } finally {
            socket = null
            synchronized(lock) { session.stop() }
            diagnostics("UDP closed: ${snapshot().reason}")
        }
    }

    override fun close() {
        closing.set(true)
        synchronized(lock) { session.input(0, 0) }
        if (Thread.currentThread() != worker) {
            worker.join(300)
            if (worker.isAlive) { socket?.close(); worker.join(100) }
        }
    }
}

internal fun isLocalControlAddress(host: String): Boolean {
    val octets = host.split('.').map { it.toIntOrNull() }
    return octets.size == 4 && octets.all { it != null && it in 0..255 } &&
        (octets[0] == 10 || octets[0] == 127 || (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 172 && octets[1]!! in 16..31))
}
