package vea.raceremote

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vea.raceremote.app.MainActivity
import vea.raceremote.control.ControlPacket

class CarVideoSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun changingCarClearsPreviousVideoAndRequiresExplicitRestart() {
        val firstVideo = MockWebServer()
        val nextVideo = MockWebServer()
        val control = MockWebServer()
        val drives = AtomicInteger()
        firstVideo.enqueue(stream(Color.BLUE))
        nextVideo.enqueue(stream(Color.RED))
        control.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 61, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val packet = ControlPacket.decode(bytes.toByteArray())!!
                if (packet.kind == ControlPacket.DRIVE) drives.incrementAndGet()
                if (packet.kind != ControlPacket.STOP)
                    webSocket.send(ByteString.of(*packet.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        firstVideo.start(); nextVideo.start(); control.start(1337)
        try {
            compose.onNodeWithTag("car_address").performTextReplacement("192.168.4.1")
            compose.onNodeWithTag("show_video").performClick()
            compose.onNodeWithTag("video_address").performTextReplacement(firstVideo.url("/stream").toString())
            compose.onNodeWithTag("toggle_video").performClick()
            awaitPixel { Color.blue(it) > 200 && Color.red(it) < 20 }

            compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
            compose.onNodeWithTag("video_status").assertTextContains("Видео выключено")
            compose.onNodeWithTag("video_address").assertTextContains("http://127.0.0.1:81/stream")
            awaitPixel { Color.red(it) < 10 && Color.green(it) < 10 && Color.blue(it) < 10 }
            assertEquals(0, control.requestCount)
            compose.onNodeWithTag("throttle").assertIsNotEnabled()

            // Separate camera addresses remain supported, but require a new start.
            compose.onNodeWithTag("video_address").performTextReplacement(nextVideo.url("/stream").toString())
            compose.waitForIdle()
            assertEquals(0, nextVideo.requestCount)
            compose.onNodeWithTag("toggle_video").performClick()
            awaitPixel { Color.red(it) > 200 && Color.blue(it) < 20 }
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            awaitPixel { Color.red(it) > 200 && Color.blue(it) < 20 }
            assertEquals(1, firstVideo.requestCount)
            assertEquals(1, nextVideo.requestCount)
        } finally {
            try { compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED) }
            finally {
                try { control.shutdown() }
                finally { try { firstVideo.shutdown() } finally { nextVideo.shutdown() } }
            }
        }
    }

    private fun awaitPixel(matches: (Int) -> Boolean) {
        // Text replacement focuses the editor and may show the Samsung IME over
        // the image. Inspect the video only once that overlay is gone.
        compose.runOnIdle {
            compose.activity.currentFocus?.clearFocus()
            compose.activity.window.insetsController?.hide(android.view.WindowInsets.Type.ime())
        }
        compose.waitUntil(3000) {
            !compose.activity.window.decorView.rootWindowInsets.isVisible(android.view.WindowInsets.Type.ime())
        }
        var previous: Int? = null
        compose.waitUntil(5000) {
            val center = compose.onNodeWithTag("video_image").fetchSemanticsNode().boundsInWindow.center
            val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            val pixel = screenshot.getPixel(center.x.toInt(), center.y.toInt())
            if (previous != pixel && !matches(pixel)) {
                val file = java.io.File(compose.activity.getExternalFilesDir(null), "car-video-selection-mismatch.png")
                file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.util.Log.d("CarVideoSelectionTest", "ime=${compose.activity.window.decorView.rootWindowInsets.isVisible(android.view.WindowInsets.Type.ime())}")
            }
            screenshot.recycle()
            if (previous != pixel) android.util.Log.d("CarVideoSelectionTest", "pixel=${Integer.toHexString(pixel)} center=$center")
            previous = pixel
            matches(pixel)
        }
    }

    private fun stream(color: Int): MockResponse {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
        bitmap.recycle()
        val body = Buffer()
        repeat(160) {
            body.writeUtf8("\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n").write(jpeg)
        }
        return MockResponse().setHeader("Content-Type", "multipart/x-mixed-replace;boundary=frame")
            .setBody(body).throttleBody(jpeg.size.toLong() + 80, 100, TimeUnit.MILLISECONDS)
    }
}
