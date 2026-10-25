package vea.raceremote.control;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.IntRange;
import kotlin.text.StringsKt;

/* JADX INFO: compiled from: UdpControlClient.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class UdpControlClient implements Closeable {
    public static final int $stable = 8;
    private final InetAddress address;
    private final Function1<DatagramSocket, Unit> bindSocket;
    private final Function0<Long> clockMs;
    private final AtomicBoolean closing;
    private final Function1<String, Unit> diagnostics;
    private final Object lock;
    private final UdpControlSession session;
    private volatile DatagramSocket socket;
    private final Thread worker;

    /* JADX WARN: Multi-variable type inference failed */
    public UdpControlClient(String host, long chipId, Function1<? super DatagramSocket, Unit> function1, Function1<? super String, Unit> function2, Function0<Long> function0) throws UnknownHostException {
        long attempt;
        Integer num;
        boolean z;
        Intrinsics.checkNotNullParameter(host, "host");
        Intrinsics.checkNotNullParameter(function1, "bindSocket");
        Intrinsics.checkNotNullParameter(function2, "diagnostics");
        Intrinsics.checkNotNullParameter(function0, "clockMs");
        this.bindSocket = function1;
        this.diagnostics = function2;
        this.clockMs = function0;
        boolean z2 = false;
        this.closing = new AtomicBoolean(false);
        this.lock = new Object();
        boolean z3 = true;
        Iterable $this$map$iv = StringsKt.split$default((CharSequence) host, new char[]{'.'}, false, 0, 6, (Object) null);
        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        for (Object item$iv$iv : $this$map$iv) {
            destination$iv$iv.add(StringsKt.toIntOrNull((String) item$iv$iv));
        }
        List octets = (List) destination$iv$iv;
        if (octets.size() == 4) {
            List $this$all$iv = octets;
            if (!($this$all$iv instanceof Collection) || !$this$all$iv.isEmpty()) {
                Iterator it = $this$all$iv.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        z3 = true;
                        break;
                    }
                    Object element$iv = it.next();
                    Integer it2 = (Integer) element$iv;
                    if (!(it2 != null && new IntRange(0, 255).contains(it2.intValue()))) {
                        z3 = false;
                        break;
                    }
                }
            }
            if (z3) {
                Integer num2 = (Integer) octets.get(0);
                if ((num2 != null && num2.intValue() == 10) || ((num = (Integer) octets.get(0)) != null && num.intValue() == 127)) {
                    z = true;
                } else {
                    Integer num3 = (Integer) octets.get(0);
                    if (num3 != null && num3.intValue() == 192) {
                        z = true;
                        Integer num4 = (Integer) octets.get(1);
                        if (num4 != null && num4.intValue() == 168) {
                        }
                    } else {
                        z = true;
                    }
                    Integer num5 = (Integer) octets.get(0);
                    int iM6557Int$arg1$callEQEQ$cond$else$branch$arg0$callrequire$classUdpControlClient = 172;
                    if (num5 != null && num5.intValue() == iM6557Int$arg1$callEQEQ$cond$else$branch$arg0$callrequire$classUdpControlClient) {
                        int iM6546Int$$this$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$classUdpControlClient = 16;
                        int iM6553Int$arg0$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$classUdpControlClient = 31;
                        Object obj = octets.get(1);
                        Intrinsics.checkNotNull(obj);
                        int iIntValue = ((Number) obj).intValue();
                        if ((iM6546Int$$this$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$classUdpControlClient > iIntValue || iIntValue > iM6553Int$arg0$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$classUdpControlClient) ? false : z) {
                        }
                    }
                }
                z2 = z;
            }
        }
        if (!z2) {
            throw new IllegalArgumentException("Local IPv4 required".toString());
        }
        InetAddress byName = InetAddress.getByName(host);
        Intrinsics.checkNotNullExpressionValue(byName, "getByName(host)");
        this.address = byName;
        SecureRandom random = new SecureRandom();
        do {
            attempt = ((long) random.nextInt()) & 4294967295L;
        } while (attempt == 0L);
        this.session = new UdpControlSession(chipId, attempt, invoke().longValue());
        Thread $this$_init__u24lambda_u244 = new Thread(new Runnable() { // from class: vea.raceremote.control.UdpControlClient$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UdpControlClient._init_$lambda$3(this.f$0);
            }
        }, "rc-udp-control");
        $this$_init__u24lambda_u244.setDaemon(true);
        $this$_init__u24lambda_u244.start();
        this.worker = $this$_init__u24lambda_u244;
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public /* synthetic */ UdpControlClient(String str, long j, Function1 function1, Function1 function2, Function0 function0, int i, DefaultConstructorMarker defaultConstructorMarker) {
        Function1 function3;
        Function0 function4;
        if ((i & 8) == 0) {
            function3 = function2;
        } else {
            function3 = new Function1<String, Unit>() { // from class: vea.raceremote.control.UdpControlClient.1
                @Override // kotlin.jvm.functions.Function1
                public /* bridge */ /* synthetic */ Unit invoke(String str2) {
                    invoke2(str2);
                    return Unit.INSTANCE;
                }

                /* JADX INFO: renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2(String it) {
                    Intrinsics.checkNotNullParameter(it, "it");
                }
            };
        }
        if ((i & 16) == 0) {
            function4 = function0;
        } else {
            function4 = new Function0<Long>() { // from class: vea.raceremote.control.UdpControlClient.2
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // kotlin.jvm.functions.Function0
                public final Long invoke() {
                    return Long.valueOf(System.nanoTime() / ((long) 1000000));
                }
            };
        }
        this(str, j, function1, function3, function4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void _init_$lambda$3(UdpControlClient this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.run();
    }

    public final UdpControlSession.Snapshot snapshot() {
        UdpControlSession.Snapshot snapshot;
        synchronized (this.lock) {
            snapshot = this.session.snapshot();
        }
        return snapshot;
    }

    public final void input(int throttle, int steering) {
        synchronized (this.lock) {
            if (!this.closing.get()) {
                this.session.input(throttle, steering);
            }
            Unit unit = Unit.INSTANCE;
        }
    }

    private final void run() {
        Function1<String, Unit> function1;
        StringBuilder sb;
        UdpControlPacket outgoing;
        boolean z;
        try {
            try {
                DatagramSocket datagramSocket = new DatagramSocket();
                try {
                    DatagramSocket udp = datagramSocket;
                    this.socket = udp;
                    invoke(udp);
                    udp.connect(this.address, 3334);
                    udp.setSoTimeout(5);
                    byte[] buffer = new byte[256];
                    while (true) {
                        synchronized (this.lock) {
                            if (this.closing.get()) {
                                UdpControlSession.stop$default(this.session, null, 1, null);
                            }
                            outgoing = this.session.next(invoke().longValue());
                        }
                        if (outgoing != null) {
                            byte[] bytes = outgoing.encode();
                            udp.send(new DatagramPacket(bytes, bytes.length));
                            invoke("sent kind=" + outgoing.getKind() + " seq=" + outgoing.getSequence() + " basis=" + outgoing.getBasis());
                        }
                        if (snapshot().getPhase() == UdpControlSession.Phase.CLOSED) {
                            Unit unit = Unit.INSTANCE;
                            CloseableKt.closeFinally(datagramSocket, null);
                            this.socket = null;
                            synchronized (this.lock) {
                                UdpControlSession.stop$default(this.session, null, 1, null);
                                Unit unit2 = Unit.INSTANCE;
                            }
                            function1 = this.diagnostics;
                            sb = new StringBuilder();
                            break;
                        }
                        try {
                            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                            udp.receive(datagram);
                            UdpControlPacket.Companion companion = UdpControlPacket.INSTANCE;
                            byte[] data = datagram.getData();
                            Intrinsics.checkNotNullExpressionValue(data, "datagram.data");
                            byte[] bArrCopyOf = Arrays.copyOf(data, datagram.getLength());
                            Intrinsics.checkNotNullExpressionValue(bArrCopyOf, "copyOf(this, newSize)");
                            UdpControlPacket p = companion.decode(bArrCopyOf);
                            synchronized (this.lock) {
                                if (p != null) {
                                    try {
                                        z = this.session.receive(p, invoke().longValue());
                                    } catch (Throwable th) {
                                        throw th;
                                    }
                                }
                            }
                            boolean accepted = z;
                            invoke("received seq=" + (p != null ? Long.valueOf(p.getSequence()) : null) + " accepted=" + accepted);
                        } catch (SocketTimeoutException e) {
                        }
                    }
                } catch (Throwable th2) {
                    try {
                        throw th2;
                    } catch (Throwable th3) {
                        CloseableKt.closeFinally(datagramSocket, th2);
                        throw th3;
                    }
                }
            } catch (Exception e2) {
                synchronized (this.lock) {
                    this.session.stop("Transport failure: " + e2.getClass().getSimpleName());
                    Unit unit3 = Unit.INSTANCE;
                    invoke("transport failure " + e2.getClass().getSimpleName());
                    this.socket = null;
                    synchronized (this.lock) {
                        UdpControlSession.stop$default(this.session, null, 1, null);
                        Unit unit4 = Unit.INSTANCE;
                        function1 = this.diagnostics;
                        sb = new StringBuilder();
                    }
                }
            }
            function1.invoke(sb.append("closed ").append(snapshot().getReason()).toString());
        } catch (Throwable th4) {
            this.socket = null;
            synchronized (this.lock) {
                UdpControlSession.stop$default(this.session, null, 1, null);
                Unit unit5 = Unit.INSTANCE;
                invoke("closed " + snapshot().getReason());
                throw th4;
            }
        }
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws InterruptedException {
        DatagramSocket datagramSocket;
        this.closing.set(true);
        synchronized (this.lock) {
            this.session.input(0, 0);
            Unit unit = Unit.INSTANCE;
        }
        if (!Intrinsics.areEqual(Thread.currentThread(), this.worker)) {
            this.worker.join(300);
            if (this.worker.isAlive() && (datagramSocket = this.socket) != null) {
                datagramSocket.close();
            }
            this.worker.join(100);
        }
    }
}
