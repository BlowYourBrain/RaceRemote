package vea.raceremote

import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vea.raceremote.app.MainActivity
import vea.raceremote.control.*

/** Rebuilt screen and real Android UDP sockets; the car is a loopback fixture. */
class UdpControlScreenDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectionDoesNotArmAndExplicitStopClearsHeldInput() {
        val status = MockWebServer()
        val udp = DatagramSocket(3334, InetAddress.getByName("127.0.0.1"))
        val running = AtomicBoolean(true)
        val arms = AtomicInteger()
        val drives = AtomicInteger()
        val stopped = AtomicBoolean()
        val latest = AtomicReference<UdpControlPacket>()
        udp.soTimeout = 100
        val worker = Thread {
            while (running.get()) {
                try {
                    val received = DatagramPacket(ByteArray(256), 256)
                    udp.receive(received)
                    val p = UdpControlPacket.decode(received.data.copyOf(received.length)) ?: continue
                    if (p.chipId != FIXTURE_CHIP) continue
                    val reply = when (p.kind) {
                        UdpControlPacket.OPEN -> p.copy(kind = UdpControlPacket.HELLO, token = 42)
                        UdpControlPacket.ARM -> { arms.incrementAndGet(); p.copy(kind = UdpControlPacket.ACK) }
                        UdpControlPacket.DRIVE -> { latest.set(p); drives.incrementAndGet(); p.copy(kind = UdpControlPacket.ACK, basis = 0) }
                        UdpControlPacket.STOP -> { stopped.set(true); latest.set(p); null }
                        else -> null
                    }
                    if (reply != null) {
                        val bytes = reply.encode()
                        udp.send(DatagramPacket(bytes, bytes.size, received.address, received.port))
                    }
                } catch (_: SocketTimeoutException) { }
                catch (e: Exception) { if (running.get()) throw e }
            }
        }.apply { isDaemon = true; start() }
        status.enqueue(MockResponse().setBody("""{"protocol":1,"acceptingControl":true,"controlTransport":"udp-v1","controlPort":3334,"chipId":"123456"}"""))
        status.start()
        try {
            compose.runOnUiThread {
                compose.activity.setContent {
                    MaterialTheme {
                        CarControlScreen(carPicker = { enabled, pick ->
                            Button(onClick = { pick(DiscoveredCar("127.0.0.1", 3334, FIXTURE_CHIP, false, "udp-v1", 2)) },
                                enabled = enabled, modifier = Modifier.testTag("pick_udp")) { Text("UDP fixture") }
                        }, statusPort = status.port)
                    }
                }
            }
            compose.onNodeWithTag("pick_udp").performClick()
            assertEquals(0, arms.get())
            assertEquals(0, status.requestCount)
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            compose.onNodeWithTag("driving_mode").performClick()
            compose.onNodeWithTag("drive_throttle").performTouchInput { down(center); moveTo(center + Offset(0f, -height * .3f)) }
            compose.waitUntil(3000) { (latest.get()?.throttle ?: 0) > 200 }
            compose.onNodeWithTag("stop_car").performClick()
            compose.waitUntil(3000) { stopped.get() }
            compose.onNodeWithTag("drive_throttle").assertIsNotEnabled()
            compose.onNodeWithTag("drive_throttle").performTouchInput { cancel() }
            assertEquals(0, latest.get().throttle)
            assertEquals(0, latest.get().steering)
            assertEquals(1, arms.get())
            assertEquals(1, status.requestCount)
        } finally {
            try { compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED) }
            finally { running.set(false); udp.close(); worker.join(500); status.shutdown() }
        }
    }
}
