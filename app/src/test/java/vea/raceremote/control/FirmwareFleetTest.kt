package vea.raceremote.control

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.ByteString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in host integration: tools/run_firmware_fleet.py builds the C++ adapter. */
class FirmwareFleetTest {
    @Test fun sixCarsKeepTheirOwnersAndIsolateLossOfOneLink() {
        val executable = System.getenv("RC_CONTROL_BRIDGE")
        assumeTrue("Run tools/run_firmware_fleet.py for the native firmware integration", executable != null)
        NativeControl(executable!!).use { core ->
            val http = OkHttpClient()
            val cars = (0..5).map { NativeCar(it, core) }
            val owners = cars.map { CarControlClient() }
            val intruders = cars.map { CarControlClient() }
            val replacement = CarControlClient()
            val failure = AtomicReference<Throwable>()
            val watchdog = Executors.newSingleThreadScheduledExecutor()
            watchdog.scheduleAtFixedRate({
                if (failure.get() == null) try {
                    cars.forEach { car ->
                        val state = core.call(car.index, "tick")
                        if (state.accepted) car.expiries.incrementAndGet()
                    }
                } catch (error: Throwable) { failure.compareAndSet(null, error) }
            }, 5, 5, TimeUnit.MILLISECONDS)
            fun await(label: String, condition: () -> Boolean) {
                val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (!condition() && System.nanoTime() < until) {
                    failure.get()?.let { throw AssertionError("Native watchdog failed", it) }
                    Thread.sleep(5)
                }
                assertTrue(label, condition())
            }
            fun states() = cars.map { core.call(it.index, "state") }
            try {
                owners.forEachIndexed { index, client -> client.connect("127.0.0.1", http, cars[index].server.port) }
                await("Six owners must connect") { owners.all { it.state.value.connected } }
                owners.forEachIndexed { index, client -> client.setInput((index + 1) / 10f, -(index + 1) / 10f) }
                await("Each native car must receive only its own input") {
                    states().withIndex().all { (i, s) -> s.active && s.armed && s.throttle == (i + 1) * 100 && s.steering == -(i + 1) * 100 }
                }
                val owned = states()
                println("OWNERS $owned")
                intruders.forEachIndexed { index, client -> client.connect("127.0.0.1", http, cars[index].server.port) }
                await("Six second connection attempts must be evaluated") { cars.all { it.attempts.get() == 2 } }
                cars.forEach { assertEquals("Second connection stole car ${it.index}", 1, it.admissions.get()) }
                await("Second drivers must be disconnected") { intruders.all { !it.state.value.connecting && !it.state.value.connected } }
                val afterRejection = states()
                afterRejection.forEachIndexed { index, s ->
                    assertTrue(s.active && s.armed)
                    assertEquals(owned[index].owner, s.owner)
                    assertEquals(owned[index].token, s.token)
                    assertEquals(owned[index].throttle, s.throttle)
                }
                println("REJECTED_SECOND_DRIVERS $afterRejection")

                owners[0].disconnect()
                await("Released car must stop") { !core.call(0, "state").active }
                replacement.connect("127.0.0.1", http, cars[0].server.port)
                await("New driver must explicitly connect") { replacement.state.value.connected }
                val neutral = core.call(0, "state")
                assertTrue(neutral.active && neutral.armed)
                assertEquals(0, neutral.throttle); assertEquals(0, neutral.steering)
                assertNotEquals(owned[0].token, neutral.token)
                println("REPLACEMENT_NEUTRAL $neutral")
                replacement.setInput(.75f, -.25f)
                await("New driver input must reach released car") { core.call(0, "state").throttle == 750 }

                // Drop all received commands and suppress disconnect notification,
                // so the C++ timeout, not a delivered STOP, must clear this car.
                cars[2].silent.set(true)
                await("Isolated controller must expire without a disconnect event") { !core.call(2, "state").active }
                await("Android must also stop waiting for ACK") { !owners[2].state.value.connected }
                val stopped = core.call(2, "state")
                assertEquals(0, stopped.throttle); assertEquals(0, stopped.steering)
                assertFalse(stopped.armed); assertTrue(cars[2].expiries.get() > 0)
                val survivors = listOf(0, 1, 3, 4, 5)
                val sequences = survivors.associateWith { core.call(it, "state").sequence }
                await("All other cars must continue receiving commands") {
                    survivors.all { index ->
                        val s = core.call(index, "state")
                        s.active && s.armed && s.sequence >= sequences.getValue(index) + 3 &&
                            s.throttle == if (index == 0) 750 else (index + 1) * 100
                    }
                }
                assertTrue(replacement.state.value.connected)
                listOf(1, 3, 4, 5).forEach { assertTrue(owners[it].state.value.connected) }
                println("ONE_LINK_LOST_OTHERS_ADVANCING ${states()}")
                failure.get()?.let { throw AssertionError("Native watchdog failed", it) }
            } finally {
                watchdog.shutdownNow()
                watchdog.awaitTermination(3, TimeUnit.SECONDS)
                (owners + intruders + replacement).forEach { it.close() }
                cars.forEach { it.close() }
                http.dispatcher().executorService().shutdownNow()
                http.connectionPool().evictAll()
            }
        }
    }
}

