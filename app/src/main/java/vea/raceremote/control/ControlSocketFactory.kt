package vea.raceremote.control

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/** Control frames should leave immediately, including on a Wi-Fi-bound socket. */
class ControlSocketFactory(private val delegate: SocketFactory) : SocketFactory() {
    private fun Socket.immediate() = apply { tcpNoDelay = true }
    override fun createSocket(): Socket = delegate.createSocket().immediate()
    override fun createSocket(host: String, port: Int): Socket = delegate.createSocket(host, port).immediate()
    override fun createSocket(host: String, port: Int, local: InetAddress, localPort: Int): Socket =
        delegate.createSocket(host, port, local, localPort).immediate()
    override fun createSocket(host: InetAddress, port: Int): Socket = delegate.createSocket(host, port).immediate()
    override fun createSocket(host: InetAddress, port: Int, local: InetAddress, localPort: Int): Socket =
        delegate.createSocket(host, port, local, localPort).immediate()
}
