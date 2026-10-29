package vea.raceremote.control

import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real Kotlin UDP socket/client against the firmware C++ core via a host-only stdin bridge. */
class FirmwareUdpInteropTest {
    @Test(timeout = 20000) fun nativeCoreKeepsOwnerStopsOnLossAndRequiresFreshNeutralSession() {
        val executable = System.getenv("RC_UDP_BRIDGE")
        assumeTrue("Run tools/run_firmware_udp.py for native UDP integration", executable != null)
        NativeCar(executable!!).use { car ->
            UdpControlClient("127.0.0.1", CHIP, {}).use { owner ->
                await(car) { owner.snapshot().phase == UdpControlSession.Phase.ACTIVE }
                owner.input(200, 100)
                await(car) { owner.snapshot().acknowledgedThrottle == 200 && car.state.get()?.throttle == 200 }
                val before = owner.snapshot().acknowledgements
                UdpControlClient("127.0.0.1", CHIP, {}).use { intruder ->
                    Thread.sleep(300)
                    assertEquals(UdpControlSession.Phase.OPENING, intruder.snapshot().phase)
                    assertEquals(UdpControlSession.Phase.ACTIVE, owner.snapshot().phase)
                    assertTrue(owner.snapshot().acknowledgements > before + 3)
                    assertEquals(200, car.state.get()!!.throttle)
                }
                owner.input(-200, -100)
                await(car) { owner.snapshot().acknowledgedThrottle == -200 && car.state.get()?.bi?.let { it > 0 } == true }
            }
            await(car) { car.state.get()?.active == false }
            assertEquals("Explicit client close must deliver an accepted STOP", 1, car.acceptedStops.get())
            assertNeutral(car.state.get()!!)

            UdpControlClient("127.0.0.1", CHIP, {}).use { owner ->
                await(car) { owner.snapshot().phase == UdpControlSession.Phase.ACTIVE }
                owner.input(300, 200)
                await(car) { owner.snapshot().acknowledgedThrottle == 300 }
                car.dropRequests.set(true) // STOP cannot reach firmware either: native tick must expire it.
                await(car) { owner.snapshot().phase == UdpControlSession.Phase.CLOSED && car.state.get()?.active == false }
                assertEquals("ACK freshness timeout", owner.snapshot().reason)
                assertNeutral(car.state.get()!!)
                assertEquals("Loss must stop through expiry with no delivered STOP", 1, car.acceptedStops.get())
            }
            car.dropRequests.set(false)
            UdpControlClient("127.0.0.1", CHIP, {}).use { replacement ->
                await(car) { replacement.snapshot().phase == UdpControlSession.Phase.ACTIVE }
                assertEquals(0, replacement.snapshot().throttle)
                assertEquals(0, replacement.snapshot().acknowledgedThrottle)
                assertEquals(0, car.state.get()!!.throttle)
                assertEquals(0, car.state.get()!!.steering)
            }
            await(car) { car.state.get()?.active == false }
        }
    }

    private fun assertNeutral(state: NativeState) {
        assertFalse(state.armed)
        assertEquals(0, state.throttle); assertEquals(0, state.steering)
        assertEquals(0, state.fi); assertEquals(0, state.bi)
    }

    private fun await(car: NativeCar, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 4_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            car.failure.get()?.let { throw AssertionError("Native bridge failed", it) }
            Thread.sleep(5)
        }
        car.failure.get()?.let { throw AssertionError("Native bridge failed", it) }
        assertTrue("Native/client condition was not reached: ${car.state.get()}", condition())
    }

    private data class NativeState(val active: Boolean, val armed: Boolean, val throttle: Int,
        val steering: Int, val fi: Int, val bi: Int)

    private class NativeCar(executable: String) : Closeable {
        val dropRequests = AtomicBoolean()
        val acceptedStops = AtomicInteger()
        val failure = AtomicReference<Throwable>()
        val state = AtomicReference<NativeState>()
        private val running = AtomicBoolean(true)
        private val udp = DatagramSocket(3334, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 5 }
        private val process = try { ProcessBuilder(executable).redirectErrorStream(true).start() }
            catch (error: Throwable) { udp.close(); throw error }
        private val input = process.inputStream.bufferedReader()
        private val output = process.outputStream.bufferedWriter()
        private fun now() = (System.nanoTime() / 1_000_000) and 0xffffffffL

        private fun call(command: String): List<String> {
            output.write(command); output.newLine(); output.flush()
            val fields = requireNotNull(input.readLine()) { "Native bridge EOF" }.split(' ')
            check(fields.size == 12) { "Invalid native reply: $fields" }
            if (fields[0] == "3") acceptedStops.incrementAndGet()
            state.set(NativeState(fields[1] == "1", fields[2] == "1", fields[3].toInt(),
                fields[4].toInt(), fields[8].toInt(), fields[9].toInt()))
            return fields
        }

        private val worker = Thread({
            try {
                while (running.get()) {
                    val datagram = DatagramPacket(ByteArray(256), 256)
                    try {
                        udp.receive(datagram)
                        if (!dropRequests.get()) {
                            val address = datagram.address.address.withIndex().fold(0L) { value, b ->
                                value or ((b.value.toLong() and 255) shl (8 * b.index))
                            }
                            val hex = datagram.data.take(datagram.length).joinToString("") { "%02x".format(it) }
                            val result = call("R ${now()} $address ${datagram.port} $hex")
                            if (result[11] != "-") {
                                val bytes = result[11].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                val sent = runCatching { udp.send(DatagramPacket(bytes, bytes.size, datagram.address, datagram.port)) }.isSuccess
                                call("S ${now()} ${if (sent) 1 else 0} ${result[11]}")
                            }
                        }
                    } catch (_: SocketTimeoutException) { }
                    call("T ${now()}")
                }
            } catch (error: Throwable) {
                if (running.get()) failure.set(error)
            }
        }, "native-udp-car").apply { isDaemon = true; start() }

        override fun close() {
            running.set(false); udp.close(); process.destroy(); worker.join(1000)
            if (process.isAlive) process.destroyForcibly()
            input.close(); output.close()
        }
    }

    companion object { private const val CHIP = 0x5008d93f1fb8L }
}
