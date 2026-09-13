package vea.raceremote.control

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import org.junit.Assert.*
import org.junit.Test

class CarDiscoveryTest {
    private val golden = "525244010200390578563412c8f09ea66da0000001010000"
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun sharedFirmwareVectorAndInvalidPackets() {
        assertEquals("525244010100000078563412", DiscoveryPacket.query(0x12345678).joinToString("") { "%02x".format(it) })
        val car = DiscoveryPacket.reply(golden, 0x12345678, "192.168.1.4")!!
        assertEquals("RaceRemote-a06da6", car.name)
        assertEquals(1337, car.port); assertTrue(car.busy)
        assertNull(DiscoveryPacket.reply(golden, 1, car.host))
        assertNull(DiscoveryPacket.reply(golden.copyOf(25), 0x12345678, car.host))
        for (offset in listOf(0, 1, 2, 3, 4, 5, 18, 19, 20, 21, 22, 23)) {
            assertNull(DiscoveryPacket.reply(golden.copyOf().also { it[offset] = 99 }, 0x12345678, car.host))
        }
        assertNull(DiscoveryPacket.reply(golden.copyOf().also { it[6] = 0; it[7] = 0 }, 0x12345678, car.host))
    }

    @Test fun udpScanFindsTwoCarsAndRejectsOldNonceWithoutOpeningControl() {
        DatagramSocket(DiscoveryPacket.PORT, InetAddress.getLoopbackAddress()).use { server ->
            server.soTimeout = 4000
            val worker = Executors.newSingleThreadExecutor()
            val responder = worker.submit {
                val query = DatagramPacket(ByteArray(12), 12)
                server.receive(query)
                val response = golden.copyOf()
                query.data.copyInto(response, 8, 8, 12)
                val stale = response.copyOf().also { it[8] = (it[8].toInt() xor 128).toByte() }
                for (bytes in listOf(stale, response, response.copyOf().also { it[15] = -89; it[21] = 0 }))
                    server.send(DatagramPacket(bytes, bytes.size, query.address, query.port))
            }
            val discovery = CarDiscovery()
            try {
                discovery.search(listOf(InetAddress.getLoopbackAddress()))
                await { !discovery.state.value.searching }
                responder.get()
                assertEquals(2, discovery.state.value.cars.size)
                assertEquals(setOf("RaceRemote-a06da6", "RaceRemote-a06da7"), discovery.state.value.cars.map { it.name }.toSet())
                assertTrue(discovery.state.value.cars.any { it.busy })
                assertTrue(discovery.state.value.cars.any { !it.busy })
            } finally { discovery.close(); worker.shutdownNow() }
        }
    }

    @Test fun cancellationCannotPublishResultsFromPreviousSearch() {
        val discovery = CarDiscovery()
        try {
            discovery.search(listOf(InetAddress.getLoopbackAddress()))
            discovery.cancel()
            Thread.sleep(150)
            assertEquals(DiscoveryState(), discovery.state.value)
        } finally { discovery.close() }
    }

    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < end) Thread.sleep(10)
        assertTrue(condition())
    }
}
