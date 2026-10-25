package vea.raceremote.control;

import java.util.Iterator;
import kotlin.Metadata;
import kotlin.collections.IntIterator;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;

/* JADX INFO: compiled from: UdpControlPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class UdpControlPacket {
    public static final int ACK = 129;
    public static final int ARM = 1;
    public static final int DRIVE = 2;
    public static final int HELLO = 128;
    public static final int OPEN = 0;
    public static final int PORT = 3334;
    public static final int SIZE = 32;
    public static final int STOP = 3;
    private final long attempt;
    private final long basis;
    private final long chipId;
    private final int kind;
    private final long sequence;
    private final int steering;
    private final int throttle;
    private final long token;

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion();
    public static final int $stable = 0;

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final int getKind() {
        return this.kind;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final long getAttempt() {
        return this.attempt;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final long getToken() {
        return this.token;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final long getSequence() {
        return this.sequence;
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
    public final long getChipId() {
        return this.chipId;
    }

    /* JADX INFO: renamed from: component8, reason: from getter */
    public final long getBasis() {
        return this.basis;
    }

    public final UdpControlPacket copy(int kind, long attempt, long token, long sequence, int throttle, int steering, long chipId, long basis) {
        return new UdpControlPacket(kind, attempt, token, sequence, throttle, steering, chipId, basis);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UdpControlPacket)) {
            return false;
        }
        UdpControlPacket udpControlPacket = (UdpControlPacket) other;
        if (this.kind != udpControlPacket.kind) {
            return false;
        }
        if (this.attempt != udpControlPacket.attempt) {
            return false;
        }
        if (this.token != udpControlPacket.token) {
            return false;
        }
        if (this.sequence != udpControlPacket.sequence) {
            return false;
        }
        if (this.throttle != udpControlPacket.throttle) {
            return false;
        }
        if (this.steering != udpControlPacket.steering) {
            return false;
        }
        if (this.chipId != udpControlPacket.chipId) {
            return false;
        }
        return this.basis != udpControlPacket.basis ? false : true;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * Integer.hashCode(this.kind)) + Long.hashCode(this.attempt))) + Long.hashCode(this.token))) + Long.hashCode(this.sequence))) + Integer.hashCode(this.throttle))) + Integer.hashCode(this.steering))) + Long.hashCode(this.chipId))) + Long.hashCode(this.basis);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UdpControlPacket(").append("kind=").append(this.kind).append(", ").append("attempt=").append(this.attempt).append(", ").append("token=").append(this.token).append(", ").append("sequence=").append(this.sequence).append(", ").append("throttle=").append(this.throttle).append(", ").append("steering=").append(this.steering).append(", ").append("chipId=").append(this.chipId).append(", ").append("basis=");
        sb.append(this.basis).append(")");
        return sb.toString();
    }

    public UdpControlPacket(int kind, long attempt, long token, long sequence, int throttle, int steering, long chipId, long basis) {
        this.kind = kind;
        this.attempt = attempt;
        this.token = token;
        this.sequence = sequence;
        this.throttle = throttle;
        this.steering = steering;
        this.chipId = chipId;
        this.basis = basis;
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public /* synthetic */ UdpControlPacket(int i, long j, long j2, long j3, int i2, int i3, long j4, long j5, int i4, DefaultConstructorMarker defaultConstructorMarker) {
        long jM6660Long$parambasis$classUdpControlPacket;
        if ((i4 & 128) == 0) {
            jM6660Long$parambasis$classUdpControlPacket = j5;
        } else {
            jM6660Long$parambasis$classUdpControlPacket = 0L;
        }
        this(i, j, j2, j3, i2, i3, j4, jM6660Long$parambasis$classUdpControlPacket);
    }

    public final int getKind() {
        return this.kind;
    }

    public final long getAttempt() {
        return this.attempt;
    }

    public final long getToken() {
        return this.token;
    }

    public final long getSequence() {
        return this.sequence;
    }

    public final int getThrottle() {
        return this.throttle;
    }

    public final int getSteering() {
        return this.steering;
    }

    public final long getChipId() {
        return this.chipId;
    }

    public final long getBasis() {
        return this.basis;
    }

    /* JADX WARN: Code duplicated, block: B:30:0x006f  */
    /* JADX WARN: Code duplicated, block: B:53:0x00c0  */
    public final byte[] encode() {
        boolean z;
        boolean z2;
        int iM6599Int$$this$callrangeTo$$this$callcontains$cond$cond$cond$arg0$callrequire$funencode$classUdpControlPacket = 0;
        int iM6624Int$arg0$callrangeTo$$this$callcontains$cond$cond$cond$arg0$callrequire$funencode$classUdpControlPacket = 255;
        int i = this.kind;
        if (iM6599Int$$this$callrangeTo$$this$callcontains$cond$cond$cond$arg0$callrequire$funencode$classUdpControlPacket <= i && i <= iM6624Int$arg0$callrangeTo$$this$callcontains$cond$cond$cond$arg0$callrequire$funencode$classUdpControlPacket) {
            int iM6594Int$$this$callrangeTo$$this$callcontains$branch$cond$cond$arg0$callrequire$funencode$classUdpControlPacket = 1;
            long jM6657Long$arg0$callrangeTo$$this$callcontains$branch$cond$cond$arg0$callrequire$funencode$classUdpControlPacket = 4294967295L;
            long j = this.attempt;
            if (((long) iM6594Int$$this$callrangeTo$$this$callcontains$branch$cond$cond$arg0$callrequire$funencode$classUdpControlPacket) <= j && j <= jM6657Long$arg0$callrangeTo$$this$callcontains$branch$cond$cond$arg0$callrequire$funencode$classUdpControlPacket) {
                int iM6592Int$$this$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire$funencode$classUdpControlPacket = 0;
                long jM6656Long$arg0$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire$funencode$classUdpControlPacket = 4294967295L;
                long j2 = this.token;
                if (((long) iM6592Int$$this$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire$funencode$classUdpControlPacket) <= j2 && j2 <= jM6656Long$arg0$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire$funencode$classUdpControlPacket) {
                    int iM6590Int$$this$callrangeTo$$this$callcontains$branch$arg0$callrequire$funencode$classUdpControlPacket = 0;
                    long jM6654Long$arg0$callrangeTo$$this$callcontains$branch$arg0$callrequire$funencode$classUdpControlPacket = 4294967295L;
                    long j3 = this.sequence;
                    if (((long) iM6590Int$$this$callrangeTo$$this$callcontains$branch$arg0$callrequire$funencode$classUdpControlPacket) <= j3 && j3 <= jM6654Long$arg0$callrangeTo$$this$callcontains$branch$arg0$callrequire$funencode$classUdpControlPacket) {
                        z = true;
                    } else {
                        z = false;
                    }
                } else {
                    z = false;
                }
            } else {
                z = false;
            }
        } else {
            z = false;
        }
        if (!z) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        int iM6598Int$$this$callrangeTo$$this$callcontains$cond$cond$arg0$callrequire1$funencode$classUdpControlPacket = -1000;
        int iM6623Int$arg0$callrangeTo$$this$callcontains$cond$cond$arg0$callrequire1$funencode$classUdpControlPacket = 1000;
        int i2 = this.throttle;
        if (iM6598Int$$this$callrangeTo$$this$callcontains$cond$cond$arg0$callrequire1$funencode$classUdpControlPacket <= i2 && i2 <= iM6623Int$arg0$callrangeTo$$this$callcontains$cond$cond$arg0$callrequire1$funencode$classUdpControlPacket) {
            int iM6593Int$$this$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire1$funencode$classUdpControlPacket = -1000;
            int iM6620Int$arg0$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire1$funencode$classUdpControlPacket = 1000;
            int i3 = this.steering;
            if (iM6593Int$$this$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire1$funencode$classUdpControlPacket <= i3 && i3 <= iM6620Int$arg0$callrangeTo$$this$callcontains$branch$cond$arg0$callrequire1$funencode$classUdpControlPacket) {
                int iM6591Int$$this$callrangeTo$$this$callcontains$branch$arg0$callrequire1$funencode$classUdpControlPacket = 1;
                long jM6655Long$arg0$callrangeTo$$this$callcontains$branch$arg0$callrequire1$funencode$classUdpControlPacket = 281474976710655L;
                long j4 = this.chipId;
                if (((long) iM6591Int$$this$callrangeTo$$this$callcontains$branch$arg0$callrequire1$funencode$classUdpControlPacket) <= j4 && j4 <= jM6655Long$arg0$callrangeTo$$this$callcontains$branch$arg0$callrequire1$funencode$classUdpControlPacket) {
                    z2 = true;
                } else {
                    z2 = false;
                }
            } else {
                z2 = false;
            }
        } else {
            z2 = false;
        }
        if (!z2) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        int iM6589Int$$this$callrangeTo$$this$callcontains$arg0$callrequire2$funencode$classUdpControlPacket = 0;
        long jM6653Long$arg0$callrangeTo$$this$callcontains$arg0$callrequire2$funencode$classUdpControlPacket = 4294967295L;
        long j5 = this.basis;
        if (!(((long) iM6589Int$$this$callrangeTo$$this$callcontains$arg0$callrequire2$funencode$classUdpControlPacket) <= j5 && j5 <= jM6653Long$arg0$callrangeTo$$this$callcontains$arg0$callrequire2$funencode$classUdpControlPacket)) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        byte[] bytes = new byte[32];
        bytes[0] = 82;
        bytes[1] = 85;
        bytes[2] = 1;
        bytes[3] = (byte) this.kind;
        encode$lambda$1$put(bytes, 4, 4, this.attempt);
        encode$lambda$1$put(bytes, 8, 4, this.token);
        encode$lambda$1$put(bytes, 12, 4, this.sequence);
        encode$lambda$1$put(bytes, 16, 2, this.throttle);
        encode$lambda$1$put(bytes, 18, 2, this.steering);
        encode$lambda$1$put(bytes, 20, 4, this.basis);
        encode$lambda$1$put(bytes, 24, 8, this.chipId);
        return bytes;
    }

    private static final void encode$lambda$1$put(byte[] $bytes, int offset, int size, long value) {
        for (int i = 0; i < size; i++) {
            int it = i;
            $bytes[offset + it] = (byte) (value >>> (8 * it));
        }
    }

    /* JADX INFO: compiled from: UdpControlPacket.kt */
    public static final class Companion {
        private Companion() {
        }

        public final UdpControlPacket decode(byte[] bytes) {
            Intrinsics.checkNotNullParameter(bytes, "bytes");
            if (bytes.length == 32) {
                boolean z = false;
                if (bytes[0] == 82 && bytes[1] == 85 && bytes[2] == ((byte) 1)) {
                    UdpControlPacket p = new UdpControlPacket(bytes[3] & 255, decode$get(bytes, 4, 4), decode$get(bytes, 8, 4), decode$get(bytes, 12, 4), (short) decode$get(bytes, 16, 2), (short) decode$get(bytes, 18, 2), decode$get(bytes, 24, 8), decode$get(bytes, 20, 4));
                    if (p.getAttempt() != 0L) {
                        int iM6595Int$$this$callrangeTo$$this$callcontains$branch$cond$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = 1;
                        long jM6658Long$arg0$callrangeTo$$this$callcontains$branch$cond$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = 281474976710655L;
                        long chipId = p.getChipId();
                        if (((long) iM6595Int$$this$callrangeTo$$this$callcontains$branch$cond$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket) <= chipId && chipId <= jM6658Long$arg0$callrangeTo$$this$callcontains$branch$cond$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket) {
                            int iM6596Int$$this$callrangeTo$$this$callcontains$branch$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = -1000;
                            int iM6621Int$arg0$callrangeTo$$this$callcontains$branch$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = 1000;
                            int throttle = p.getThrottle();
                            if (iM6596Int$$this$callrangeTo$$this$callcontains$branch$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket <= throttle && throttle <= iM6621Int$arg0$callrangeTo$$this$callcontains$branch$cond$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket) {
                                int iM6597Int$$this$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = -1000;
                                int iM6622Int$arg0$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket = 1000;
                                int steering = p.getSteering();
                                if (iM6597Int$$this$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket <= steering && steering <= iM6622Int$arg0$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$calltakeIf$fundecode$classCompanion$classUdpControlPacket) {
                                    z = true;
                                }
                            }
                        }
                    }
                    if (z) {
                        return p;
                    }
                    return null;
                }
            }
            return null;
        }

        private static final long decode$get(byte[] $bytes, int offset, int size) {
            Iterable $this$fold$iv = RangesKt.until(0, size);
            long initial$iv = 0L;
            long accumulator$iv = initial$iv;
            Iterator<Integer> it = $this$fold$iv.iterator();
            while (it.hasNext()) {
                int element$iv = ((IntIterator) it).nextInt();
                long value = accumulator$iv;
                accumulator$iv = value | ((((long) $bytes[offset + element$iv]) & 255) << (8 * element$iv));
            }
            return accumulator$iv;
        }
    }
}
