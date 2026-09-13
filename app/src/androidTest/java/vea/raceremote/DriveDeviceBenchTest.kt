package vea.raceremote

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import vea.raceremote.control.CarControlClient
import vea.raceremote.control.ControlPacket
import vea.raceremote.control.ControlSocketFactory

/** Explicit opt-in only; refuses any device that reports enabled actuators. */
class DriveDeviceBenchTest {
    @Test fun nativeClientDrivePlan() = runBench("native")
    @Test fun silentSocketTimesOut() = runBench("watchdog")

    private fun runBench(scenario: String) {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Requires explicit disabled-output bench opt-in", args.getString("driveBench") == "disabled-outputs")
        val host = requireNotNull(args.getString("driveBenchHost"))
        val carId = requireNotNull(args.getString("driveBenchCarId"))
        val octets = host.split('.').map { it.toIntOrNull() }
        require(octets.size == 4 && octets.all { it != null && it in 0..255 } &&
            (octets[0] == 10 || (octets[0] == 192 && octets[1] == 168) ||
                (octets[0] == 172 && octets[1]!! in 16..31))) { "Private IPv4 only" }
        require(carId.startsWith("RaceRemote-") && carId.length <= 40)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val started = SystemClock.elapsedRealtime()
        val events = Collections.synchronizedList(mutableListOf<JSONObject>())
        fun record(kind: String, data: Any) {
            events.add(JSONObject().put("elapsedMs", SystemClock.elapsedRealtime() - started).put("kind", kind).put("data", data))
        }
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val wifi = manager.allNetworks.firstOrNull {
            manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        val builder = OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS).callTimeout(2, TimeUnit.SECONDS)
        if (wifi != null) builder.socketFactory(wifi.socketFactory)
        val http = builder.build()
        val wifiLock = context.applicationContext.getSystemService(WifiManager::class.java)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "RaceRemote:drive-bench")
            .apply { setReferenceCounted(false) }
        val client = CarControlClient(diagnostics = { record("client", it) })
        var raw: WebSocket? = null
        var rawToken = 0L
        var complete = false

