package vea.raceremote.control;

import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: UdpControlSession.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class UdpControlSession {
    public static final int $stable = 8;
    private int acknowledgedSteering;
    private int acknowledgedThrottle;
    private int acknowledgements;
    private Long armStarted;
    private final long attempt;
    private final long chipId;
    private long confirmedAt;
    private long helloAt;
    private long lastAck;
    private Long lastSent;
    private long maxRttMs;
    private final long openedAt;
    private final Sent[] pending = new Sent[64];
    private Phase phase = Phase.OPENING;
    private String reason = "Connecting";
    private long sequence;
    private int steering;
    private UdpControlPacket stopping;
    private int throttle;
    private long token;

    /* JADX INFO: compiled from: UdpControlSession.kt */
    public enum Phase {
        OPENING,
        ARMING,
        ACTIVE,
        CLOSED
    }

    /* JADX INFO: compiled from: UdpControlSession.kt */
    public /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[Phase.values().length];
            try {
                iArr[Phase.OPENING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Phase.ARMING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Phase.ACTIVE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Phase.CLOSED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    /* JADX WARN: Code duplicated, block: B:17:0x004e  */
    public UdpControlSession(long chipId, long attempt, long openedAt) {
        boolean z;
        this.chipId = chipId;
        this.attempt = attempt;
        this.openedAt = openedAt;
        if (((long) 1) <= chipId && chipId <= 281474976710655L) {
            z = (((long) 1) > attempt ? 1 : (((long) 1) == attempt ? 0 : -1)) <= 0 && (attempt > 4294967295L ? 1 : (attempt == 4294967295L ? 0 : -1)) <= 0;
        }
        if (z) {
        } else {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
    }

    public final long getAttempt() {
        return this.attempt;
    }

    public final long getChipId() {
        return this.chipId;
    }

    /* JADX INFO: compiled from: UdpControlSession.kt */
    public static final /* data */ class Snapshot {
        public static final int $stable = 0;
        private final int acknowledgedSteering;
        private final int acknowledgedThrottle;
        private final int acknowledgements;
        private final long lastAck;
        private final long maxRttMs;
        private final Phase phase;
        private final String reason;
        private final int steering;
        private final int throttle;

        /* JADX INFO: renamed from: component1, reason: from getter */
        public final Phase getPhase() {
            return this.phase;
        }

        /* JADX INFO: renamed from: component2, reason: from getter */
        public final String getReason() {
            return this.reason;
        }

        /* JADX INFO: renamed from: component3, reason: from getter */
        public final int getAcknowledgements() {
            return this.acknowledgements;
        }

        /* JADX INFO: renamed from: component4, reason: from getter */
        public final long getLastAck() {
            return this.lastAck;
        }

        /* JADX INFO: renamed from: component5, reason: from getter */
        public final int getThrottle() {
            return this.throttle;
        }

        /* JADX INFO: renamed from: component6, reason: from getter */
        public final int getSteering() {
            return this.steering;
        }

        /* JADX INFO: renamed from: component7, reason: from getter */
        public final long getMaxRttMs() {
            return this.maxRttMs;
        }

        /* JADX INFO: renamed from: component8, reason: from getter */
        public final int getAcknowledgedThrottle() {
            return this.acknowledgedThrottle;
        }

        /* JADX INFO: renamed from: component9, reason: from getter */
        public final int getAcknowledgedSteering() {
            return this.acknowledgedSteering;
        }

        public final Snapshot copy(Phase phase, String reason, int acknowledgements, long lastAck, int throttle, int steering, long maxRttMs, int acknowledgedThrottle, int acknowledgedSteering) {
            Intrinsics.checkNotNullParameter(phase, "phase");
            Intrinsics.checkNotNullParameter(reason, "reason");
            return new Snapshot(phase, reason, acknowledgements, lastAck, throttle, steering, maxRttMs, acknowledgedThrottle, acknowledgedSteering);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Snapshot)) {
                return false;
            }
            Snapshot snapshot = (Snapshot) other;
            if (this.phase != snapshot.phase) {
                return false;
            }
            if (!Intrinsics.areEqual(this.reason, snapshot.reason)) {
                return false;
            }
            if (this.acknowledgements != snapshot.acknowledgements) {
                return false;
            }
            if (this.lastAck != snapshot.lastAck) {
                return false;
            }
            if (this.throttle != snapshot.throttle) {
                return false;
            }
            if (this.steering != snapshot.steering) {
                return false;
            }
            if (this.maxRttMs != snapshot.maxRttMs) {
                return false;
            }
            if (this.acknowledgedThrottle != snapshot.acknowledgedThrottle) {
                return false;
            }
            return this.acknowledgedSteering != snapshot.acknowledgedSteering ? false : true;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * this.phase.hashCode()) + this.reason.hashCode())) + Integer.hashCode(this.acknowledgements))) + Long.hashCode(this.lastAck))) + Integer.hashCode(this.throttle))) + Integer.hashCode(this.steering))) + Long.hashCode(this.maxRttMs))) + Integer.hashCode(this.acknowledgedThrottle))) + Integer.hashCode(this.acknowledgedSteering);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Snapshot(").append("phase=").append(this.phase).append(", ").append("reason=").append(this.reason).append(", ").append("acknowledgements=").append(this.acknowledgements).append(", ").append("lastAck=").append(this.lastAck).append(", ").append("throttle=").append(this.throttle).append(", ").append("steering=").append(this.steering).append(", ").append("maxRttMs=").append(this.maxRttMs).append(", ").append("acknowledgedThrottle=");
            sb.append(this.acknowledgedThrottle).append(", ").append("acknowledgedSteering=").append(this.acknowledgedSteering).append(")");
            return sb.toString();
        }

        public Snapshot(Phase phase, String reason, int acknowledgements, long lastAck, int throttle, int steering, long maxRttMs, int acknowledgedThrottle, int acknowledgedSteering) {
            Intrinsics.checkNotNullParameter(phase, "phase");
            Intrinsics.checkNotNullParameter(reason, "reason");
            this.phase = phase;
            this.reason = reason;
            this.acknowledgements = acknowledgements;
            this.lastAck = lastAck;
            this.throttle = throttle;
            this.steering = steering;
            this.maxRttMs = maxRttMs;
            this.acknowledgedThrottle = acknowledgedThrottle;
            this.acknowledgedSteering = acknowledgedSteering;
        }

        public final int getAcknowledgements() {
            return this.acknowledgements;
        }

        public final long getLastAck() {
            return this.lastAck;
        }

        public final Phase getPhase() {
            return this.phase;
        }

        public final String getReason() {
            return this.reason;
        }

        public final long getMaxRttMs() {
            return this.maxRttMs;
        }

        public final int getSteering() {
            return this.steering;
        }

        public final int getThrottle() {
            return this.throttle;
        }

        public final int getAcknowledgedSteering() {
            return this.acknowledgedSteering;
        }

        public final int getAcknowledgedThrottle() {
            return this.acknowledgedThrottle;
        }
    }

    /* JADX INFO: compiled from: UdpControlSession.kt */
    private static final /* data */ class Sent {
        private final long at;
        private final UdpControlPacket packet;

        public static /* synthetic */ Sent copy$default(Sent sent, UdpControlPacket udpControlPacket, long j, int i, Object obj) {
            if ((i & 1) != 0) {
                udpControlPacket = sent.packet;
            }
            if ((i & 2) != 0) {
                j = sent.at;
            }
            return sent.copy(udpControlPacket, j);
        }

        /* JADX INFO: renamed from: component1, reason: from getter */
        public final UdpControlPacket getPacket() {
            return this.packet;
        }

        /* JADX INFO: renamed from: component2, reason: from getter */
        public final long getAt() {
            return this.at;
        }

        public final Sent copy(UdpControlPacket packet, long at) {
            Intrinsics.checkNotNullParameter(packet, "packet");
            return new Sent(packet, at);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Sent)) {
                return false;
            }
            Sent sent = (Sent) other;
            if (Intrinsics.areEqual(this.packet, sent.packet)) {
                return this.at != sent.at ? false : true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.packet.hashCode()) + Long.hashCode(this.at);
        }

        public String toString() {
            return "Sent(" + "packet=" + this.packet + ", " + "at=" + this.at + ")";
        }

        public Sent(UdpControlPacket packet, long at) {
            Intrinsics.checkNotNullParameter(packet, "packet");
            this.packet = packet;
            this.at = at;
        }

        public final long getAt() {
            return this.at;
        }

        public final UdpControlPacket getPacket() {
            return this.packet;
        }
    }

    public final Snapshot snapshot() {
        return new Snapshot(this.phase, this.reason, this.acknowledgements, this.lastAck, this.throttle, this.steering, this.maxRttMs, this.acknowledgedThrottle, this.acknowledgedSteering);
    }

    /* JADX WARN: Code duplicated, block: B:15:0x002d  */
    public final void input(int throttle, int steering) {
        boolean z;
        if (throttle <= 1000 && -1000 <= throttle) {
            z = steering <= 1000 && -1000 <= steering;
        }
        if (!z) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        if (this.phase == Phase.ACTIVE) {
            this.throttle = throttle;
            this.steering = steering;
        }
    }

    public static /* synthetic */ void stop$default(UdpControlSession udpControlSession, String str, int i, Object obj) {
        if ((i & 1) != 0) {
            str = "Stopped";
        }
        udpControlSession.stop(str);
    }

    public final void stop(String reason) {
        Intrinsics.checkNotNullParameter(reason, "reason");
        if (this.phase == Phase.CLOSED) {
            return;
        }
        if (this.token != 0L) {
            this.stopping = packet$default(this, 3, this.sequence, 0, 0, 0L, 28, null);
        }
        this.phase = Phase.CLOSED;
        this.reason = reason;
        this.throttle = 0;
        this.steering = 0;
        this.acknowledgedThrottle = 0;
        this.acknowledgedSteering = 0;
        ArraysKt.fill$default(this.pending, (Object) null, 0, 0, 6, (Object) null);
    }

    private final void expire(long now) {
        switch (WhenMappings.$EnumSwitchMapping$0[this.phase.ordinal()]) {
            case 1:
                if (now - this.openedAt >= 4000) {
                    stop("HELLO timeout");
                }
                break;
            case 2:
                Long l = this.armStarted;
                if (now - (l != null ? l.longValue() : this.helloAt) >= 200) {
                    stop("ARM timeout");
                }
                break;
            case 3:
                if (now - this.confirmedAt >= 200) {
                    stop("ACK freshness timeout");
                }
                break;
        }
    }

    public final UdpControlPacket next(long now) {
        int iM6732Int$branch$when$valinterval$funnext$classUdpControlSession;
        UdpControlPacket it;
        expire(now);
        if (this.phase == Phase.CLOSED) {
            UdpControlPacket udpControlPacket = this.stopping;
            this.stopping = null;
            return udpControlPacket;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[this.phase.ordinal()]) {
            case 1:
                iM6732Int$branch$when$valinterval$funnext$classUdpControlSession = 100;
                break;
            case 2:
                iM6732Int$branch$when$valinterval$funnext$classUdpControlSession = 50;
                break;
            default:
                iM6732Int$branch$when$valinterval$funnext$classUdpControlSession = 20;
                break;
        }
        int interval = iM6732Int$branch$when$valinterval$funnext$classUdpControlSession;
        Long l = this.lastSent;
        boolean z = false;
        if (l != null) {
            long it2 = l.longValue();
            if ((now - it2 < ((long) interval)) == true) {
                z = true;
            }
        }
        if (z) {
            return null;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[this.phase.ordinal()]) {
            case 1:
                it = packet$default(this, 0, 0L, 0, 0, 0L, 28, null);
                break;
            case 2:
                it = packet$default(this, 1, 1, 0, 0, 0L, 28, null);
                if (this.armStarted == null) {
                    this.armStarted = Long.valueOf(now);
                    this.pending[1] = new Sent(it, now);
                }
                break;
            case 3:
                long jM6740Long$arg0$calland$arg0$call$setsequence$$branch2$when$valp$funnext$classUdpControlSession = 4294967295L & (this.sequence + ((long) 1));
                this.sequence = jM6740Long$arg0$calland$arg0$call$setsequence$$branch2$when$valp$funnext$classUdpControlSession;
                it = packet(2, jM6740Long$arg0$calland$arg0$call$setsequence$$branch2$when$valp$funnext$classUdpControlSession, this.throttle, this.steering, this.lastAck);
                this.pending[(int) (this.sequence % ((long) 64))] = new Sent(it, now);
                break;
            case 4:
                return null;
            default:
                throw new NoWhenBranchMatchedException();
        }
        this.lastSent = Long.valueOf(now);
        return it;
    }

    public final boolean receive(UdpControlPacket p, long now) {
        Intrinsics.checkNotNullParameter(p, "p");
        expire(now);
        if (this.phase == Phase.CLOSED || p.getChipId() != this.chipId || p.getAttempt() != this.attempt || p.getBasis() != 0L) {
            return false;
        }
        if (this.phase == Phase.OPENING) {
            if (p.getKind() != 128 || p.getToken() == 0 || p.getSequence() != 0 || p.getThrottle() != 0 || p.getSteering() != 0) {
                return false;
            }
            this.token = p.getToken();
            this.helloAt = now;
            this.lastSent = null;
            this.phase = Phase.ARMING;
            return true;
        }
        if (p.getKind() != 129 || p.getToken() != this.token) {
            return false;
        }
        Sent sent = this.pending[(int) (p.getSequence() % ((long) 64))];
        if (sent == null) {
            return false;
        }
        long distance = (p.getSequence() - this.lastAck) & 4294967295L;
        if (sent.getPacket().getSequence() == p.getSequence()) {
            boolean z = false;
            if (1 <= distance && distance < 2147483648L) {
                z = true;
            }
            if (z && now - sent.getAt() < 200 && p.getThrottle() == sent.getPacket().getThrottle() && p.getSteering() == sent.getPacket().getSteering()) {
                if (this.phase == Phase.ARMING && p.getSequence() != 1) {
                    return false;
                }
                this.lastAck = p.getSequence();
                this.confirmedAt = sent.getAt();
                this.acknowledgements++;
                this.acknowledgedThrottle = p.getThrottle();
                this.acknowledgedSteering = p.getSteering();
                this.maxRttMs = Math.max(this.maxRttMs, now - sent.getAt());
                this.pending[(int) (p.getSequence() % ((long) 64))] = null;
                if (this.phase == Phase.ARMING) {
                    this.sequence = 1;
                    this.phase = Phase.ACTIVE;
                    this.reason = "Connected";
                }
                return true;
            }
        }
        return false;
    }

    static /* synthetic */ UdpControlPacket packet$default(UdpControlSession udpControlSession, int i, long j, int i2, int i3, long j2, int i4, Object obj) {
        return udpControlSession.packet(i, j, (i4 & 4) != 0 ? 0 : i2, (i4 & 8) != 0 ? 0 : i3, (i4 & 16) != 0 ? 0L : j2);
    }

    private final UdpControlPacket packet(int kind, long seq, int t, int s, long basis) {
        return new UdpControlPacket(kind, this.attempt, this.token, seq, t, s, this.chipId, basis);
    }
}