private data class NativeState(
    val accepted: Boolean, val active: Boolean, val armed: Boolean, val owner: Int,
    val token: Long, val sequence: Long, val throttle: Int, val steering: Int,
    val lastCommand: Long, val stopRevision: Long, val response: String,
)

private class NativeControl(executable: String) : Closeable {
    private val process = ProcessBuilder(executable).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    private val input = process.outputStream.bufferedWriter()
    private val replies = LinkedBlockingQueue<String>()
    private val started = System.nanoTime()
    init {
        Thread({ process.inputStream.bufferedReader().useLines { lines -> lines.forEach { replies.add(it) } } }, "firmware-replies")
            .apply { isDaemon = true; start() }
    }
    @Synchronized fun call(car: Int, operation: String, client: Int = 0, token: Long = 0, hex: String = "-"): NativeState {
        val now = ((System.nanoTime() - started) / 1_000_000) and 0xffffffffL
        input.write("$car $operation $client $token $now $hex\n"); input.flush()
        val values = requireNotNull(replies.poll(2, TimeUnit.SECONDS)) { "Native bridge did not reply to $operation" }.split(' ')
        require(values.size == 11)
        return NativeState(values[0] == "1", values[1] == "1", values[2] == "1", values[3].toInt(),
            values[4].toLong(), values[5].toLong(), values[6].toInt(), values[7].toInt(),
            values[8].toLong(), values[9].toLong(), values[10])
    }
    override fun close() {
        process.destroy()
        if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS) }
    }
}

private class NativeCar(val index: Int, private val core: NativeControl) : Closeable {
    val server = MockWebServer()
    val attempts = AtomicInteger()
    val admissions = AtomicInteger()
    val expiries = AtomicInteger()
    val silent = AtomicBoolean(false)
    private val closing = AtomicBoolean(false)
    private val ids = AtomicInteger()
    private val sockets = ConcurrentHashMap<Int, WebSocket>()
    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path != "/control") return MockResponse().setResponseCode(404)
                val id = ids.incrementAndGet()
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        sockets[id] = webSocket
                        val state = core.call(index, "connect", id, 1000L + index * 100 + id)
                        if (state.accepted) admissions.incrementAndGet()
                        attempts.incrementAndGet()
                        reply(webSocket, state)
                    }
                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        if (!closing.get() && !silent.get()) reply(webSocket, core.call(index, "accept", id, hex = bytes.hex()))
                    }
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { release(id); webSocket.close(code, reason) }
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { release(id) }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { release(id) }
                    private fun release(id: Int) {
                        sockets.remove(id)
                        if (!closing.get() && !silent.get()) core.call(index, "disconnect", id)
                    }
                })
            }
        }
        server.start()
    }
    private fun reply(socket: WebSocket, state: NativeState) {
        if (state.response == "-") socket.close(1008, "Control unavailable")
        else socket.send(ByteString.decodeHex(state.response))
    }
    override fun close() {
        closing.set(true)
        sockets.values.forEach { it.cancel() }
        server.shutdown()
    }
}
