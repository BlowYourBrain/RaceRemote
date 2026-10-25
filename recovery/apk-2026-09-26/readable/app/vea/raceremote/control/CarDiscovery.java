package vea.raceremote.control;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/* JADX INFO: compiled from: CarDiscovery.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class CarDiscovery {
    private long generation;
    private final MutableStateFlow<DiscoveryState> mutableState;
    private DatagramSocket socket;
    private final StateFlow<DiscoveryState> state;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion();
    public static final int $stable = 8;

    public CarDiscovery() {
        MutableStateFlow<DiscoveryState> MutableStateFlow = StateFlowKt.MutableStateFlow(new DiscoveryState(false, null, null, 7));
        this.mutableState = MutableStateFlow;
        this.state = FlowKt.asStateFlow(MutableStateFlow);
    }

    public final StateFlow<DiscoveryState> getState() {
        return this.state;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void search$default(CarDiscovery carDiscovery, List list, Function1 function1, int i, Object obj) {
        if ((i & 1) != 0) {
            list = INSTANCE.broadcastTargets();
        }
        if ((i & 2) != 0) {
            function1 = new Function1<DatagramSocket, Unit>() { // from class: vea.raceremote.control.CarDiscovery.search.1
                @Override // kotlin.jvm.functions.Function1
                public /* bridge */ /* synthetic */ Unit invoke(DatagramSocket datagramSocket) {
                    invoke2(datagramSocket);
                    return Unit.INSTANCE;
                }

                /* JADX INFO: renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2(DatagramSocket it) {
                    Intrinsics.checkNotNullParameter(it, "it");
                }
            };
        }
        carDiscovery.search(list, function1);
    }

    public final synchronized void search(final List<? extends InetAddress> targets, final Function1<? super DatagramSocket, Unit> configure) {
        Intrinsics.checkNotNullParameter(targets, "targets");
        Intrinsics.checkNotNullParameter(configure, "configure");
        cancel();
        if (targets.isEmpty()) {
            setValue(new DiscoveryState(false, null, "Нет доступной локальной сети", 3));
            return;
        }
        final long current = this.generation;
        setValue(new DiscoveryState(true, null, "Ищу машинки…", 2));
        this.worker.execute(new Runnable() { // from class: vea.raceremote.control.CarDiscovery$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CarDiscovery.search$lambda$8(this.f$0, configure, targets, current);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x0257, code lost:

        r2 = r0.values();
        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r2, "found.values");
        r12.setValue(new vea.raceremote.control.DiscoveryState(r0, kotlin.collections.CollectionsKt.toList(r2), vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6284String$arg2$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized1$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery()));
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x0277, code lost:

        r0 = kotlin.Unit.INSTANCE;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x027a, code lost:

        monitor-exit(r26);
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x027c, code lost:

        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x027e, code lost:

        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x0281, code lost:

        monitor-exit(r26);
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x0282, code lost:

        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00ec, code lost:

        r18 = (java.lang.System.nanoTime() - r12) / ((long) vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6262Int$arg0$calldiv$valelapsed$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery());
        r18 = r18 - r14;
        r20 = r12;
        r12 = vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6260Int$$this$calltoLong$arg1$callgreaterOrEqual$cond$if$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery();
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x010a, code lost:

        if (r18 < r12) goto L71;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x010c, code lost:

        r12 = r28.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x0114, code lost:

        if (r12.hasNext() == false) goto L248;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x0116, code lost:

        r0 = (java.net.InetAddress) r12.next();
        r16 = r11.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x0172, code lost:

        r22 = r8;
        r23 = r11;
        r14 = r18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x0179, code lost:

        r22 = r8;
        r23 = r11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x017d, code lost:

        r0 = new java.net.DatagramPacket(new byte[vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6261Int$arg0$call$init$$arg0$call$init$$valpacket$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery()], vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6266Int$arg1$call$init$$valpacket$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery());
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x0191, code lost:

        r0.receive(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:76:0x019a, code lost:

        if (r0.getPort() != 3333) goto L113;
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x019c, code lost:

        r0 = vea.raceremote.control.CarDiscovery.INSTANCE;
        r8 = r0.getAddress();
        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r8, "packet.address");
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x01ab, code lost:

        if (r0.localAddress(r8) != false) goto L80;
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x01af, code lost:

        r0 = vea.raceremote.control.DiscoveryPacket.INSTANCE;
        r8 = r0.getData();
        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r8, "packet.data");
        r8 = java.util.Arrays.copyOf(r8, r0.getLength());
        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r8, "copyOf(this, newSize)");
        r11 = r0.getAddress().getHostAddress();
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x01cf, code lost:

        if (r11 != null) goto L83;
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x01d3, code lost:

        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r11, "packet.address.hostAddress ?: continue");
        r0 = r0.reply(r8, r9, r11);
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01dc, code lost:

        if (r0 != null) goto L86;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01df, code lost:

        r0 = r0.getChipId() + vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6277String$1$str$valkey$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery() + r0.getHost();
        r0 = (vea.raceremote.control.DiscoveredCar) r0.get(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x020a, code lost:

        if (r0 == null) goto L89;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x020c, code lost:

        r0 = r0.getDiscoveryVersion();
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x0211, code lost:

        r0 = vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6269Int$branch$when$arg0$callgreater$cond$if2$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery();
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x021b, code lost:

        if (r0 > r0.getDiscoveryVersion()) goto L112;
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x021d, code lost:

        r0.put(r0, r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x022d, code lost:

        if (r0.size() <= vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6267Int$arg1$callgreater$cond$if3$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery()) goto L95;
     */
    /* JADX WARN: Code restructure failed: missing block: B:94:0x022f, code lost:

        r0 = r0.keySet();
        kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, "found.keys");
        r0.remove(kotlin.collections.CollectionsKt.first(r0));
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0241, code lost:

        monitor-enter(r26);
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x0247, code lost:

        if (r26.generation != r29) goto L102;
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x0249, code lost:

        r12 = r26.mutableState;
        r0 = vea.raceremote.control.LiveLiterals$CarDiscoveryKt.INSTANCE.m6249Boolean$arg0$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized1$body$loop1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static final void search$lambda$8(CarDiscovery this$0, Function1 $configure, List $targets, long $current) {
        Throwable th;
        long nonce;
        List<byte[]> queries;
        DatagramSocket datagramSocket;
        long started;
        int i;
        List queries2;
        InetAddress target;
        int i2;
        List queries3;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($configure, "$configure");
        Intrinsics.checkNotNullParameter($targets, "$targets");
        LinkedHashMap found = new LinkedHashMap();
        DatagramSocket datagramSocket2 = null;
        try {
            try {
                DatagramSocket datagramSocket3 = new DatagramSocket();
                try {
                    DatagramSocket udp = datagramSocket3;
                    int i3 = 0;
                    synchronized (this$0) {
                        try {
                            if (this$0.generation != $current) {
                                try {
                                    CloseableKt.closeFinally(datagramSocket3, null);
                                    synchronized (this$0) {
                                        if (this$0.generation == $current) {
                                            this$0.socket = null;
                                        }
                                    }
                                    return;
                                } catch (Throwable th2) {
                                    th = th2;
                                    datagramSocket2 = datagramSocket3;
                                }
                            } else {
                                this$0.socket = udp;
                                Unit unit = Unit.INSTANCE;
                                $configure.invoke(udp);
                                udp.setBroadcast(true);
                                udp.setSoTimeout(100);
                                do {
                                    nonce = ((long) new SecureRandom().nextInt()) & 4294967295L;
                                } while (nonce == 0L);
                                queries = CollectionsKt.listOf((Object[]) new byte[][]{DiscoveryPacket.query$default(DiscoveryPacket.INSTANCE, nonce, 0, 2, null), DiscoveryPacket.INSTANCE.query(nonce, 2)});
                                long started2 = System.nanoTime();
                                long lastSend = -500;
                                while (true) {
                                    if (System.nanoTime() - started2 < 3000000000L) {
                                        synchronized (this$0) {
                                            datagramSocket = datagramSocket3;
                                            try {
                                                if (this$0.generation != $current) {
                                                    try {
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        datagramSocket2 = datagramSocket;
                                                    }
                                                } else {
                                                    try {
                                                        Unit unit2 = Unit.INSTANCE;
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        throw th;
                                                    }
                                                }
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        }
                                        CloseableKt.closeFinally(datagramSocket, null);
                                        synchronized (this$0) {
                                            if (this$0.generation != $current) {
                                                break;
                                            }
                                            this$0.socket = null;
                                            break;
                                        }
                                    }
                                    datagramSocket2 = datagramSocket3;
                                    synchronized (this$0) {
                                        if (this$0.generation == $current) {
                                            MutableStateFlow<DiscoveryState> mutableStateFlow = this$0.mutableState;
                                            boolean zM6250Boolean$arg0$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery = false;
                                            Collection collectionValues = found.values();
                                            Intrinsics.checkNotNullExpressionValue(collectionValues, "found.values");
                                            mutableStateFlow.setValue(new DiscoveryState(zM6250Boolean$arg0$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized1$fun$anonymous$$arg0$calluse$try$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery, CollectionsKt.toList(collectionValues), found.isEmpty() ? "Машинки не найдены. Можно ввести адрес вручную." : "Выбери машинку"));
                                        }
                                        Unit unit3 = Unit.INSTANCE;
                                    }
                                    Unit unit4 = Unit.INSTANCE;
                                    CloseableKt.closeFinally(datagramSocket2, null);
                                    synchronized (this$0) {
                                        if (this$0.generation == $current) {
                                            this$0.socket = null;
                                        }
                                        Unit unit5 = Unit.INSTANCE;
                                    }
                                    return;
                                    th = th;
                                    try {
                                        throw th;
                                    } catch (Throwable th6) {
                                        CloseableKt.closeFinally(datagramSocket2, th);
                                        throw th6;
                                    }
                                    datagramSocket3 = datagramSocket;
                                    started2 = started;
                                    i3 = i;
                                    queries = queries2;
                                }
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    }
                    Unit unit6 = Unit.INSTANCE;
                    return;
                    for (byte[] query : queries) {
                        try {
                            Result.Companion companion = Result.INSTANCE;
                            i2 = i3;
                            try {
                                queries3 = queries;
                                try {
                                    udp.send(new DatagramPacket(query, query.length, target, 3333));
                                    Result.m4399constructorimpl(Unit.INSTANCE);
                                    i3 = i2;
                                    queries = queries3;
                                } catch (Throwable th8) {
                                    th = th8;
                                    Result.Companion companion2 = Result.INSTANCE;
                                    Result.m4399constructorimpl(ResultKt.createFailure(th));
                                    i3 = i2;
                                    queries = queries3;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                queries3 = queries;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            i2 = i3;
                            queries3 = queries;
                        }
                    }
                } catch (Throwable th11) {
                    th = th11;
                    datagramSocket2 = datagramSocket3;
                }
            } catch (Exception e) {
                synchronized (this$0) {
                    if (this$0.generation == $current) {
                        MutableStateFlow<DiscoveryState> mutableStateFlow2 = this$0.mutableState;
                        boolean zM6248Boolean$arg0$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized$catch$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery = false;
                        Collection collectionValues2 = found.values();
                        Intrinsics.checkNotNullExpressionValue(collectionValues2, "found.values");
                        mutableStateFlow2.setValue(new DiscoveryState(zM6248Boolean$arg0$call$init$$arg0$call$setvalue$$branch$if$fun$anonymous$$arg1$callsynchronized$catch$fun$anonymous$$arg0$callexecute$funsearch$classCarDiscovery, CollectionsKt.toList(collectionValues2), "Не удалось выполнить поиск"));
                    }
                    Unit unit7 = Unit.INSTANCE;
                    synchronized (this$0) {
                        if (this$0.generation == $current) {
                            this$0.socket = null;
                        }
                        Unit unit8 = Unit.INSTANCE;
                    }
                }
            }
        } catch (Throwable th12) {
            synchronized (this$0) {
                if (this$0.generation == $current) {
                    this$0.socket = null;
                }
                Unit unit9 = Unit.INSTANCE;
                throw th12;
            }
        }
    }

    public final synchronized void cancel() {
        this.generation++;
        DatagramSocket datagramSocket = this.socket;
        if (datagramSocket != null) {
            datagramSocket.close();
        }
        this.socket = null;
        setValue(new DiscoveryState(false, null, null, 7));
    }

    public final synchronized void close() {
        cancel();
        this.worker.shutdownNow();
    }

    /* JADX INFO: compiled from: CarDiscovery.kt */
    public static final class Companion {
        private Companion() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public final boolean localAddress(InetAddress address) {
            return (address instanceof Inet4Address) && (((Inet4Address) address).isSiteLocalAddress() || ((Inet4Address) address).isLoopbackAddress());
        }

        public final List<InetAddress> broadcastTargets() {
            Object objM4399constructorimpl;
            try {
                Result.Companion companion = Result.INSTANCE;
                Companion $this$broadcastTargets_u24lambda_u244 = this;
                Iterable list = Collections.list(NetworkInterface.getNetworkInterfaces());
                Intrinsics.checkNotNullExpressionValue(list, "list(NetworkInterface.getNetworkInterfaces())");
                Iterable $this$filter$iv = list;
                Collection destination$iv$iv = new ArrayList();
                for (Object element$iv$iv : $this$filter$iv) {
                    NetworkInterface it = (NetworkInterface) element$iv$iv;
                    if (it.isUp() && !it.isLoopback()) {
                        destination$iv$iv.add(element$iv$iv);
                    }
                }
                Iterable<NetworkInterface> $this$flatMap$iv = (List) destination$iv$iv;
                Collection destination$iv$iv2 = new ArrayList();
                for (NetworkInterface it2 : $this$flatMap$iv) {
                    Iterable interfaceAddresses = it2.getInterfaceAddresses();
                    Intrinsics.checkNotNullExpressionValue(interfaceAddresses, "it.interfaceAddresses");
                    Iterable list$iv$iv = interfaceAddresses;
                    CollectionsKt.addAll(destination$iv$iv2, list$iv$iv);
                }
                Iterable $this$filter$iv2 = (List) destination$iv$iv2;
                Collection destination$iv$iv3 = new ArrayList();
                for (Object element$iv$iv2 : $this$filter$iv2) {
                    InterfaceAddress it3 = (InterfaceAddress) element$iv$iv2;
                    InetAddress address = it3.getAddress();
                    Intrinsics.checkNotNullExpressionValue(address, "it.address");
                    if ($this$broadcastTargets_u24lambda_u244.localAddress(address)) {
                        destination$iv$iv3.add(element$iv$iv2);
                    }
                }
                Iterable $this$mapNotNull$iv = (List) destination$iv$iv3;
                Collection destination$iv$iv4 = new ArrayList();
                for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
                    InterfaceAddress it4 = (InterfaceAddress) element$iv$iv$iv;
                    InetAddress broadcast = it4.getBroadcast();
                    if (broadcast != null) {
                        destination$iv$iv4.add(broadcast);
                    }
                }
                objM4399constructorimpl = Result.m4399constructorimpl(CollectionsKt.distinct((List) destination$iv$iv4));
            } catch (Throwable th) {
                Result.Companion companion2 = Result.INSTANCE;
                objM4399constructorimpl = Result.m4399constructorimpl(ResultKt.createFailure(th));
            }
            List listEmptyList = CollectionsKt.emptyList();
            if (Result.m4405isFailureimpl(objM4399constructorimpl)) {
                objM4399constructorimpl = listEmptyList;
            }
            return (List) objM4399constructorimpl;
        }
    }
}
