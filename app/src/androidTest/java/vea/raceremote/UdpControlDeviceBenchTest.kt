package vea.raceremote

import androidx.test.platform.app.InstrumentationRegistry
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import vea.raceremote.control.*

/** Explicit host argument required; refuses every firmware with physical outputs enabled. */
class UdpControlDeviceBenchTest {
    @Test fun driveReverseStopAndSilentWatchdogOnNullBackend() {
        val args = InstrumentationRegistry.getArguments()
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
            UdpControlClient(host, endpoint.chipId, {}).use { client ->
                await(4000) { client.snapshot().phase == UdpControlSession.Phase.ACTIVE }
                val started = System.nanoTime()
                val duration = 30_000_000_000L
                while (System.nanoTime() - started < duration) {
                    client.input(if (System.nanoTime() - started < duration / 2) 200 else -200, 100)
                    Thread.sleep(20)
                    assertEquals(client.snapshot().reason, UdpControlSession.Phase.ACTIVE, client.snapshot().phase)
                }
                val value = status()
                assertTrue(value.getBoolean("armed"))
                assertEquals(-200, value.getInt("throttle"))
                assertEquals(100, value.getInt("steering"))
                android.util.Log.i("UdpRecoveryBench", "stable30s ${client.snapshot()}")
            }
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
