package vea.raceremote.control

data class CarIdentity(val token: Long, val chipId: Long)

/** Optional, session-bound board identifier. This is not authentication. */
object IdentityPacket {
    const val PATH = "/control/identity"

    fun decode(bytes: ByteArray): CarIdentity? {
        if (bytes.size != 16 || bytes[0] != 82.toByte() || bytes[1] != 67.toByte() ||
            bytes[2] != 1.toByte() || bytes[3] != 130.toByte() || bytes[14] != 0.toByte() || bytes[15] != 0.toByte()) return null
        fun number(offset: Int, count: Int): Long = (0 until count).fold(0L) { value, i ->
            value or ((bytes[offset + i].toLong() and 255) shl (8 * i))
        }
        val token = number(4, 4)
        val chipId = number(8, 6)
        return if (token != 0L && chipId != 0L) CarIdentity(token, chipId) else null
    }
}
