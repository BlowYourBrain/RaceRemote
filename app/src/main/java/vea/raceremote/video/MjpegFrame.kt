package vea.raceremote.video

import java.io.IOException

data class MjpegFrame(val bytes: ByteArray, val sequence: Long?, val capturedUs: Long?)

/** Per HTTP connection. Camera timestamps are ordered, never subtracted from phone time. */
internal class FrameFreshness {
    private var metadataMode: Pair<Boolean, Boolean>? = null
    private var previous: MjpegFrame? = null

    fun accept(frame: MjpegFrame): Boolean {
        val mode = (frame.sequence != null) to (frame.capturedUs != null)
        if (metadataMode != null && metadataMode != mode)
            throw IOException("Video metadata changed within stream")
        metadataMode = mode
        val old = previous
        if (old != null) {
            if (frame.sequence != null && old.sequence != null) {
                val delta = (frame.sequence - old.sequence) and 0xffff_ffffL
                // Unsigned serial arithmetic: wraps are allowed, repeats/backwards are not.
                if (delta == 0L || delta >= 0x8000_0000L) return false
            }
            if (frame.capturedUs != null && old.capturedUs != null && frame.capturedUs <= old.capturedUs)
                return false
        }
        // Keep only metadata, not an extra retained JPEG buffer.
        previous = MjpegFrame(ByteArray(0), frame.sequence, frame.capturedUs)
        return true
    }
}
