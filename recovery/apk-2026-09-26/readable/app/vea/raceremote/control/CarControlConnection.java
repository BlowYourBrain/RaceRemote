package vea.raceremote.control;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt__Builders_commonKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.DelayKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.SupervisorKt;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import okhttp3.OkHttpClient;

/* JADX INFO: compiled from: CarControlConnection.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class CarControlConnection {
    public static final int $stable = 8;
    private boolean closed;
    private final Function1<String, Unit> diagnostics;
    private long generation;
    private Job job;
    private final MutableStateFlow<CarConnectionState> mutableState;
    private final Function3<String, OkHttpClient, Integer, ControlEndpoint> resolve;
    private final CoroutineScope scope;
    private final StateFlow<CarConnectionState> state;
    private UdpControlClient udp;
    private CarControlClient websocket;

    /* JADX WARN: Multi-variable type inference failed */
    public CarControlConnection() {
        this(null, 0 == true ? 1 : 0, 3);
    }

    /* JADX INFO: renamed from: vea.raceremote.control.CarControlConnection$1, reason: invalid class name */
    /* JADX INFO: compiled from: CarControlConnection.kt */
    /* synthetic */ class AnonymousClass1 extends FunctionReferenceImpl implements Function3<String, OkHttpClient, Integer, ControlEndpoint> {
        AnonymousClass1(Object obj) {
            super(3, obj, ControlEndpointProbe.class, "load", "load(Ljava/lang/String;Lokhttp3/OkHttpClient;I)Lvea/raceremote/control/ControlEndpoint;", 0);
        }

        @Override // kotlin.jvm.functions.Function3
        public /* bridge */ /* synthetic */ ControlEndpoint invoke(String str, OkHttpClient okHttpClient, Integer num) {
            return invoke(str, okHttpClient, num.intValue());
        }

        public final ControlEndpoint invoke(String p0, OkHttpClient p1, int p2) {
            Intrinsics.checkNotNullParameter(p0, "p0");
            Intrinsics.checkNotNullParameter(p1, "p1");
            return ((ControlEndpointProbe) this.receiver).load(p0, p1, p2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public CarControlConnection(Function3<? super String, ? super OkHttpClient, ? super Integer, ControlEndpoint> function3, Function1<? super String, Unit> function1) {
        Intrinsics.checkNotNullParameter(function3, "resolve");
        Intrinsics.checkNotNullParameter(function1, "diagnostics");
        this.resolve = function3;
        this.diagnostics = function1;
        MutableStateFlow<CarConnectionState> MutableStateFlow = StateFlowKt.MutableStateFlow(new CarConnectionState(null, false, false, 0, 0, 31));
        this.mutableState = MutableStateFlow;
        this.state = FlowKt.asStateFlow(MutableStateFlow);
        this.scope = CoroutineScopeKt.CoroutineScope(SupervisorKt.SupervisorJob$default((Job) null, 1, (Object) null).plus(Dispatchers.getIO()));
    }

    public final StateFlow<CarConnectionState> getState() {
        return this.state;
    }

    /* JADX WARN: Code duplicated, block: B:71:0x012e  */
    /* JADX WARN: Code duplicated, block: B:76:0x0141  */
    /* JADX WARN: Code duplicated, block: B:79:0x0147 A[Catch: all -> 0x01be, TRY_LEAVE, TryCatch #0 {, blocks: (B:4:0x0007, B:9:0x0022, B:10:0x0051, B:12:0x0057, B:13:0x0069, B:15:0x0076, B:17:0x007e, B:34:0x00b9, B:40:0x00c9, B:46:0x00db, B:58:0x0101, B:83:0x01a4, B:61:0x010a, B:63:0x0112, B:72:0x012f, B:79:0x0147, B:49:0x00e4, B:51:0x00ec, B:54:0x00f6, B:43:0x00d2, B:37:0x00c2, B:20:0x0089, B:21:0x008d, B:23:0x0093, B:25:0x009d), top: B:89:0x0007 }] */
    /* JADX WARN: Code duplicated, block: B:82:0x01a2  */
    public final synchronized void connect(String host, OkHttpClient http, Function1<? super DatagramSocket, Unit> bindUdp, Integer expectedPort, Long expectedChipId, int statusPort, String expectedTransport) {
        boolean z;
        boolean z2;
        boolean z3;
        Integer num;
        Intrinsics.checkNotNullParameter(host, "host");
        Intrinsics.checkNotNullParameter(http, "http");
        Intrinsics.checkNotNullParameter(bindUdp, "bindUdp");
        if (this.closed) {
            return;
        }
        disconnect$default(this, null, 1, null);
        Iterable $this$map$iv = StringsKt.split$default((CharSequence) host, new char[]{'.'}, false, 0, 6, (Object) null);
        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        for (Object item$iv$iv : $this$map$iv) {
            destination$iv$iv.add(StringsKt.toIntOrNull((String) item$iv$iv));
        }
        List octets = (List) destination$iv$iv;
        if (octets.size() == 4) {
            List $this$any$iv = octets;
            if (!($this$any$iv instanceof Collection) || !$this$any$iv.isEmpty()) {
                Iterator it = $this$any$iv.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        z = false;
                        break;
                    }
                    Object element$iv = it.next();
                    Integer it2 = (Integer) element$iv;
                    if (it2 == null || !new IntRange(0, 255).contains(it2.intValue())) {
                        z = true;
                        break;
                    }
                }
            } else {
                z = false;
            }
            if (!z) {
                Integer num2 = (Integer) octets.get(0);
                if (num2 != null && num2.intValue() == 10) {
                    z2 = true;
                    int iM6126Int$$this$callrangeTo$$this$callcontains$$this$callnot$else$cond$if1$funconnect$classCarControlConnection = 1;
                    if (statusPort <= 65535 || iM6126Int$$this$callrangeTo$$this$callcontains$$this$callnot$else$cond$if1$funconnect$classCarControlConnection > statusPort) {
                        z3 = false;
                    } else {
                        z3 = z2;
                    }
                    if (z3) {
                        long current = this.generation;
                        setValue(new CarConnectionState("Проверка машинки…", false, true, 0, 0, 26));
                        this.job = BuildersKt__Builders_commonKt.launch$default(this.scope, null, null, new C05262(host, http, statusPort, current, expectedChipId, expectedPort, expectedTransport, bindUdp, null), 3, null);
                        return;
                    }
                } else {
                    Integer num3 = (Integer) octets.get(0);
                    if (num3 != null && num3.intValue() == 127) {
                        z2 = true;
                    } else {
                        Integer num4 = (Integer) octets.get(0);
                        if (num4 != null && num4.intValue() == 192 && (num = (Integer) octets.get(1)) != null && num.intValue() == 168) {
                            z2 = true;
                        } else {
                            Integer num5 = (Integer) octets.get(0);
                            if (num5 != null && num5.intValue() == 172) {
                                z2 = true;
                                Object obj = octets.get(1);
                                Intrinsics.checkNotNull(obj);
                                int iIntValue = ((Number) obj).intValue();
                                if (16 <= iIntValue && iIntValue < 32) {
                                }
                            }
                        }
                    }
                    int iM6126Int$$this$callrangeTo$$this$callcontains$$this$callnot$else$cond$if1$funconnect$classCarControlConnection2 = 1;
                    if (statusPort <= 65535) {
                        z3 = false;
                    } else {
                        z3 = false;
                    }
                    if (z3) {
                        long current2 = this.generation;
                        setValue(new CarConnectionState("Проверка машинки…", false, true, 0, 0, 26));
                        this.job = BuildersKt__Builders_commonKt.launch$default(this.scope, null, null, new C05262(host, http, statusPort, current2, expectedChipId, expectedPort, expectedTransport, bindUdp, null), 3, null);
                        return;
                    }
                }
            }
        }
        setValue(new CarConnectionState("Укажи локальный IPv4-адрес машинки", false, false, 0, 0, 30));
    }

    /* JADX INFO: renamed from: vea.raceremote.control.CarControlConnection$connect$2, reason: invalid class name and case insensitive filesystem */
    /* JADX INFO: compiled from: CarControlConnection.kt */
    @DebugMetadata(c = "vea.raceremote.control.CarControlConnection$connect$2", f = "CarControlConnection.kt", i = {0}, l = {88}, m = "invokeSuspend", n = {"$this$launch"}, s = {"L$0"})
    static final class C05262 extends SuspendLambda implements Function2<CoroutineScope, Continuation<? super Unit>, Object> {
        final /* synthetic */ Function1<DatagramSocket, Unit> $bindUdp;
        final /* synthetic */ long $current;
        final /* synthetic */ Long $expectedChipId;
        final /* synthetic */ Integer $expectedPort;
        final /* synthetic */ String $expectedTransport;
        final /* synthetic */ String $host;
        final /* synthetic */ OkHttpClient $http;
        final /* synthetic */ int $statusPort;
        private /* synthetic */ Object L$0;
        int label;

        /* JADX INFO: renamed from: vea.raceremote.control.CarControlConnection$connect$2$WhenMappings */
        /* JADX INFO: compiled from: CarControlConnection.kt */
        public /* synthetic */ class WhenMappings {
            public static final /* synthetic */ int[] $EnumSwitchMapping$0;

            static {
                int[] iArr = new int[UdpControlSession.Phase.values().length];
                try {
                    iArr[UdpControlSession.Phase.OPENING.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[UdpControlSession.Phase.ARMING.ordinal()] = 2;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[UdpControlSession.Phase.ACTIVE.ordinal()] = 3;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[UdpControlSession.Phase.CLOSED.ordinal()] = 4;
                } catch (NoSuchFieldError e4) {
                }
                $EnumSwitchMapping$0 = iArr;
            }
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        /* JADX WARN: Multi-variable type inference failed */
        C05262(String str, OkHttpClient okHttpClient, int i, long j, Long l, Integer num, String str2, Function1<? super DatagramSocket, Unit> function1, Continuation<? super C05262> continuation) {
            super(2, continuation);
            this.$host = str;
            this.$http = okHttpClient;
            this.$statusPort = i;
            this.$current = j;
            this.$expectedChipId = l;
            this.$expectedPort = num;
            this.$expectedTransport = str2;
            this.$bindUdp = function1;
        }

        @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
        public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {
            C05262 c05262 = CarControlConnection.this.new C05262(this.$host, this.$http, this.$statusPort, this.$current, this.$expectedChipId, this.$expectedPort, this.$expectedTransport, this.$bindUdp, continuation);
            c05262.L$0 = obj;
            return c05262;
        }

        @Override // kotlin.jvm.functions.Function2
        public final Object invoke(CoroutineScope coroutineScope, Continuation<? super Unit> continuation) {
            return ((C05262) create(coroutineScope, continuation)).invokeSuspend(Unit.INSTANCE);
        }

        @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
        public final Object invokeSuspend(Object $result) {
            C05262 c05262;
            CoroutineScope $this$launch;
            UdpControlSession.Snapshot snapshot;
            boolean zM6114Boolean$branch$when$cond$when$fun$anonymous$$arg0$calllet$else$when$branch$when$valtmp5_elvis_lhs$valvalue$fun$anonymous$$arg1$callsynchronized$body$loop$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection;
            CarConnectionState value;
            StateFlow<CarConnectionState> state;
            Object coroutine_suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();
            try {
                switch (this.label) {
                    case 0:
                        ResultKt.throwOnFailure($result);
                        c05262 = this;
                        $this$launch = (CoroutineScope) c05262.L$0;
                        ControlEndpoint endpoint = (ControlEndpoint) CarControlConnection.this.resolve.invoke(c05262.$host, c05262.$http, Boxing.boxInt(c05262.$statusPort));
                        CarControlConnection carControlConnection = CarControlConnection.this;
                        long j = c05262.$current;
                        Long l = c05262.$expectedChipId;
                        Integer num = c05262.$expectedPort;
                        String str = c05262.$expectedTransport;
                        String str2 = c05262.$host;
                        Function1<DatagramSocket, Unit> function1 = c05262.$bindUdp;
                        OkHttpClient okHttpClient = c05262.$http;
                        synchronized (carControlConnection) {
                            if (j == carControlConnection.generation && !carControlConnection.closed) {
                                int iM6127Int$$this$callrangeTo$$this$callcontains$arg0$callrequire$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = 1;
                                long jM6139Long$arg0$callrangeTo$$this$callcontains$arg0$callrequire$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = 281474976710655L;
                                long chipId = endpoint.getChipId();
                                if (!(((long) iM6127Int$$this$callrangeTo$$this$callcontains$arg0$callrequire$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection) <= chipId && chipId <= jM6139Long$arg0$callrangeTo$$this$callcontains$arg0$callrequire$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection)) {
                                    throw new IllegalArgumentException("Failed requirement.".toString());
                                }
                                int iM6128Int$$this$callrangeTo$$this$callcontains$arg0$callrequire1$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = 1;
                                int iM6130Int$arg0$callrangeTo$$this$callcontains$arg0$callrequire1$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = 65535;
                                int port = endpoint.getPort();
                                if (!(iM6128Int$$this$callrangeTo$$this$callcontains$arg0$callrequire1$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection <= port && port <= iM6130Int$arg0$callrangeTo$$this$callcontains$arg0$callrequire1$fun$anonymous$$arg1$callsynchronized$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection)) {
                                    throw new IllegalArgumentException("Failed requirement.".toString());
                                }
                                if (l != null && endpoint.getChipId() != l.longValue()) {
                                    carControlConnection.disconnect("По этому адресу другая машинка");
                                    return Unit.INSTANCE;
                                }
                                if (num != null && endpoint.getPort() != num.intValue()) {
                                    carControlConnection.disconnect("Настройки машинки изменились — выбери её заново");
                                    return Unit.INSTANCE;
                                }
                                if (str != null && !Intrinsics.areEqual(endpoint.getTransport(), str)) {
                                    carControlConnection.disconnect("Способ управления изменился — выбери машинку заново");
                                    return Unit.INSTANCE;
                                }
                                String transport = endpoint.getTransport();
                                if (Intrinsics.areEqual(transport, "udp-v1")) {
                                    if (endpoint.getPort() == 3334) {
                                        carControlConnection.udp = new UdpControlClient(str2, endpoint.getChipId(), function1, carControlConnection.diagnostics, null, 16, null);
                                    } else {
                                        throw new IllegalArgumentException("Failed requirement.".toString());
                                    }
                                } else if (Intrinsics.areEqual(transport, "websocket-v1")) {
                                    CarControlClient it = new CarControlClient(carControlConnection.diagnostics, false, null, 6);
                                    it.connect(str2, okHttpClient, endpoint.getPort(), Boxing.boxLong(endpoint.getChipId()));
                                    carControlConnection.websocket = it;
                                } else {
                                    carControlConnection.disconnect("Версия управления не поддерживается");
                                    return Unit.INSTANCE;
                                }
                                Unit unit = Unit.INSTANCE;
                                break;
                            }
                            return Unit.INSTANCE;
                        }
                    case 1:
                        c05262 = this;
                        $this$launch = (CoroutineScope) c05262.L$0;
                        ResultKt.throwOnFailure($result);
                        break;
                    default:
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }
                while (CoroutineScopeKt.isActive($this$launch)) {
                    CarControlConnection carControlConnection2 = CarControlConnection.this;
                    long j2 = c05262.$current;
                    synchronized (carControlConnection2) {
                        if (j2 == carControlConnection2.generation && !carControlConnection2.closed) {
                            CarControlClient carControlClient = carControlConnection2.websocket;
                            if (carControlClient == null || (state = carControlClient.getState()) == null || (value = state.getValue()) == null) {
                                UdpControlClient udpControlClient = carControlConnection2.udp;
                                if (udpControlClient != null && (snapshot = udpControlClient.snapshot()) != null) {
                                    int i = WhenMappings.$EnumSwitchMapping$0[snapshot.getPhase().ordinal()];
                                    if (i == 1) {
                                        zM6114Boolean$branch$when$cond$when$fun$anonymous$$arg0$calllet$else$when$branch$when$valtmp5_elvis_lhs$valvalue$fun$anonymous$$arg1$callsynchronized$body$loop$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = true;
                                    } else {
                                        zM6114Boolean$branch$when$cond$when$fun$anonymous$$arg0$calllet$else$when$branch$when$valtmp5_elvis_lhs$valvalue$fun$anonymous$$arg1$callsynchronized$body$loop$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection = i == 2;
                                    }
                                    if (zM6114Boolean$branch$when$cond$when$fun$anonymous$$arg0$calllet$else$when$branch$when$valtmp5_elvis_lhs$valvalue$fun$anonymous$$arg1$callsynchronized$body$loop$try$fun$anonymous$$arg2$calllaunch$arg0$call$setjob$$funconnect$classCarControlConnection) {
                                        value = new CarConnectionState("Подключение…", false, true, 0, 0, 26);
                                    } else if (i == 3) {
                                        value = new CarConnectionState("Управление подключено", true, false, snapshot.getAcknowledgedThrottle(), snapshot.getAcknowledgedSteering(), 4);
                                    } else {
                                        if (i != 4) {
                                            throw new NoWhenBranchMatchedException();
                                        }
                                        value = new CarConnectionState("Нет свежего ответа — остановлено", false, false, 0, 0, 30);
                                    }
                                } else {
                                    return Unit.INSTANCE;
                                }
                            }
                            carControlConnection2.mutableState.setValue(value);
                            if (!value.getConnected() && !value.getConnecting()) {
                                carControlConnection2.disconnect(value.getMessage());
                                return Unit.INSTANCE;
                            }
                            Unit unit2 = Unit.INSTANCE;
                            c05262.L$0 = $this$launch;
                            c05262.label = 1;
                            if (DelayKt.delay(20, c05262) == coroutine_suspended) {
                                return coroutine_suspended;
                            }
                        }
                        return Unit.INSTANCE;
                    }
                }
            } catch (CancellationException error) {
                throw error;
            } catch (Exception error2) {
                CarControlConnection carControlConnection3 = CarControlConnection.this;
                long j3 = this.$current;
                synchronized (carControlConnection3) {
                    if (j3 == carControlConnection3.generation && !carControlConnection3.closed) {
                        carControlConnection3.diagnostics.invoke("Connection failed: " + error2.getClass().getSimpleName());
                        carControlConnection3.disconnect("Не удалось проверить машинку — остановлено");
                    }
                    Unit unit3 = Unit.INSTANCE;
                }
            }
            return Unit.INSTANCE;
        }
    }

    public final synchronized void setInput(float throttle, float steering) {
        boolean z = true;
        if ((Float.isInfinite(throttle) || Float.isNaN(throttle)) ? false : true) {
            if (Float.isInfinite(steering) || Float.isNaN(steering)) {
                z = false;
            }
            if (z) {
                if (getValue().getConnected()) {
                    UdpControlClient udpControlClient = this.udp;
                    if (udpControlClient != null) {
                        udpControlClient.input((int) (RangesKt.coerceIn(throttle, -1.0f, 1.0f) * 1000), (int) (RangesKt.coerceIn(steering, -1.0f, 1.0f) * 1000));
                    }
                    CarControlClient carControlClient = this.websocket;
                    if (carControlClient != null) {
                        carControlClient.setInput(throttle, steering);
                    }
                    return;
                }
                return;
            }
        }
        disconnect("Некорректное управление");
    }

    public static /* synthetic */ void disconnect$default(CarControlConnection carControlConnection, String str, int i, Object obj) {
        if ((i & 1) != 0) {
            str = "Остановлено";
        }
        carControlConnection.disconnect(str);
    }

    public final synchronized void disconnect(String message) {
        Intrinsics.checkNotNullParameter(message, "message");
        this.generation++;
        setValue(new CarConnectionState(message, false, false, 0, 0, 30));
        Job job = this.job;
        if (job != null) {
            Job.DefaultImpls.cancel$default(job, (CancellationException) null, 1, (Object) null);
        }
        this.job = null;
        UdpControlClient udpControlClient = this.udp;
        if (udpControlClient != null) {
            udpControlClient.close();
        }
        this.udp = null;
        CarControlClient carControlClient = this.websocket;
        if (carControlClient != null) {
            carControlClient.close();
        }
        this.websocket = null;
    }

    public final synchronized void close() {
        this.closed = true;
        disconnect$default(this, null, 1, null);
        CoroutineScopeKt.cancel$default(this.scope, null, 1, null);
    }
}