        fun status(phase: String): JSONObject {
            val before = SystemClock.elapsedRealtime()
            val json = http.newCall(Request.Builder().url("http://$host/status").build()).execute().use { response ->
                assertEquals("HTTP status", 200, response.code())
                JSONObject(requireNotNull(response.body()).string())
            }
            record("status", JSONObject().put("phase", phase).put("requestMs", SystemClock.elapsedRealtime() - before).put("value", json))
            assertEquals("Unexpected device", carId, json.getString("carId"))
            assertEquals(1, json.getInt("protocol"))
            assertFalse("Refusing an enabled physical drive", json.getBoolean("outputsEnabled"))
            val p = json.getJSONObject("drivePlan")
            val fi = p.getInt("fiPermille")
            val bi = p.getInt("biPermille")
            assertTrue("Duty cap violated", fi in 0..250 && bi in 0..250)
            assertTrue("Both directions active", fi == 0 || bi == 0)
            return json
        }
        fun awaitStatus(phase: String, predicate: (JSONObject) -> Boolean): JSONObject {
            val deadline = SystemClock.elapsedRealtime() + 3000
            do {
                val value = status(phase)
                if (predicate(value)) return value
                SystemClock.sleep(40)
            } while (SystemClock.elapsedRealtime() < deadline)
            throw AssertionError("No matching device state: $phase; client=${client.state.value}")
        }
        fun zero(value: JSONObject): Boolean = value.getJSONObject("drivePlan").let {
            it.getInt("fiPermille") == 0 && it.getInt("biPermille") == 0
        }
        try {
            wifiLock.acquire()
            val initial = status("preflight")
            assertFalse("Device already armed", initial.getBoolean("armed"))
            assertTrue("Nonzero preflight plan", zero(initial))
            if (scenario == "native") {
                record("action", "native client connect")
                client.connect(host, http)
                val armDeadline = SystemClock.elapsedRealtime() + 4500
                while (!client.state.value.connected && SystemClock.elapsedRealtime() < armDeadline) SystemClock.sleep(10)
                assertTrue("Native client did not ARM: ${client.state.value}", client.state.value.connected)
                client.setInput(.6f, .25f)
                record("action", "native forward throttle=600 steering=250")
                awaitStatus("forward") {
                    it.getBoolean("armed") && it.getInt("throttle") == 600 &&
                        it.getJSONObject("drivePlan").getInt("fiPermille") == 150 &&
                        it.getJSONObject("drivePlan").getInt("steeringNormalized") == 250
                }
                client.setInput(-.6f, -.25f)
                record("action", "native reverse throttle=-600 steering=-250")
                awaitStatus("reverse_pause") { it.getJSONObject("drivePlan").getString("mode") == "reverse_wait" && zero(it) }
                awaitStatus("reverse") {
                    it.getBoolean("armed") && it.getInt("throttle") == -600 &&
                        it.getJSONObject("drivePlan").getInt("biPermille") == 150
                }
                client.setInput(0f, 0f)
                record("action", "native neutral")
                awaitStatus("neutral") { it.getBoolean("armed") && it.getInt("throttle") == 0 && zero(it) }
                client.setInput(.4f, 0f)
                awaitStatus("before_stop") { it.getJSONObject("drivePlan").getInt("fiPermille") > 0 }
                client.disconnect()
                record("action", "native STOP/disconnect")
                awaitStatus("stopped") { !it.getBoolean("armed") && zero(it) }
            }

            if (scenario == "watchdog") {
                // Keep a raw WebSocket open after one DRIVE, with no automatic
                // client STOP or heartbeat: exercise the firmware timeout itself.
                val packets = LinkedBlockingQueue<ControlPacket>()
                raw = http.newBuilder().socketFactory(ControlSocketFactory(http.socketFactory())).build()
                    .newWebSocket(Request.Builder().url("ws://$host:1337/control").build(), object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            ControlPacket.decode(bytes.toByteArray())?.let { packets.offer(it) }
                        }
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            record("raw_failure", t.toString())
                        }
                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            record("raw_closing", code)
                            webSocket.close(code, reason)
                        }
                    })
                fun packet(kind: Int, sequence: Long): ControlPacket {
                    val p = requireNotNull(packets.poll(3, TimeUnit.SECONDS)) { "Missing raw packet $kind/$sequence" }
                    assertEquals(kind, p.kind)
                    assertEquals(sequence, p.sequence)
                    return p
                }
                val hello = packet(ControlPacket.HELLO, 0)
                rawToken = hello.token
                assertTrue(requireNotNull(raw).send(ByteString.of(*ControlPacket(ControlPacket.ARM, rawToken, 1).encode())))
                assertEquals(rawToken, packet(ControlPacket.ACK, 1).token)
                assertTrue(requireNotNull(raw).send(ByteString.of(*ControlPacket(ControlPacket.DRIVE, rawToken, 2, 600, 0).encode())))
                assertEquals(rawToken, packet(ControlPacket.ACK, 2).token)
                record("action", "raw DRIVE acknowledged; stop sending while socket stays open")
                awaitStatus("before_watchdog") { it.getBoolean("armed") && it.getJSONObject("drivePlan").getInt("fiPermille") > 0 }
                awaitStatus("watchdog_zero") { !it.getBoolean("armed") && zero(it) }
            }
            complete = true
        } catch (t: Throwable) {
            record("failure", t.toString())
            throw t
        } finally {
            client.close()
            if (rawToken != 0L) raw?.send(ByteString.of(*ControlPacket(ControlPacket.STOP, rawToken, 2).encode()))
            raw?.cancel()
            try {
                awaitStatus("cleanup") { !it.getBoolean("armed") && zero(it) }
            } catch (t: Throwable) {
                record("cleanup_failure", t.toString())
                complete = false
            }
            if (wifiLock.isHeld) wifiLock.release()
            val report = JSONObject().put("complete", complete).put("scenario", scenario).put("host", host).put("carId", carId)
                .put("scope", "Native Android control client and separate raw timeout probe; shadow firmware outputs only")
                .put("events", synchronized(events) { JSONArray(events.toList()) })
            File(context.filesDir, "drive-device-$scenario.json").writeText(report.toString(2))
            http.dispatcher().executorService().shutdownNow()
            http.connectionPool().evictAll()
        }
        assertTrue("Cleanup must be verified", complete)
    }
}
