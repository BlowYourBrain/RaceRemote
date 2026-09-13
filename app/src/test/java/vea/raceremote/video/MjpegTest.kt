package vea.raceremote.video

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.junit.Assert.*
import org.junit.Test
import vea.raceremote.control.CarControlClient
import vea.raceremote.control.ControlPacket

class MjpegTest {
    private val jpeg = byteArrayOf(-1, -40, 1, 2, -1, -39)
    private fun part(headers: String = "Content-Type: image/jpeg\r\nContent-Length: 6", bytes: ByteArray = jpeg) =
        "\r\n--frame\r\n$headers\r\n\r\n".toByteArray() + bytes

    @Test fun fragmentedReadsAndQuotedBoundaryPreserveEveryFrame() {
        val payload = part() + part(bytes = jpeg.copyOf().also { it[2] = 3 }) + "\r\n--frame--\r\n".toByteArray()
        val source = object : ByteArrayInputStream(payload) {
            override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, minOf(len, 1))
        }
        val reader = MjpegReader(source, "multipart/x-mixed-replace; boundary=\"frame\"")
        assertArrayEquals(jpeg, reader.nextFrame())
        assertEquals(3.toByte(), reader.nextFrame()!![2])
        assertNull(reader.nextFrame())
    }

    @Test fun incompleteAndUnboundedFramesAreRejected() {
        for (headers in listOf(
            "Content-Type: image/jpeg", "Content-Type: image/jpeg\r\nContent-Length: -1",
            "Content-Type: image/jpeg\r\nContent-Length: 524289",
            "Content-Type: image/jpeg\r\nContent-Length: 9999999999999",
            "Content-Type: image/jpeg\r\nContent-Length: 6\r\ncontent-length: 7",
            "Content-Type: text/html\r\nContent-Length: 6",
            "Content-Type: image/jpeg\r\nX-Long: ${"a".repeat(4097)}",
        )) expectIo { MjpegReader(part(headers).inputStream(), "multipart/x-mixed-replace;boundary=frame").nextFrame() }
        expectIo { MjpegReader(part(bytes = jpeg.copyOf(4)).inputStream(), "multipart/x-mixed-replace;boundary=frame").nextFrame() }
        expectIo { MjpegReader(part(bytes = ByteArray(6)).inputStream(), "multipart/x-mixed-replace;boundary=frame").nextFrame() }
    }

    @Test fun htmlMissingBoundaryAndWrongDelimiterAreRejected() {
        for (type in listOf("text/html", "multipart/x-mixed-replace", "multipart/x-mixed-replace;boundary="))
            expectIo { MjpegReader(part().inputStream(), type) }
        expectIo { MjpegReader(part().inputStream(), "multipart/x-mixed-replace;boundary=other").nextFrame() }
    }

    @Test fun videoEndingPreservesControlAndNewDriveAcknowledgements() {
        val controlServer = MockWebServer()
        val videoServer = MockWebServer()
        val http = OkHttpClient()
        val control = CarControlClient()
        val videoEnded = CountDownLatch(1)
        val frameReceived = CountDownLatch(1)
        val video = MjpegStream({ assertArrayEquals(jpeg, it); frameReceived.countDown() }, { videoEnded.countDown() })
        controlServer.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 42, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        videoServer.enqueue(MockResponse().setHeader("Content-Type", "multipart/x-mixed-replace;boundary=frame")
            .setBody(Buffer().write(part() + "\r\n--frame--\r\n".toByteArray())))
        controlServer.start(); videoServer.start()
        try {
            control.connect("127.0.0.1", http, controlServer.port)
            await { control.state.value.connected }
            video.start(videoServer.url("/stream").toString(), http)
            assertTrue(frameReceived.await(3, TimeUnit.SECONDS))
            assertTrue(videoEnded.await(3, TimeUnit.SECONDS))
            assertTrue(control.state.value.connected)
            control.setInput(.4f, -.2f)
            await { control.state.value.throttle == 400 && control.state.value.steering == -200 }
            assertEquals(1, controlServer.requestCount)
        } finally {
            video.close(); control.close(); controlServer.shutdown(); videoServer.shutdown()
            http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll()
        }
    }

    private fun expectIo(action: () -> Unit) {
        try { action(); fail("Expected IOException") } catch (_: IOException) { }
    }

    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < end) Thread.sleep(5)
        assertTrue(condition())
    }
}
