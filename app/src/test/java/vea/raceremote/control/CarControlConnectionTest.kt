package vea.raceremote.control

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class CarControlConnectionTest {
    @Test fun mismatchedOrUnsupportedEndpointCannotOpenControlSocket() {
        val chip = 0x123456L
        for (endpoint in listOf(ControlEndpoint("udp-v1", 3334, chip + 1), ControlEndpoint("udp-v1", 3335, chip),
            ControlEndpoint("websocket-v1", 3334, chip), ControlEndpoint("unknown", 3334, chip),
            ControlEndpoint("udp-v1", 3334, 0))) {
            val binds = AtomicInteger()
            val client = CarControlConnection(resolve = { _, _, _ -> endpoint })
            try {
                client.connect("127.0.0.1", OkHttpClient(), { binds.incrementAndGet() }, 3334, chip, expectedTransport = "udp-v1")
                await { !client.state.value.connecting }
                assertFalse(client.state.value.connected)
                assertEquals(0, binds.get())
            } finally { client.close() }
        }
    }

    @Test fun stopDuringBlockingProbeCannotStartALateSession() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val exited = CountDownLatch(1)
        val binds = AtomicInteger()
        val client = CarControlConnection(resolve = { _, _, _ ->
            entered.countDown()
            check(release.await(2, TimeUnit.SECONDS))
            exited.countDown()
            ControlEndpoint("udp-v1", 3334, 0x123456)
        })
        try {
            client.connect("127.0.0.1", OkHttpClient(), { binds.incrementAndGet() })
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            client.disconnect("User stop")
            release.countDown()
            assertTrue(exited.await(2, TimeUnit.SECONDS))
            Thread.sleep(100)
            assertEquals(CarConnectionState("User stop"), client.state.value)
            assertEquals(0, binds.get())
        } finally { release.countDown(); client.close() }
    }

    @Test fun publicAddressAndBadStatusPortAreRejectedBeforeProbe() {
        val calls = AtomicInteger()
        val client = CarControlConnection(resolve = { _, _, _ -> calls.incrementAndGet(); error("Must not probe") })
        try {
            client.connect("8.8.8.8", OkHttpClient())
            client.connect("127.0.0.1", OkHttpClient(), statusPort = 0)
            client.connect("localhost", OkHttpClient())
            assertEquals(0, calls.get())
            assertFalse(client.state.value.connecting)
        } finally { client.close() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(condition())
    }
}
