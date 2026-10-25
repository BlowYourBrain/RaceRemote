package vea.raceremote.control

/** Pure state machine reconstructed from the APK, with a monotonic millisecond clock. */
class UdpControlSession(val chipId: Long, val attempt: Long, private val openedAt: Long) {
    enum class Phase { OPENING, ARMING, ACTIVE, CLOSED }
    data class Snapshot(
        val phase: Phase, val reason: String, val acknowledgements: Int, val lastAck: Long,
        val throttle: Int, val steering: Int, val maxRttMs: Long,
        val acknowledgedThrottle: Int, val acknowledgedSteering: Int,
    )
    private data class Sent(val packet: UdpControlPacket, val at: Long)
    private val pending = arrayOfNulls<Sent>(64)
    private var phase = Phase.OPENING
    private var reason = "Connecting"
    private var token = 0L
    private var sequence = 0L
    private var helloAt = 0L
    private var lastSent: Long? = null
    private var armStarted: Long? = null
    private var confirmedAt = 0L
    private var lastAck = 0L
    private var acknowledgements = 0
    private var maxRttMs = 0L
    private var throttle = 0
    private var steering = 0
    private var acknowledgedThrottle = 0
    private var acknowledgedSteering = 0
    private var stopping: UdpControlPacket? = null

    init { require(chipId in 1..0xffffffffffffL && attempt in 1..0xffffffffL) }

    fun snapshot() = Snapshot(phase, reason, acknowledgements, lastAck, throttle, steering,
        maxRttMs, acknowledgedThrottle, acknowledgedSteering)

    fun input(throttle: Int, steering: Int) {
        require(throttle in -1000..1000 && steering in -1000..1000)
        if (phase == Phase.ACTIVE) { this.throttle = throttle; this.steering = steering }
    }

    fun stop(reason: String = "Stopped") {
        if (phase == Phase.CLOSED) return
        if (token != 0L) stopping = packet(UdpControlPacket.STOP, sequence)
        phase = Phase.CLOSED
        this.reason = reason
        throttle = 0; steering = 0; acknowledgedThrottle = 0; acknowledgedSteering = 0
        pending.fill(null)
    }

    private fun expire(now: Long) {
        when (phase) {
            Phase.OPENING -> if (now - openedAt >= 4000) stop("HELLO timeout")
            Phase.ARMING -> if (now - (armStarted ?: helloAt) >= 200) stop("ARM timeout")
            Phase.ACTIVE -> if (now - confirmedAt >= 200) stop("ACK freshness timeout")
            Phase.CLOSED -> Unit
        }
    }

    fun next(now: Long): UdpControlPacket? {
        expire(now)
        if (phase == Phase.CLOSED) return stopping.also { stopping = null }
        val interval = when (phase) { Phase.OPENING -> 100; Phase.ARMING -> 50; else -> 20 }
        if (lastSent?.let { now - it < interval } == true) return null
        val outgoing = when (phase) {
            Phase.OPENING -> packet(UdpControlPacket.OPEN, 0)
            Phase.ARMING -> packet(UdpControlPacket.ARM, 1).also {
                if (armStarted == null) { armStarted = now; pending[1] = Sent(it, now) }
            }
            Phase.ACTIVE -> {
                sequence = (sequence + 1) and 0xffffffffL
                packet(UdpControlPacket.DRIVE, sequence, throttle, steering, lastAck).also {
                    pending[(sequence % 64).toInt()] = Sent(it, now)
                }
            }
            Phase.CLOSED -> return null
        }
        lastSent = now
        return outgoing
    }

    fun receive(p: UdpControlPacket, now: Long): Boolean {
        expire(now)
        if (phase == Phase.CLOSED || p.chipId != chipId || p.attempt != attempt || p.basis != 0L) return false
        if (phase == Phase.OPENING) {
            if (p.kind != UdpControlPacket.HELLO || p.token == 0L || p.sequence != 0L || p.throttle != 0 || p.steering != 0) return false
            token = p.token; helloAt = now; lastSent = null; phase = Phase.ARMING
            return true
        }
        if (p.kind != UdpControlPacket.ACK || p.token != token || p.sequence !in 0..0xffffffffL) return false
        val sent = pending[(p.sequence % 64).toInt()] ?: return false
        val distance = (p.sequence - lastAck) and 0xffffffffL
        if (sent.packet.sequence != p.sequence || distance !in 1 until 0x80000000L ||
            now - sent.at >= 200 || p.throttle != sent.packet.throttle || p.steering != sent.packet.steering) return false
        if (phase == Phase.ARMING && p.sequence != 1L) return false
        lastAck = p.sequence; confirmedAt = sent.at; acknowledgements++
        acknowledgedThrottle = p.throttle; acknowledgedSteering = p.steering
        maxRttMs = maxOf(maxRttMs, now - sent.at)
        pending[(p.sequence % 64).toInt()] = null
        if (phase == Phase.ARMING) { sequence = 1; phase = Phase.ACTIVE; reason = "Connected" }
        return true
    }

    private fun packet(kind: Int, seq: Long, t: Int = 0, s: Int = 0, basis: Long = 0) =
        UdpControlPacket(kind, attempt, token, seq, t, s, chipId, basis)
}
