package vea.raceremote.control

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in: actual ESP32 Wi-Fi with the production Kotlin client, null backend only. */
class FirmwareUdpDeviceTest {
    @Test(timeout = 180000) fun wifiDiscoveryOwnershipDriveCloseAndSilentWatchdog() {
        val host = System.getenv("RC_UDP_DEVICE_HOST")
        assumeTrue("Explicit physical null-backend host required", host != null)
        require(isLocalControlAddress(host!!))
        val expectedChip = requireNotNull(System.getenv("RC_UDP_DEVICE_CHIP_ID")).toLong(16)
        val holdMs = (System.getenv("RC_UDP_DEVICE_HOLD_MS") ?: "15000").toLong()
        require(holdMs in 1000..60000)
        val http = OkHttpClient.Builder().proxy(Proxy.NO_PROXY).callTimeout(3, TimeUnit.SECONDS).build()
        fun status(): JSONObject = http.newCall(Request.Builder().url("http://$host/status").build()).execute().use {
            check(it.code() == 200)
            val source = requireNotNull(it.body()).source()
            check(!source.request(8193))
            JSONObject(source.readUtf8()).also { value ->
                check(value.getString("chipId").toLong(16) == expectedChip)
                check(!value.getBoolean("outputsEnabled") && value.getString("actuatorMode") == "disabled")
                check(value.getJSONObject("motorDispatch").getString("backend") == "null")
                check(value.getString("firmwareSource") == "reconstructed-udp-v1")
            }
        }
        val initial = status() // Refuse physical GPIO backends before any control datagram.
        val trace = java.util.ArrayDeque<String>()
        assertNeutral(initial)
        val endpoint = ControlEndpointProbe.load(host, http)
        assertEquals(ControlEndpoint("udp-v1", 3334, expectedChip), endpoint)
        fun discover(busy: Boolean) {
            DatagramSocket().use { udp ->
                udp.connect(InetAddress.getByName(host), DiscoveryPacket.PORT); udp.soTimeout = 1500
                val query = DiscoveryPacket.query(0x12345678, 2)
                udp.send(DatagramPacket(query, query.size))
                val reply = DatagramPacket(ByteArray(25), 25); udp.receive(reply)
                assertEquals(DiscoveredCar(host, 3334, expectedChip, busy, "udp-v1", 2),
                    DiscoveryPacket.reply(reply.data.copyOf(reply.length), 0x12345678, host))
            }
        }
        try {
            discover(false)
            UdpControlClient(host, expectedChip, {}, { event ->
                synchronized(trace) {
                    trace.addLast("${System.nanoTime() / 1_000_000} $event")
                    if (trace.size > 256) trace.removeFirst()
                }
            }).use { owner ->
                await { owner.snapshot().phase == UdpControlSession.Phase.ACTIVE }
                discover(true)
                UdpControlClient(host, expectedChip, {}).use { intruder ->
                    Thread.sleep(300)
                    assertEquals(UdpControlSession.Phase.OPENING, intruder.snapshot().phase)
                    assertEquals(UdpControlSession.Phase.ACTIVE, owner.snapshot().phase)
                }
                for (throttle in listOf(300, -300)) {
                    owner.input(throttle, 100)
                    await { owner.snapshot().acknowledgedThrottle == throttle }
                    val end = System.nanoTime() + holdMs * 1_000_000
                    while (System.nanoTime() < end) {
                        Thread.sleep(250)
                        assertEquals(owner.snapshot().reason, UdpControlSession.Phase.ACTIVE, owner.snapshot().phase)
                    }
                    val value = status()
                    assertEquals(throttle, value.getInt("throttle")); assertEquals(100, value.getInt("steering"))
                    val plan = value.getJSONObject("drivePlan")
                    assertTrue(plan.getInt(if (throttle > 0) "fiPermille" else "biPermille") > 0)
                    assertEquals(0, plan.getInt(if (throttle > 0) "biPermille" else "fiPermille"))
                    println("DEVICE holdMs=$holdMs throttle=$throttle ${owner.snapshot()}")
                }
            }
            await { !status().getBoolean("armed") }
            // Device status proves neutral after close; it cannot distinguish STOP from timeout.
            assertNeutral(status()); discover(false)

            // Abandon a separately armed production session, sending no STOP.
            DatagramSocket().use { udp ->
                udp.connect(InetAddress.getByName(host), 3334); udp.soTimeout = 5
                fun now() = System.nanoTime() / 1_000_000
                val session = UdpControlSession(expectedChip, 0x12345678, now())
                val deadline = now() + 4000
                while (session.snapshot().acknowledgedThrottle != 200 && now() < deadline) {
                    session.input(200, -100)
                    session.next(now())?.let { p -> val b = p.encode(); udp.send(DatagramPacket(b, b.size)) }
                    try {
                        val p = DatagramPacket(ByteArray(256), 256); udp.receive(p)
                        UdpControlPacket.decode(p.data.copyOf(p.length))?.let { session.receive(it, now()) }
                    } catch (_: SocketTimeoutException) { }
                }
                assertEquals(200, session.snapshot().acknowledgedThrottle)
                assertTrue(status().getBoolean("armed"))
            }
            Thread.sleep(300)
            assertNeutral(status()); discover(false)
            println("DEVICE full-chip/status/discovery owner/client-close-neutral/silent-watchdog passed; outputs disabled")
        } catch (failure: Throwable) {
            synchronized(trace) { println("DEVICE failure trace:\n" + trace.joinToString("\n")) }
            throw failure
        } finally {
            Thread.sleep(300)
            assertNeutral(status())
            http.dispatcher().executorService().shutdown(); http.connectionPool().evictAll()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 4_000_000_000
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue("Device condition not reached", condition())
    }

    private fun assertNeutral(value: JSONObject) {
        assertFalse(value.getBoolean("armed"))
        assertEquals(0, value.getInt("throttle")); assertEquals(0, value.getInt("steering"))
        val plan = value.getJSONObject("drivePlan")
        assertEquals(0, plan.getInt("fiPermille")); assertEquals(0, plan.getInt("biPermille"))
        assertEquals(0, plan.getInt("steeringNormalized"))
    }
}
