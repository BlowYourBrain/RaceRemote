package vea.raceremote.control;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: CarControlConnection.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class ControlEndpoint {
    public static final int $stable = 0;
    private final long chipId;
    private final int port;
    private final String transport;

    public static /* synthetic */ ControlEndpoint copy$default(ControlEndpoint controlEndpoint, String str, int i, long j, int i2, Object obj) {
        if ((i2 & 1) != 0) {
            str = controlEndpoint.transport;
        }
        if ((i2 & 2) != 0) {
            i = controlEndpoint.port;
        }
        if ((i2 & 4) != 0) {
            j = controlEndpoint.chipId;
        }
        return controlEndpoint.copy(str, i, j);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final String getTransport() {
        return this.transport;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final int getPort() {
        return this.port;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final long getChipId() {
        return this.chipId;
    }

    public final ControlEndpoint copy(String transport, int port, long chipId) {
        Intrinsics.checkNotNullParameter(transport, "transport");
        return new ControlEndpoint(transport, port, chipId);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ControlEndpoint)) {
            return false;
        }
        ControlEndpoint controlEndpoint = (ControlEndpoint) other;
        if (!Intrinsics.areEqual(this.transport, controlEndpoint.transport)) {
            return false;
        }
        if (this.port != controlEndpoint.port) {
            return false;
        }
        return this.chipId != controlEndpoint.chipId ? false : true;
    }

    public int hashCode() {
        return (31 * ((31 * this.transport.hashCode()) + Integer.hashCode(this.port))) + Long.hashCode(this.chipId);
    }

    public String toString() {
        return "ControlEndpoint(" + "transport=" + this.transport + ", " + "port=" + this.port + ", " + "chipId=" + this.chipId + ")";
    }

    public ControlEndpoint(String transport, int port, long chipId) {
        Intrinsics.checkNotNullParameter(transport, "transport");
        this.transport = transport;
        this.port = port;
        this.chipId = chipId;
    }

    public final long getChipId() {
        return this.chipId;
    }

    public final int getPort() {
        return this.port;
    }

    public final String getTransport() {
        return this.transport;
    }
}
