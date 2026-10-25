package vea.raceremote.control;

import java.util.Iterator;
import kotlin.Metadata;
import kotlin.collections.IntIterator;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;

/* JADX INFO: compiled from: DiscoveryPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class DiscoveryPacket {
    public static final int PORT = 3333;
    public static final DiscoveryPacket INSTANCE = new DiscoveryPacket();
    public static final int $stable = 0;

    private DiscoveryPacket() {
    }

    public static /* synthetic */ byte[] query$default(DiscoveryPacket discoveryPacket, long j, int i, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            i = 1;
        }
        return discoveryPacket.query(j, i);
    }

    /* JADX WARN: Code duplicated, block: B:16:0x0033  */
    public final byte[] query(long nonce, int version) {
        boolean z;
        if (!(nonce <= 4294967295L && ((long) 1) <= nonce)) {
            z = false;
        } else if (version <= 2 && 1 <= version) {
            z = true;
        } else {
            z = false;
        }
        if (!z) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        byte[] bytes = new byte[12];
        bytes[0] = 82;
        bytes[1] = 82;
        bytes[2] = 68;
        bytes[3] = (byte) version;
        bytes[4] = 1;
        bytes[5] = 0;
        bytes[6] = 0;
        bytes[7] = 0;
        bytes[8] = 0;
        bytes[9] = 0;
        bytes[10] = 0;
        bytes[11] = 0;
        int iM6446Int$arg0$callrepeat$fun$anonymous$$arg0$callalso$funquery$classDiscoveryPacket = 4;
        for (int i = 0; i < iM6446Int$arg0$callrepeat$fun$anonymous$$arg0$callalso$funquery$classDiscoveryPacket; i++) {
            int it = i;
            bytes[8 + it] = (byte) (nonce >>> (8 * it));
        }
        return bytes;
    }

    public final DiscoveredCar reply(byte[] bytes, long expectedNonce, String host) {
        String strM6485String$branch2$when$valtransport$funreply$classDiscoveryPacket;
        Intrinsics.checkNotNullParameter(bytes, "bytes");
        Intrinsics.checkNotNullParameter(host, "host");
        if (bytes.length == 24 && bytes[0] == 82 && bytes[1] == 82 && bytes[2] == 68) {
            byte b = bytes[3];
            if ((1 <= b && b < 3) && bytes[4] == 2 && bytes[5] == 0 && bytes[20] == 1) {
                byte b2 = bytes[21];
                if ((b2 >= 0 && b2 < 2) && bytes[23] == ((byte) 0)) {
                    int version = bytes[3];
                    if (version == 1 && bytes[22] == ((byte) 0)) {
                        strM6485String$branch2$when$valtransport$funreply$classDiscoveryPacket = "websocket-v1";
                    } else if (version == 2 && bytes[22] == ((byte) 1)) {
                        strM6485String$branch2$when$valtransport$funreply$classDiscoveryPacket = "websocket-v1";
                    } else {
                        if (version != 2 || bytes[22] != ((byte) 2)) {
                            return null;
                        }
                        strM6485String$branch2$when$valtransport$funreply$classDiscoveryPacket = "udp-v1";
                    }
                    String transport = strM6485String$branch2$when$valtransport$funreply$classDiscoveryPacket;
                    if (expectedNonce == 0 || reply$number(bytes, 8, 4) != expectedNonce) {
                        return null;
                    }
                    int port = (int) reply$number(bytes, 6, 2);
                    long chipId = reply$number(bytes, 12, 8);
                    if (port != 0) {
                        if (chipId <= 281474976710655L && ((long) 1) <= chipId) {
                            if (!Intrinsics.areEqual(transport, "udp-v1") || port == 3334) {
                                return new DiscoveredCar(host, port, chipId, bytes[21] == ((byte) 1), transport, version);
                            }
                            return null;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private static final long reply$number(byte[] $bytes, int offset, int count) {
        Iterable $this$fold$iv = RangesKt.until(0, count);
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
