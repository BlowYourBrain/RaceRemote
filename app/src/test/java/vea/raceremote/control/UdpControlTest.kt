package vea.raceremote.control

import org.junit.Assert.*
import org.junit.Test

class UdpControlTest {
    private val chip = 0x5008d93f1fb8L
    private fun packet(kind: Int, seq: Long = 0, token: Long = 9, t: Int = 0, s: Int = 0) =
        UdpControlPacket(kind, 7, token, seq, t, s, chip)

    private fun active(): UdpControlSession = UdpControlSession(chip, 7, 0).also {
        assertEquals(UdpControlPacket.OPEN, it.next(0)!!.kind)
        assertTrue(it.receive(packet(UdpControlPacket.HELLO), 10))
        assertEquals(1L, it.next(10)!!.sequence)
        assertTrue(it.receive(packet(UdpControlPacket.ACK, 1), 20))
        assertEquals(UdpControlSession.Phase.ACTIVE, it.snapshot().phase)
    }

    @Test fun wireRoundTripAndMalformedInputs() {
        val p = packet(UdpControlPacket.DRIVE, 0xffffffffL, 0xffffffffL, -1000, 1000).copy(basis = 0xffffffffL)
        val bytes = p.encode()
        assertEquals(32, bytes.size)
        assertArrayEquals(byteArrayOf(82, 85, 1, 2, 7, 0, 0, 0), bytes.copyOf(8))
        assertEquals(p, UdpControlPacket.decode(bytes))
        assertNull(UdpControlPacket.decode(bytes.copyOf(33)))
        for (index in listOf(0, 1, 2)) assertNull(UdpControlPacket.decode(bytes.clone().also { it[index] = 0 }))
        assertNull(UdpControlPacket.decode(bytes.clone().also { for (i in 4..7) it[i] = 0 }))
        assertNull(UdpControlPacket.decode(bytes.clone().also { it[31] = 1 }))
        assertNull(UdpControlPacket.decode(bytes.clone().also { it[16] = 0; it[17] = 127 }))
    }

    @Test fun ackMustMatchPendingSessionAndAxes() {
        val session = active()
        session.input(200, -100)
        val drive = session.next(30)!!
        assertEquals(1L, drive.basis)
        val ack = drive.copy(kind = UdpControlPacket.ACK, basis = 0)
        for (bad in listOf(ack.copy(chipId = chip + 1), ack.copy(attempt = 8), ack.copy(token = 10),
            ack.copy(throttle = 201), ack.copy(basis = 1), ack.copy(sequence = 3))) assertFalse(session.receive(bad, 40))
        assertTrue(session.receive(ack, 40))
        assertFalse(session.receive(ack, 41))
        assertEquals(200, session.snapshot().acknowledgedThrottle)
        assertEquals(-100, session.snapshot().acknowledgedSteering)
    }

    @Test fun freshnessUsesSendTimeAndExpiresBeforeReceiving() {
        val session = active()
        val drive = session.next(30)!!
        assertTrue(session.receive(drive.copy(kind = UdpControlPacket.ACK, basis = 0), 209))
        assertNotNull(session.next(229))
        assertEquals(UdpControlPacket.STOP, session.next(230)!!.kind)
        assertEquals(UdpControlSession.Phase.CLOSED, session.snapshot().phase)
        assertNull(session.next(231))
        val late = active()
        val pending = late.next(30)!!
        assertFalse(late.receive(pending.copy(kind = UdpControlPacket.ACK, basis = 0), 210))
        assertEquals(UdpControlSession.Phase.CLOSED, late.snapshot().phase)
    }

    @Test fun handshakeDeadlinesAndArmRetriesNeverRenewDeadline() {
        val opening = UdpControlSession(chip, 7, 0)
        assertFalse(opening.receive(packet(UdpControlPacket.HELLO), 4000))
        val arming = UdpControlSession(chip, 7, 0)
        assertTrue(arming.receive(packet(UdpControlPacket.HELLO), 0))
        arming.next(0)
        assertEquals(UdpControlPacket.ARM, arming.next(50)!!.kind)
        assertFalse(arming.receive(packet(UdpControlPacket.ACK, 1), 200))
        assertEquals(UdpControlPacket.STOP, arming.next(200)!!.kind)
    }

    @Test fun stopClearsInputsAndEmitsOnce() {
        val session = active()
        session.input(1000, -1000)
        session.stop("User stop")
        val stop = session.next(25)!!
        assertEquals(UdpControlPacket.STOP, stop.kind)
        assertEquals(0, stop.throttle)
        assertEquals(0, stop.steering)
        assertEquals("User stop", session.snapshot().reason)
        assertEquals(0, session.snapshot().acknowledgedThrottle)
        assertNull(session.next(30))
    }
}
