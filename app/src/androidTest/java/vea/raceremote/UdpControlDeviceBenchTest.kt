package vea.raceremote

import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Rule
import vea.raceremote.app.MainActivity
import vea.raceremote.control.*

/** Explicit host argument required; refuses every firmware with physical outputs enabled. */
class UdpControlDeviceBenchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun driveReverseStopAndSilentWatchdogOnNullBackend() {
        val args = InstrumentationRegistry.getArguments()
        val holdMs = args.getString("udpBenchHoldMs")?.toLongOrNull() ?: 15_000L
        require(holdMs in 1000..60_000)
        val host = args.getString("udpBenchHost")
        assumeTrue("Supply udpBenchHost for the physical null-backend bench", host != null)
        val http = OkHttpClient()
        fun status(): JSONObject = http.newCall(Request.Builder().url("http://$host/status").build()).execute().use {
            check(it.code() == 200)
            JSONObject(requireNotNull(it.body()).string()).also { value ->
                check(!value.getBoolean("outputsEnabled")) { "Null backend required" }
                check(value.getString("actuatorMode") == "disabled")
            }
        }
        val initial = status()
        val endpoint = ControlEndpointProbe.load(host!!, http)
        assertEquals("udp-v1", endpoint.transport)
        assertEquals(3334, endpoint.port)
        assertFalse(initial.getBoolean("armed"))
        try {
            compose.onNodeWithTag("find_cars").performClick()
            val selected = compose.onNodeWithTag("discovered_${endpoint.chipId}")
            compose.waitUntil(5000) { runCatching { selected.assertExists() }.isSuccess }
            selected.assertTextContains(" · $host\n", substring = true)
            selected.performClick()
            assertFalse(status().getBoolean("armed"))
            compose.onNodeWithTag("selected_car_identity").assertTextContains(endpoint.chipId.toString(16).padStart(12, '0'), substring = true)
            compose.onNodeWithTag("connect_car").performClick()
            compose.waitUntil(5000) { runCatching { compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено") }.isSuccess }
            compose.onNodeWithTag("driving_mode").performClick()
            val gas = compose.onNodeWithTag("drive_throttle").fetchSemanticsNode().boundsInRoot
            compose.onRoot().performTouchInput { down(0, gas.center) }
            for (forward in listOf(true, false)) {
                compose.onRoot().performTouchInput { moveTo(0, gas.center + Offset(0f, gas.height * if (forward) -.3f else .3f)) }
                val deadline = System.nanoTime() + holdMs * 1_000_000L
                while (System.nanoTime() < deadline) {
                    Thread.sleep(500)
                    compose.onNodeWithTag("driving_connection_status").assertTextContains("Управление подключено")
                }
                val value = status()
                assertTrue(value.getBoolean("armed"))
                assertTrue(if (forward) value.getInt("throttle") > 200 else value.getInt("throttle") < -200)
                assertEquals(0, value.getInt("steering"))
                android.util.Log.i("UdpRecoveryBench", "screenHoldMs=$holdMs forward=$forward throttle=${value.getInt("throttle")}")
            }
            val stop = compose.onNodeWithTag("stop_car").fetchSemanticsNode().boundsInRoot.center
            compose.onRoot().performTouchInput { down(1, stop); up(1) }
            compose.onNodeWithTag("drive_throttle").assertIsNotEnabled()
            compose.onRoot().performTouchInput { cancel() }
            await(1000) { !status().getBoolean("armed") }
            assertNeutral(status())

            // Abandon a separately armed session without sending STOP.
            DatagramSocket().use { udp ->
                udp.connect(InetAddress.getByName(host), 3334)
                udp.soTimeout = 20
                val clock = { System.nanoTime() / 1_000_000 }
                val session = UdpControlSession(endpoint.chipId, 0x12345678, clock())
                val deadline = clock() + 4000
                while (session.snapshot().acknowledgedThrottle != 200 && clock() < deadline) {
                    session.input(200, 0)
                    session.next(clock())?.let { p ->
                        val bytes = p.encode(); udp.send(DatagramPacket(bytes, bytes.size))
                    }
                    try {
                        val datagram = DatagramPacket(ByteArray(256), 256)
                        udp.receive(datagram)
                        UdpControlPacket.decode(datagram.data.copyOf(datagram.length))?.let { session.receive(it, clock()) }
                    } catch (_: java.net.SocketTimeoutException) { }
                }
                assertEquals(200, session.snapshot().acknowledgedThrottle)
                assertTrue(status().getBoolean("armed"))
            }
            Thread.sleep(300)
            assertNeutral(status())
            android.util.Log.i("UdpRecoveryBench", "explicit STOP and silent watchdog neutral verified")
        } finally {
            runCatching { compose.onRoot().performTouchInput { cancel() } }
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            // No physical outputs are enabled; even a failed test must leave the firmware expired.
            Thread.sleep(300)
            assertNeutral(status())
            http.dispatcher().executorService().shutdown()
            http.connectionPool().evictAll()
        }
    }

    private fun assertNeutral(value: JSONObject) {
        assertFalse(value.getBoolean("armed"))
        assertEquals(0, value.getInt("throttle"))
        assertEquals(0, value.getInt("steering"))
        assertEquals(0, value.getJSONObject("drivePlan").getInt("fiPermille"))
        assertEquals(0, value.getJSONObject("drivePlan").getInt("biPermille"))
    }

    private fun await(ms: Long, condition: () -> Boolean) {
        val end = System.nanoTime() + ms * 1_000_000
        while (!condition() && System.nanoTime() < end) Thread.sleep(10)
        assertTrue(condition())
    }
}
