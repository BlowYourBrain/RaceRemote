package vea.raceremote.control;

import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.CharsKt;

/* JADX INFO: compiled from: DiscoveryPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class DiscoveredCar {
    public static final int $stable = 0;
    private final boolean busy;
    private final long chipId;
    private final int discoveryVersion;
    private final String host;
    private final int port;
    private final String transport;

    public static /* synthetic */ DiscoveredCar copy$default(DiscoveredCar discoveredCar, String str, int i, long j, boolean z, String str2, int i2, int i3, Object obj) {
        if ((i3 & 1) != 0) {
            str = discoveredCar.host;
        }
        if ((i3 & 2) != 0) {
            i = discoveredCar.port;
        }
        int i4 = i;
        if ((i3 & 4) != 0) {
            j = discoveredCar.chipId;
        }
        long j2 = j;
        if ((i3 & 8) != 0) {
            z = discoveredCar.busy;
        }
        boolean z2 = z;
        if ((i3 & 16) != 0) {
            str2 = discoveredCar.transport;
        }
        String str3 = str2;
        if ((i3 & 32) != 0) {
            i2 = discoveredCar.discoveryVersion;
        }
        return discoveredCar.copy(str, i4, j2, z2, str3, i2);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final String getHost() {
        return this.host;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final int getPort() {
        return this.port;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final long getChipId() {
        return this.chipId;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final boolean getBusy() {
        return this.busy;
    }

    /* JADX INFO: renamed from: component5, reason: from getter */
    public final String getTransport() {
        return this.transport;
    }

    /* JADX INFO: renamed from: component6, reason: from getter */
    public final int getDiscoveryVersion() {
        return this.discoveryVersion;
    }

    public final DiscoveredCar copy(String host, int port, long chipId, boolean busy, String transport, int discoveryVersion) {
        Intrinsics.checkNotNullParameter(host, "host");
        Intrinsics.checkNotNullParameter(transport, "transport");
        return new DiscoveredCar(host, port, chipId, busy, transport, discoveryVersion);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiscoveredCar)) {
            return false;
        }
        DiscoveredCar discoveredCar = (DiscoveredCar) other;
        if (!Intrinsics.areEqual(this.host, discoveredCar.host)) {
            return false;
        }
        if (this.port != discoveredCar.port) {
            return false;
        }
        if (this.chipId != discoveredCar.chipId) {
            return false;
        }
        if (this.busy != discoveredCar.busy) {
            return false;
        }
        if (Intrinsics.areEqual(this.transport, discoveredCar.transport)) {
            return this.discoveryVersion != discoveredCar.discoveryVersion ? false : true;
        }
        return false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v7, types: [int] */
    /* JADX WARN: Type inference failed for: r2v10 */
    /* JADX WARN: Type inference failed for: r2v11 */
    /* JADX WARN: Type inference failed for: r2v5, types: [int] */
    public int hashCode() {
        int iM6449Int$arg0$calltimes$$this$callplus$setresult2$funhashCode$classDiscoveredCar = 31 * ((31 * ((31 * this.host.hashCode()) + Integer.hashCode(this.port))) + Long.hashCode(this.chipId));
        boolean z = this.busy;
        ?? r2 = z;
        if (z) {
            r2 = 1;
        }
        return (31 * ((31 * (iM6449Int$arg0$calltimes$$this$callplus$setresult2$funhashCode$classDiscoveredCar + r2)) + this.transport.hashCode())) + Integer.hashCode(this.discoveryVersion);
    }

    public String toString() {
        return "DiscoveredCar(" + "host=" + this.host + ", " + "port=" + this.port + ", " + "chipId=" + this.chipId + ", " + "busy=" + this.busy + ", " + "transport=" + this.transport + ", " + "discoveryVersion=" + this.discoveryVersion + ")";
    }

    public DiscoveredCar(String host, int port, long chipId, boolean busy, String transport, int discoveryVersion) {
        Intrinsics.checkNotNullParameter(host, "host");
        Intrinsics.checkNotNullParameter(transport, "transport");
        this.host = host;
        this.port = port;
        this.chipId = chipId;
        this.busy = busy;
        this.transport = transport;
        this.discoveryVersion = discoveryVersion;
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public /* synthetic */ DiscoveredCar(String str, int i, long j, boolean z, String str2, int i2, int i3, DefaultConstructorMarker defaultConstructorMarker) {
        String strM6486String$paramtransport$classDiscoveredCar;
        int iM6462Int$paramdiscoveryVersion$classDiscoveredCar;
        if ((i3 & 16) == 0) {
            strM6486String$paramtransport$classDiscoveredCar = str2;
        } else {
            strM6486String$paramtransport$classDiscoveredCar = "websocket-v1";
        }
        if ((i3 & 32) == 0) {
            iM6462Int$paramdiscoveryVersion$classDiscoveredCar = i2;
        } else {
            iM6462Int$paramdiscoveryVersion$classDiscoveredCar = 1;
        }
        this(str, i, j, z, strM6486String$paramtransport$classDiscoveredCar, iM6462Int$paramdiscoveryVersion$classDiscoveredCar);
    }

    public final boolean getBusy() {
        return this.busy;
    }

    public final long getChipId() {
        return this.chipId;
    }

    public final String getHost() {
        return this.host;
    }

    public final int getPort() {
        return this.port;
    }

    public final int getDiscoveryVersion() {
        return this.discoveryVersion;
    }

    public final String getTransport() {
        return this.transport;
    }

    public final String getName() {
        StringBuilder sbAppend = new StringBuilder().append("RaceRemote-");
        String string = Long.toString(this.chipId >>> 24, CharsKt.checkRadix(16));
        Intrinsics.checkNotNullExpressionValue(string, "toString(this, checkRadix(radix))");
        return sbAppend.append(string).toString();
    }
}
