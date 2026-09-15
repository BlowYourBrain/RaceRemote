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

class IdentityHandshakeTest {
    private val chip = 0xa06da69ef0c8L
    private val token = 0x12345678L
    private val identity = "5243018278563412c8f09ea66da00000".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val hello get() = ControlPacket(ControlPacket.HELLO, token, 0).encode()

    @Test fun identityMatchesCppGoldenAndRejectsMalformedFields() {
        assertEquals(CarIdentity(token, chip), IdentityPacket.decode(identity))
        for (index in listOf(0, 1, 2, 3, 14, 15))
            assertNull(IdentityPacket.decode(identity.copyOf().also { it[index] = 99 }))
        assertNull(IdentityPacket.decode(identity.copyOf(15)))
        assertNull(IdentityPacket.decode(identity.copyOf(17)))
        assertNull(IdentityPacket.decode(identity.copyOf().also { bytes -> (4..7).forEach { bytes[it] = 0 } }))
        assertNull(IdentityPacket.decode(identity.copyOf().also { bytes -> (8..13).forEach { bytes[it] = 0 } }))
    }

    @Test fun matchingIdentityAndHelloCanArm() = handshake(listOf(identity, hello), null)
    @Test fun anotherBoardAtSameAddressCannotArm() = handshake(
        listOf(identity.copyOf().also { it[8] = (it[8].toInt() xor 1).toByte() }, hello), "По этому адресу другая машинка")
    @Test fun legacyHelloCannotSilentlyBypassRequiredIdentity() = handshake(listOf(hello), "Машинка не подтвердила свой ID")
    @Test fun helloFromAnotherSessionCannotArm() = handshake(
        listOf(identity, ControlPacket(ControlPacket.HELLO, token + 1, 0).encode()), "ID и управление относятся к разным сессиям")
    @Test fun duplicateIdentityCannotArm() = handshake(listOf(identity, identity, hello), "Ошибка протокола")
    @Test fun lateIdentityCannotArm() = handshake(listOf(identity, hello), "Нет свежего ответа — остановлено", "Received identity")
    @Test fun identityDoesNotExtendHelloDeadline() = handshake(listOf(identity, hello), "Нет свежего ответа — остановлено", "Received bytes=")

    @Test fun identityAloneCannotArm() {
        val server = MockWebServer()
        val http = OkHttpClient()
        val frames = LinkedBlockingQueue<ByteString>()
        val peers = LinkedBlockingQueue<WebSocket>()
        val client = CarControlClient(clockMs = { 0 })
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                peers.add(webSocket); webSocket.send(ByteString.of(*identity))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { frames.add(bytes) }
        }))
        server.start()
        try {
            client.connect("127.0.0.1", http, server.port, chip)
            assertNotNull(peers.poll(3, TimeUnit.SECONDS))
            assertNull(frames.poll(150, TimeUnit.MILLISECONDS))
            assertFalse(client.state.value.connected)
            assertTrue(client.state.value.connecting)
        } finally { client.close(); server.shutdown(); http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll() }
    }

    private fun handshake(messages: List<ByteArray>, rejection: String?, advanceOn: String? = null) {
        val now = AtomicLong(0)
        val arms = LinkedBlockingQueue<ControlPacket>()
        val server = MockWebServer()
        val http = OkHttpClient()
        val client = CarControlClient(diagnostics = { message ->
            if (advanceOn != null && message.startsWith(advanceOn)) now.set(4000)
        }, clockMs = { now.get() })
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { messages.forEach { webSocket.send(ByteString.of(*it)) } }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind == ControlPacket.ARM) arms.add(p)
                if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        server.start()
        try {
            client.connect("127.0.0.1", http, server.port, chip)
            assertEquals(IdentityPacket.PATH, server.takeRequest(3, TimeUnit.SECONDS)!!.path)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (client.state.value.connecting && System.nanoTime() < deadline) Thread.sleep(5)
            assertFalse("Handshake did not finish", client.state.value.connecting)
            if (rejection == null) {
                assertTrue(client.state.value.connected)
                assertEquals(ControlPacket(ControlPacket.ARM, token, 1), arms.poll(1, TimeUnit.SECONDS))
            } else {
                assertFalse(client.state.value.connected)
                assertEquals(rejection, client.state.value.message)
                assertNull("Rejected identity must never send ARM", arms.poll(100, TimeUnit.MILLISECONDS))
            }
        } finally { client.close(); server.shutdown(); http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll() }
    }
}
