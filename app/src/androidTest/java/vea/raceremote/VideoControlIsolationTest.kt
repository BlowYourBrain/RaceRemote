package vea.raceremote

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import okio.Buffer
import org.junit.Rule
import org.junit.Test
import vea.raceremote.app.MainActivity
import vea.raceremote.control.ControlPacket

class VideoControlIsolationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun repeatedCaptureTimesOutVideoWhileControlContinuesAndReconnectStartsFresh() {
        val control = MockWebServer()
        val video = MockWebServer()
        val drives = AtomicInteger()
        control.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 43, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val packet = ControlPacket.decode(bytes.toByteArray())!!
                if (packet.kind == ControlPacket.DRIVE) drives.incrementAndGet()
                if (packet.kind != ControlPacket.STOP)
                    webSocket.send(ByteString.of(*packet.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
        bitmap.recycle()
        fun response(repeated: Boolean): MockResponse {
            val body = Buffer()
            repeat(80) { index ->
                val timestamp = if (repeated) "9000.000000" else "0.%06d".format(java.util.Locale.ROOT, index * 1000)
                body.writeUtf8("\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n" +
                    "X-Sequence: $index\r\nX-Timestamp: $timestamp\r\n\r\n").write(jpeg)
            }
            return MockResponse().setHeader("Content-Type", "multipart/x-mixed-replace;boundary=frame")
                .setBody(body).throttleBody(jpeg.size.toLong() + 110, 100, TimeUnit.MILLISECONDS)
        }
        video.enqueue(response(repeated = true))
        video.enqueue(response(repeated = false))
        control.start(1337); video.start()
        try {
            compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("show_video").performClick()
            compose.onNodeWithTag("video_address").performTextReplacement(video.url("/stream").toString())
            compose.onNodeWithTag("toggle_video").performClick()
            compose.waitUntil(5000) {
                runCatching { compose.onNodeWithTag("video_status").assertTextContains("Нет свежего видео") }.isSuccess
            }
            val afterFailure = drives.get()
            compose.waitUntil(3000) { drives.get() >= afterFailure + 3 }
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            compose.onNodeWithTag("throttle").assertIsEnabled()
            compose.onNodeWithTag("steering").assertIsEnabled()
            compose.onNodeWithTag("toggle_video").performClick()
            compose.waitUntil(5000) {
                runCatching { compose.onNodeWithTag("video_status").assertTextContains("Принято", substring = true) }.isSuccess
            }
            compose.onNodeWithTag("toggle_video").assertTextContains("Выключить")
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            org.junit.Assert.assertEquals(2, video.requestCount)
            org.junit.Assert.assertEquals(1, control.requestCount)
        } finally {
            try { compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED) }
            finally { try { control.shutdown() } finally { video.shutdown() } }
        }
    }

    @Test fun unavailableVideoDoesNotStopControlAndBackgroundStopsBoth() {
        val control = MockWebServer()
        val video = MockWebServer()
        val drives = AtomicInteger()
        control.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 42, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind == ControlPacket.DRIVE) drives.incrementAndGet()
                if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        video.enqueue(MockResponse().setResponseCode(503))
        control.start(1337); video.start()
        try {
            compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("show_video").performClick()
            compose.onNodeWithTag("video_address").performTextReplacement(video.url("/stream").toString())
            compose.onNodeWithTag("toggle_video").performClick()
            compose.waitUntil(5000) { video.requestCount > 0 }
            compose.waitUntil(5000) {
                runCatching { compose.onNodeWithTag("video_status").assertTextContains("Видео недоступно", substring = true) }.isSuccess
            }
            val afterFailure = drives.get()
            compose.waitUntil(3000) { drives.get() >= afterFailure + 3 }
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            compose.onNodeWithTag("throttle").assertIsEnabled()
            compose.onNodeWithTag("steering").assertIsEnabled()
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithTag("connection_status").assertTextContains("остановлено", substring = true)
            compose.onNodeWithTag("video_status").assertTextContains("Видео выключено")
        } finally {
            try { compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED) }
            finally { try { control.shutdown() } finally { video.shutdown() } }
        }
    }
}
