package vea.raceremote.control

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import org.junit.Assert.*
import org.junit.Test

class CarControlTest {
    @Test fun codecMatchesFirmwareVectorAndRejectsMalformedInput() {
        val hex = "524301027856341201efcdab18fce803"
        val expected = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val packet = ControlPacket(ControlPacket.DRIVE, 0x12345678, 0xabcdef01, -1000, 1000)
        assertArrayEquals(expected, packet.encode())
        assertEquals(packet, ControlPacket.decode(expected))
        assertNull(ControlPacket.decode(expected.copyOf(15)))
        for (offset in listOf(0, 1, 2, 3)) {
            assertNull(ControlPacket.decode(expected.copyOf().also { it[offset] = 99 }))
        }
        assertNull(ControlPacket.decode(expected.copyOf().also { it[12] = -1; it[13] = 127 }))
    }

    @Test fun persistentSocketWaitsForAckAndStopsOnDeadline() {
        val now = AtomicLong(0)
        val packets = LinkedBlockingQueue<ControlPacket>()
        val sockets = LinkedBlockingQueue<WebSocket>()
        val server = MockWebServer()
        val http = OkHttpClient()
        val client = CarControlClient { now.get() }
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sockets.add(webSocket)
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 42, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                ControlPacket.decode(bytes.toByteArray())?.let { packets.add(it) }
            }
        }))
        server.start()
        try {
            client.connect("127.0.0.1", http, server.port)
            val arm = packets.poll(3, TimeUnit.SECONDS)!!
            assertEquals(ControlPacket(ControlPacket.ARM, 42, 1), arm)
            val peer = sockets.poll(1, TimeUnit.SECONDS)!!
            assertNull(packets.poll(80, TimeUnit.MILLISECONDS)) // No queue while waiting.
            peer.send(ByteString.of(*arm.copy(kind = ControlPacket.ACK).encode()))
            await { client.state.value.connected }
            client.setInput(.7f, -.3f)
            // A neutral heartbeat may precede the input change; acknowledge it.
            var drive = packets.poll(2, TimeUnit.SECONDS)!!
            if (drive.throttle == 0) {
                peer.send(ByteString.of(*drive.copy(kind = ControlPacket.ACK).encode()))
                drive = packets.poll(2, TimeUnit.SECONDS)!!
            }
            assertEquals(700, drive.throttle); assertEquals(-300, drive.steering)
            assertEquals(1, server.requestCount)
            now.set(200)
            await { !client.state.value.connected }
            assertEquals(0, client.state.value.throttle)
        } finally {
            client.close(); server.shutdown()
            http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll()
        }
    }

    @Test fun aWrongSessionAckCannotEnableControl() {
        val server = MockWebServer()
        val http = OkHttpClient()
        val client = CarControlClient()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 42, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind == ControlPacket.ARM) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK, token = 43).encode()))
            }
        }))
        server.start()
        try {
            client.connect("127.0.0.1", http, server.port)
            await { !client.state.value.connecting }
            assertFalse(client.state.value.connected)
            assertEquals("Ответ не соответствует сессии", client.state.value.message)
        } finally {
            client.close(); server.shutdown()
            http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll()
        }
    }

    @Test fun publicHostsAreRejectedBeforeConnecting() {
        val client = CarControlClient()
        val http = OkHttpClient()
        try {
            for (host in listOf("8.8.8.8", "example.com", "192.168.1.999", "192.168.1.1/path")) {
                client.connect(host, http)
                assertFalse(client.state.value.connecting)
                assertFalse(client.state.value.connected)
            }
        } finally { client.close(); http.dispatcher().executorService().shutdownNow() }
    }

    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < end) Thread.sleep(5)
        assertTrue("Condition did not become true", condition())
    }
}
