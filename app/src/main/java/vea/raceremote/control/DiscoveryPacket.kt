package vea.raceremote.control

data class DiscoveredCar(val host: String, val port: Int, val chipId: Long, val busy: Boolean) {
    val name: String get() = "RaceRemote-${(chipId ushr 24).toString(16)}"
}

object DiscoveryPacket {
    const val PORT = 3333
    fun query(nonce: Long): ByteArray {
        require(nonce in 1..0xffffffffL)
        return byteArrayOf(82, 82, 68, 1, 1, 0, 0, 0, 0, 0, 0, 0).also { bytes ->
            repeat(4) { bytes[8 + it] = (nonce ushr (8 * it)).toByte() }
        }
    }

    fun reply(bytes: ByteArray, expectedNonce: Long, host: String): DiscoveredCar? {
        if (bytes.size != 24 || bytes[0] != 82.toByte() || bytes[1] != 82.toByte() ||
            bytes[2] != 68.toByte() || bytes[3] != 1.toByte() || bytes[4] != 2.toByte() ||
            bytes[5] != 0.toByte() || bytes[20] != 1.toByte() || bytes[21].toInt() !in 0..1 ||
            bytes[22] != 0.toByte() || bytes[23] != 0.toByte()) return null
        fun number(offset: Int, count: Int): Long = (0 until count).fold(0L) { value, i ->
            value or ((bytes[offset + i].toLong() and 255) shl (8 * i))
        }
        if (expectedNonce == 0L || number(8, 4) != expectedNonce) return null
        val port = number(6, 2).toInt()
        val chipId = number(12, 8)
        if (port == 0 || chipId !in 1..0xffffffffffffL) return null
        return DiscoveredCar(host, port, chipId, bytes[21] == 1.toByte())
    }
}
