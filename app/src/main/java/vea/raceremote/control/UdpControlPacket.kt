package vea.raceremote.control

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reconstructed from the installed September APK; see recovery/apk-2026-09-26. */
data class UdpControlPacket(
    val kind: Int,
    val attempt: Long,
    val token: Long,
    val sequence: Long,
    val throttle: Int,
    val steering: Int,
    val chipId: Long,
    val basis: Long = 0,
) {
    fun encode(): ByteArray {
        require(kind in 0..255 && attempt in 1..0xffffffffL)
        require(token in 0..0xffffffffL && sequence in 0..0xffffffffL && basis in 0..0xffffffffL)
        require(throttle in -1000..1000 && steering in -1000..1000 && chipId in 1..0xffffffffffffL)
        return ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN)
            .put(82).put(85).put(1).put(kind.toByte())
            .putInt(attempt.toInt()).putInt(token.toInt()).putInt(sequence.toInt())
            .putShort(throttle.toShort()).putShort(steering.toShort())
            .putInt(basis.toInt()).putLong(chipId).array()
    }

    companion object {
        const val PORT = 3334
        const val SIZE = 32
        const val OPEN = 0
        const val ARM = 1
        const val DRIVE = 2
        const val STOP = 3
        const val HELLO = 128
        const val ACK = 129

        fun decode(bytes: ByteArray): UdpControlPacket? {
            if (bytes.size != SIZE || bytes[0] != 82.toByte() || bytes[1] != 85.toByte() || bytes[2] != 1.toByte()) return null
            val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            b.position(3)
            val kind = b.get().toInt() and 255
            val attempt = b.int.toLong() and 0xffffffffL
            val token = b.int.toLong() and 0xffffffffL
            val sequence = b.int.toLong() and 0xffffffffL
            val throttle = b.short.toInt()
            val steering = b.short.toInt()
            val basis = b.int.toLong() and 0xffffffffL
            val chipId = b.long
            if (attempt == 0L || chipId !in 1..0xffffffffffffL || throttle !in -1000..1000 || steering !in -1000..1000) return null
            return UdpControlPacket(kind, attempt, token, sequence, throttle, steering, chipId, basis)
        }
    }
}
