package vea.raceremote

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vea.raceremote.app.MainActivity
import vea.raceremote.control.ControlPacket

/** Real Android gestures and rendering, loopback servers; never sends to a physical car. */
class DrivingModeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun twoThumbsCancelAndVideoSurviveModeChanges() {
        val control = MockWebServer()
        val video = MockWebServer()
        val latest = AtomicReference<ControlPacket>()
        val drives = AtomicInteger()
        control.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 42, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind == ControlPacket.DRIVE) { latest.set(p); drives.incrementAndGet() }
                if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(20, 25, 40))
            drawText("SYNTHETIC FPV TEST", 35f, 90f, Paint().apply { color = Color.WHITE; textSize = 40f })
            drawRect(220f, 180f, 420f, 380f, Paint().apply { color = Color.rgb(255, 160, 0) })
        }
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }.toByteArray()
        bitmap.recycle()
        val part = Buffer().writeUtf8("\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n").write(jpeg).readByteArray()
        val body = Buffer().apply { repeat(1200) { write(part) } }
        video.enqueue(MockResponse().setHeader("Content-Type", "multipart/x-mixed-replace; boundary=frame")
            .setBody(body).throttleBody(part.size.toLong(), 33, java.util.concurrent.TimeUnit.MILLISECONDS))
        control.start(1337); video.start()
        try {
            compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("show_video").performClick()
            compose.onNodeWithTag("video_address").performTextReplacement(video.url("/stream").toString())
            compose.onNodeWithTag("toggle_video").performClick()
            compose.waitUntil(5000) {
                runCatching { compose.onNodeWithTag("video_status").assertTextContains("Получено", substring = true) }.isSuccess
            }
            val setup = compose.onNodeWithTag("video_image").fetchSemanticsNode().boundsInRoot
            screenshot("driving-setup.png")
            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("video_address").assertDoesNotExist()
            val race = compose.onNodeWithTag("video_image").fetchSemanticsNode().boundsInRoot
            assertTrue("Video area should grow: $setup -> $race", race.width * race.height > setup.width * setup.height * 1.3f)
            screenshot("driving-fpv.png")
            val gas = compose.onNodeWithTag("drive_throttle").fetchSemanticsNode().boundsInRoot
            val wheel = compose.onNodeWithTag("drive_steering").fetchSemanticsNode().boundsInRoot
            val gasForward = gas.center + Offset(0f, -gas.height * .3f)
            val wheelRight = wheel.center + Offset(wheel.width * .3f, 0f)
            compose.onRoot().performTouchInput {
                down(0, gas.center); down(1, wheel.center)
                moveTo(0, gasForward); moveTo(1, wheelRight)
            }
            compose.waitUntil(3000) { latest.get()?.let { it.throttle > 200 && it.steering > 200 } == true }
            compose.onRoot().performTouchInput { up(0) }
            compose.waitUntil(3000) { latest.get()?.let { it.throttle == 0 && it.steering > 200 } == true }
            compose.onRoot().performTouchInput { cancel() }
            compose.waitUntil(3000) { latest.get()?.let { it.throttle == 0 && it.steering == 0 } == true }

            // Leaving the pad clears input, even with the owner still pressed.
            compose.onRoot().performTouchInput { down(0, gas.center); moveTo(0, gasForward) }
            compose.waitUntil(3000) { (latest.get()?.throttle ?: 0) > 200 }
            compose.onRoot().performTouchInput { moveTo(0, Offset(gas.right + 8f, gas.center.y)) }
            compose.waitUntil(3000) { latest.get()?.throttle == 0 }
            compose.onRoot().performTouchInput { up(0) }

            // A second held finger in the same pad must not inherit the throttle.
            compose.onRoot().performTouchInput {
                down(0, gas.center); moveTo(0, gasForward); down(1, gas.center + Offset(5f, 5f))
            }
            compose.waitUntil(3000) { (latest.get()?.throttle ?: 0) > 200 }
            compose.onRoot().performTouchInput { up(0); moveTo(1, gasForward) }
            compose.waitUntil(3000) { latest.get()?.throttle == 0 }
            val neutralAt = drives.get()
            compose.waitUntil(3000) { drives.get() > neutralAt + 5 }
            assertEquals(0, latest.get().throttle)
            compose.onRoot().performTouchInput { up(1) }

            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            compose.onNodeWithTag("video_status").assertTextContains("Получено", substring = true)
            compose.onNodeWithTag("toggle_video").assertTextContains("Выключить")
            assertEquals("Switching layout must not reconnect video", 1, video.requestCount)
            val beforeVideoStop = drives.get()
            compose.onNodeWithTag("toggle_video").performClick()
            compose.waitUntil(3000) { drives.get() > beforeVideoStop + 3 }
            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("drive_throttle").assertIsEnabled()
            compose.onRoot().performTouchInput { down(0, gas.center); moveTo(0, gasForward) }
            compose.waitUntil(3000) { (latest.get()?.throttle ?: 0) > 200 }
            compose.onNodeWithTag("stop_car").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithTag("drive_throttle").assertIsNotEnabled()
            compose.onNodeWithTag("drive_steering").assertIsNotEnabled()
            compose.onNodeWithTag("drive_throttle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0%"))
            compose.onRoot().performTouchInput { cancel() }
        } finally {
            compose.onNodeWithTag("stop_car").performClick()
            control.shutdown(); video.shutdown()
        }
    }

    @Test fun backgroundDuringDragDoesNotResumeControl() {
        val control = MockWebServer()
        val latest = AtomicReference<ControlPacket>()
        val drives = AtomicInteger()
        control.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, 43, 0).encode()))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val p = ControlPacket.decode(bytes.toByteArray())!!
                if (p.kind == ControlPacket.DRIVE) { latest.set(p); drives.incrementAndGet() }
                if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
            }
        }))
        control.start(1337)
        try {
            compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("drive_throttle").performTouchInput {
                down(center); moveTo(center + Offset(0f, -height * .3f))
            }
            compose.waitUntil(3000) { (latest.get()?.throttle ?: 0) > 200 }
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithTag("drive_throttle").assertIsNotEnabled()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0%"))
            compose.onNodeWithTag("driving_connection_status").assertTextContains("остановлено", substring = true)
            compose.onNodeWithTag("drive_throttle").performTouchInput { cancel() }
            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("connect_car").assertIsEnabled()
            assertEquals("Background/resume must not reconnect", 1, control.requestCount)
        } finally {
            compose.onNodeWithTag("stop_car").performClick()
            control.shutdown()
        }
    }

    private fun screenshot(name: String) {
        val target = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("bench")!!
        File(target, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
