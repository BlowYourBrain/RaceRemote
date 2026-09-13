package vea.raceremote.control

/** Protocol v1. No Android dependency; intended to move to KMP commonMain. */
data class ControlPacket(
    val kind: Int,
    val token: Long,
    val sequence: Long,
    val throttle: Int = 0,
    val steering: Int = 0,
) {
    fun encode(): ByteArray {
        require(kind in setOf(ARM, DRIVE, STOP, HELLO, ACK))
        require(token in 1..0xffffffffL && sequence in 0..0xffffffffL)
        require(throttle in -1000..1000 && steering in -1000..1000)
        return ByteArray(16).also { b ->
            b[0] = 82; b[1] = 67; b[2] = 1; b[3] = kind.toByte()
            for (i in 0..3) {
                b[4 + i] = (token shr (8 * i)).toByte()
                b[8 + i] = (sequence shr (8 * i)).toByte()
            }
            b[12] = throttle.toByte(); b[13] = (throttle shr 8).toByte()
            b[14] = steering.toByte(); b[15] = (steering shr 8).toByte()
        }
    }

    companion object {
        const val ARM = 1
        const val DRIVE = 2
        const val STOP = 3
        const val HELLO = 128
        const val ACK = 129
        fun decode(b: ByteArray): ControlPacket? {
            if (b.size != 16 || b[0] != 82.toByte() || b[1] != 67.toByte() || b[2] != 1.toByte()) return null
            fun u32(offset: Int) = (0..3).fold(0L) { n, i -> n or ((b[offset + i].toLong() and 255) shl (8 * i)) }
            fun i16(offset: Int) = ((b[offset].toInt() and 255) or (b[offset + 1].toInt() shl 8)).toShort().toInt()
            val p = ControlPacket(b[3].toInt() and 255, u32(4), u32(8), i16(12), i16(14))
            return p.takeIf { it.kind in setOf(ARM, DRIVE, STOP, HELLO, ACK) && it.token != 0L &&
                it.throttle in -1000..1000 && it.steering in -1000..1000 }
        }
    }
}
