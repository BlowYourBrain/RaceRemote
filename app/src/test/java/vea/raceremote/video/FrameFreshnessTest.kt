package vea.raceremote.video

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class FrameFreshnessTest {
    private fun frame(seq: Long?, time: Long?) = MjpegFrame(byteArrayOf(1), seq, time)

    @Test fun repeatedCaptureIsRejectedEvenWithNewSendSequence() {
        val gate = FrameFreshness()
        assertTrue(gate.accept(frame(1, 123000)))
        assertFalse(gate.accept(frame(2, 123000)))
        assertFalse(gate.accept(frame(3, 122999)))
        assertTrue(gate.accept(frame(4, 123001)))
    }

    @Test fun unsignedSequenceWrapAndLossAreAllowedButReplayIsNot() {
        val gate = FrameFreshness()
        assertTrue(gate.accept(frame(0xffff_fffeL, 1)))
        assertTrue(gate.accept(frame(0, 2)))
        assertFalse(gate.accept(frame(0xffff_ffffL, 3)))
        assertFalse(gate.accept(frame(0, 4)))
        assertFalse(gate.accept(frame(0x8000_0000L, 5)))
        assertTrue(gate.accept(frame(100, 6)))
    }

    @Test fun reconnectResetsClockAndSequenceHistory() {
        val old = FrameFreshness()
        assertTrue(old.accept(frame(200, 9000)))
        assertFalse(old.accept(frame(1, 0)))
        assertTrue(FrameFreshness().accept(frame(1, 0)))
    }

    @Test fun genericAndTimestampOnlyCamerasRemainSupported() {
        val generic = FrameFreshness()
        repeat(3) { assertTrue(generic.accept(frame(null, null))) }
        val timestampOnly = FrameFreshness()
        assertTrue(timestampOnly.accept(frame(null, 0)))
        assertFalse(timestampOnly.accept(frame(null, 0)))
        assertTrue(timestampOnly.accept(frame(null, 1)))
        val sequenceOnly = FrameFreshness()
        assertTrue(sequenceOnly.accept(frame(1, null)))
        assertFalse(sequenceOnly.accept(frame(1, null)))
        assertTrue(sequenceOnly.accept(frame(2, null)))
    }

    @Test fun metadataCannotDisappearOrChangeModeMidstream() {
        val modes = listOf(frame(null, null), frame(1, null), frame(null, 1), frame(1, 1))
        for (a in modes) for (b in modes) {
            if ((a.sequence != null) == (b.sequence != null) && (a.capturedUs != null) == (b.capturedUs != null)) continue
            val gate = FrameFreshness()
            assertTrue(gate.accept(a))
            try { gate.accept(b); fail("Metadata change must fail") } catch (_: IOException) { }
        }
    }

    @Test fun parserPreservesCapturePrecisionAndRejectsInvalidMetadata() {
        fun read(metadata: String): MjpegFrame = MjpegReader(("--f\r\nContent-Type: image/jpeg\r\n" +
            "Content-Length: 4\r\n$metadata\r\n\r\n").toByteArray().plus(byteArrayOf(-1, -40, -1, -39))
            .inputStream(), "multipart/x-mixed-replace;boundary=f").nextPart()!!
        val valid = read("X-Sequence: 4294967295\r\nX-Timestamp: 4294967295.999999")
        assertEquals(0xffff_ffffL, valid.sequence)
        assertEquals(4_294_967_295_999_999L, valid.capturedUs)
        assertEquals(1L, read("X-Timestamp: 0.000001").capturedUs)
        for (metadata in listOf("X-Sequence: -1", "X-Sequence: +1", "X-Sequence: 4294967296",
            "X-Sequence: 1.0", "X-Timestamp: 1.1", "X-Timestamp: -1.000000", "X-Timestamp: 1.1000000",
            "X-Timestamp: 4294967296.000000", "X-Timestamp: NaN", "X-Sequence: 1\r\nx-sequence: 1")) {
            try { read(metadata); fail(metadata) } catch (_: IOException) { }
        }
    }
}
