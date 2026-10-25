package vea.raceremote;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.ranges.IntRange;
import kotlin.text.CharsKt;
import kotlin.text.StringsKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import vea.raceremote.app.MainActivity;
import vea.raceremote.control.UdpControlClient;
import vea.raceremote.control.UdpControlPacket;
import vea.raceremote.control.UdpControlSession;

/* JADX INFO: compiled from: UdpControlDeviceBenchTest.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class UdpControlDeviceBenchTest {
    public static final int $stable = 0;

    @Test
    public final void driveAndStop() throws JSONException {
        bench("drive");
    }

    @Test
    public final void sixNeutralSessions() throws JSONException {
        bench("link");
    }

    @Test
    public final void sustainedNeutralSession() throws JSONException {
        bench("sustained");
    }

    @Test
    public final void silenceExpiresOnBoard() throws JSONException {
        bench("watchdog");
    }

    @Test
    public final void wifiLockComparison() throws JSONException {
        bench("wifi-mode");
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x09ee */
    /* JADX WARN: Bottom block not found for handler: all -> 0x0b42 */
    /* JADX WARN: Code duplicated, block: B:216:0x07a6 A[Catch: all -> 0x08e3, TRY_LEAVE, TryCatch #31 {all -> 0x08e3, blocks: (B:214:0x078d, B:216:0x07a6), top: B:522:0x078d }] */
    /* JADX WARN: Code duplicated, block: B:251:0x08c7  */
    /* JADX WARN: Code duplicated, block: B:278:0x09e8 A[Catch: all -> 0x09ee, TRY_ENTER, TRY_LEAVE, TryCatch #35 {, blocks: (B:227:0x0863, B:245:0x0890, B:246:0x0893, B:278:0x09e8, B:242:0x088d), top: B:530:0x0863, inners: #56 }] */
    /* JADX WARN: Code duplicated, block: B:287:0x0a3d A[Catch: all -> 0x0b59, TRY_LEAVE, TryCatch #27 {all -> 0x0b59, blocks: (B:285:0x0a24, B:287:0x0a3d, B:292:0x0a9e), top: B:515:0x0a24 }] */
    /* JADX WARN: Code duplicated, block: B:290:0x0a96 A[Catch: all -> 0x0b07, TRY_LEAVE, TryCatch #50 {all -> 0x0b07, blocks: (B:288:0x0a75, B:290:0x0a96), top: B:554:0x0a75 }] */
    /* JADX WARN: Code duplicated, block: B:29:0x00f7 A[LOOP:4: B:18:0x00bc->B:29:0x00f7, LOOP_END] */
    /* JADX WARN: Code duplicated, block: B:308:0x0b12  */
    /* JADX WARN: Code duplicated, block: B:312:0x0b1b  */
    /* JADX WARN: Code duplicated, block: B:332:0x0bd2  */
    /* JADX WARN: Code duplicated, block: B:337:0x0c01 A[Catch: all -> 0x0cef, TRY_LEAVE, TryCatch #36 {all -> 0x0cef, blocks: (B:335:0x0be8, B:337:0x0c01, B:346:0x0c71), top: B:532:0x0be8 }] */
    /* JADX WARN: Code duplicated, block: B:344:0x0c69 A[Catch: all -> 0x0ccb, TRY_ENTER, TRY_LEAVE, TryCatch #21 {all -> 0x0ccb, blocks: (B:338:0x0c39, B:344:0x0c69), top: B:503:0x0c39 }] */
    /* JADX WARN: Code duplicated, block: B:357:0x0cd6  */
    /* JADX WARN: Code duplicated, block: B:494:0x0bb6 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:497:0x0b18 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:524:0x0c5a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:528:0x0803 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:601:0x0af8 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:604:0x0871 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:609:0x00f5 A[SYNTHETIC] */
    /* JADX WARN: Not initialized variable reg: 60, insn: 0x04cf: MOVE (r7 I:??[OBJECT, ARRAY]) = (r60 I:??[OBJECT, ARRAY] A[D('expectedCar' java.lang.String)]), block:B:131:0x04c1 */
    /* JADX WARN: Not initialized variable reg: 61, insn: 0x04d1: MOVE (r4 I:??[OBJECT, ARRAY]) = (r61 I:??[OBJECT, ARRAY] A[D('host' java.lang.String)]), block:B:131:0x04c1 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private final void bench(String scenario) throws JSONException {
        int i;
        String str;
        Object element$iv;
        WifiManager.WifiLock wifiLock;
        List events;
        Context context;
        String host;
        String expectedCar;
        String str2;
        OkHttpClient http;
        Ref.ObjectRef client;
        long started;
        Throwable e;
        ActivityScenario activity;
        String expectedCar2;
        String expectedCar3;
        String host2;
        String expectedCar4;
        long started2;
        Throwable th;
        char lockMode;
        WifiManager.WifiLock wifiLock2;
        WifiManager.WifiLock wifiLock3;
        Throwable th2;
        AssertionError e2;
        Ref.ObjectRef client2;
        UdpControlClient udpControlClient;
        Response response;
        Throwable th3;
        ResponseBody responseBodyBody;
        boolean passed;
        UdpControlClient udpControlClient2;
        Network wifi;
        long started3;
        String str3;
        int failures;
        boolean passed2;
        Response response2;
        Throwable th4;
        ResponseBody responseBodyBody2;
        String strM1220String$arg0$callrecord$try$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest;
        Ref.ObjectRef client3;
        long started4;
        String str4;
        OkHttpClient http2;
        String host3;
        long started5;
        Response response3;
        Throwable th5;
        ResponseBody responseBodyBody3;
        boolean z;
        Iterable $this$all$iv;
        int $i$f$all;
        boolean z2;
        Integer num;
        Integer num2;
        String str5 = scenario;
        String str6 = "report.toString(2)";
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue(Intrinsics.areEqual(args.getString("driveBench"), "disabled-outputs"));
        String host4 = args.getString("driveBenchHost");
        if (host4 == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(host4, "requireNotNull(args.getString(\"driveBenchHost\"))");
        String expectedCar5 = args.getString("driveBenchCarId");
        if (expectedCar5 == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(expectedCar5, "requireNotNull(args.getString(\"driveBenchCarId\"))");
        int i2 = 0;
        Iterable $this$map$iv = StringsKt.split$default(host4, new char[]{'.'}, false, 0, 6, (Object) null);
        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        for (Object item$iv$iv : $this$map$iv) {
            destination$iv$iv.add(StringsKt.toIntOrNull((String) item$iv$iv));
        }
        List octets = (List) destination$iv$iv;
        if (octets.size() == 4) {
            List $this$all$iv2 = octets;
            int $i$f$all2 = 0;
            if (($this$all$iv2 instanceof Collection) && $this$all$iv2.isEmpty()) {
                z = true;
            } else {
                Iterator it = $this$all$iv2.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        z = true;
                        break;
                    }
                    Object element$iv2 = it.next();
                    Integer it2 = (Integer) element$iv2;
                    if (it2 != null) {
                        $this$all$iv = $this$all$iv2;
                        int iM1066Int$$this$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$callall$branch$cond$arg0$callrequire$funbench$classUdpControlDeviceBenchTest = 0;
                        $i$f$all = $i$f$all2;
                        int $i$f$all3 = 255;
                        z2 = new IntRange(iM1066Int$$this$callrangeTo$$this$callcontains$branch$fun$anonymous$$arg0$callall$branch$cond$arg0$callrequire$funbench$classUdpControlDeviceBenchTest, $i$f$all3).contains(it2.intValue());
                        if (!z2) {
                            z = false;
                            break;
                        } else {
                            $this$all$iv2 = $this$all$iv;
                            $i$f$all2 = $i$f$all;
                        }
                    } else {
                        $this$all$iv = $this$all$iv2;
                        $i$f$all = $i$f$all2;
                    }
                    if (!z2) {
                        z = false;
                        break;
                    } else {
                        $this$all$iv2 = $this$all$iv;
                        $i$f$all2 = $i$f$all;
                    }
                }
            }
            if (z) {
                i2 = 0;
                Integer num3 = (Integer) octets.get(0);
                if ((num3 == null || num3.intValue() != 10) && ((num = (Integer) octets.get(0)) == null || num.intValue() != 192 || (num2 = (Integer) octets.get(1)) == null || num2.intValue() != 168)) {
                    Integer num4 = (Integer) octets.get(0);
                    int iM1099Int$arg1$callEQEQ$cond$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest = 172;
                    if (num4 != null && num4.intValue() == iM1099Int$arg1$callEQEQ$cond$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest) {
                        int iM1065Int$$this$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest = 16;
                        int iM1086Int$arg0$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest = 31;
                        Object obj = octets.get(1);
                        Intrinsics.checkNotNull(obj);
                        int iIntValue = ((Number) obj).intValue();
                        if (iM1065Int$$this$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest <= iIntValue && iIntValue <= iM1086Int$arg0$callrangeTo$$this$callcontains$branch$else$branch$arg0$callrequire$funbench$classUdpControlDeviceBenchTest) {
                        }
                    }
                }
                i = 1;
            } else {
                i2 = 0;
            }
            i = i2;
        } else {
            i = i2;
        }
        if (i == 0) {
            throw new IllegalArgumentException("Failed requirement.".toString());
        }
        Context context2 = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ConnectivityManager cm = (ConnectivityManager) context2.getSystemService(ConnectivityManager.class);
        Object[] allNetworks = cm.getAllNetworks();
        Intrinsics.checkNotNullExpressionValue(allNetworks, "cm.allNetworks");
        Object[] $this$firstOrNull$iv = allNetworks;
        int length = $this$firstOrNull$iv.length;
        while (true) {
            str = str6;
            if (i2 >= length) {
                element$iv = null;
                break;
            }
            element$iv = $this$firstOrNull$iv[i2];
            Object[] $this$firstOrNull$iv2 = $this$firstOrNull$iv;
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities((Network) element$iv);
            if (networkCapabilities != null && networkCapabilities.hasTransport(1) == true) {
                break;
            }
            i2++;
            $this$firstOrNull$iv = $this$firstOrNull$iv2;
            str6 = str;
        }
        if (element$iv == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Network wifi2 = (Network) element$iv;
        OkHttpClient http3 = new OkHttpClient.Builder().socketFactory(wifi2.getSocketFactory()).connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS).callTimeout(2, TimeUnit.SECONDS).build();
        List events2 = Collections.synchronizedList(new ArrayList());
        long started6 = SystemClock.elapsedRealtime();
        WifiManager wifiManager = (WifiManager) context2.getApplicationContext().getSystemService(WifiManager.class);
        WifiManager.WifiLock wifiLockCreateWifiLock = wifiManager.createWifiLock(4, "RaceRemote:udp-bench");
        ActivityScenario activity2 = null;
        Ref.ObjectRef client4 = new Ref.ObjectRef();
        boolean complete = false;
        try {
            ActivityScenario activity3 = ActivityScenario.launch(MainActivity.class);
            try {
                String str7 = "JSONObject().put(\"trial\"…rial).put(\"trace\", trace)";
                try {
                    activity3.onActivity(new ActivityScenario.ActivityAction() { // from class: vea.raceremote.UdpControlDeviceBenchTest$$ExternalSyntheticLambda0
                        @Override // androidx.test.core.app.ActivityScenario.ActivityAction
                        public final void perform(Activity activity4) {
                            UdpControlDeviceBenchTest.bench$lambda$6((MainActivity) activity4);
                        }
                    });
                    try {
                        Assert.assertEquals(Lifecycle.State.RESUMED, activity3.getState());
                        Assert.assertTrue(((PowerManager) context2.getSystemService(PowerManager.class)).isInteractive());
                        bench$record(events2, started6, "foreground", "MainActivity RESUMED, interactive, KEEP_SCREEN_ON; checked before test");
                        wifiLockCreateWifiLock.acquire();
                        long started7 = started6;
                        events = events2;
                        char lockMode2 = 4;
                        context = context2;
                        wifiLock = wifiLockCreateWifiLock;
                        WifiManager wifiManager2 = wifiManager;
                        http = http3;
                        try {
                            JSONObject initial = bench$status(http3, host4, expectedCar5, args, events, started7);
                            Assert.assertTrue("Nonneutral preflight", bench$neutral(initial));
                            Assert.assertTrue(initial.getBoolean("acceptingControl"));
                            String string = args.getString("udpBenchWarmupMs", "0");
                            Intrinsics.checkNotNullExpressionValue(string, "args.getString(\"udpBenchWarmupMs\", \"0\")");
                            long warmupMs = Long.parseLong(string);
                            if (warmupMs <= ((long) 15000) && ((long) 0) <= warmupMs) {
                                if (warmupMs > 0) {
                                    try {
                                        try {
                                            bench$record(events, started7, "warmup_start", Long.valueOf(warmupMs));
                                            SystemClock.sleep(warmupMs);
                                            bench$record(events, started7, "warmup_end", Long.valueOf(warmupMs));
                                            started7 = started7;
                                            activity = activity3;
                                            expectedCar2 = expectedCar5;
                                            try {
                                                Assert.assertTrue("Warmup must stay unarmed", bench$neutral(bench$status(http, host4, expectedCar5, args, events, started7)));
                                            } catch (Throwable th6) {
                                                host = host4;
                                                expectedCar = expectedCar2;
                                                started = started7;
                                                http = http;
                                                activity2 = activity;
                                                e = th6;
                                                str2 = str5;
                                                client = client4;
                                            }
                                        } catch (Throwable th7) {
                                            host = host4;
                                            expectedCar = expectedCar5;
                                            http = http;
                                            activity2 = activity3;
                                            e = th7;
                                            str2 = str5;
                                            started = started7;
                                            client = client4;
                                        }
                                    } catch (Throwable th8) {
                                        host = host4;
                                        expectedCar = expectedCar5;
                                        started = started7;
                                        http = http;
                                        activity2 = activity3;
                                        e = th8;
                                        str2 = str5;
                                        client = client4;
                                    }
                                } else {
                                    activity = activity3;
                                    expectedCar2 = expectedCar5;
                                }
                                try {
                                    String string2 = initial.getString("chipId");
                                    Intrinsics.checkNotNullExpressionValue(string2, "initial.getString(\"chipId\")");
                                    long chip = Long.parseLong(string2, CharsKt.checkRadix(16));
                                    try {
                                        if (Intrinsics.areEqual(str5, "drive")) {
                                            String expectedCar6 = expectedCar2;
                                            try {
                                                UdpControlClient c = bench$connect(host4, chip, client4, wifi2, events, started7);
                                                c.input(500, -300);
                                                expectedCar4 = expectedCar6;
                                                bench$awaitStatus(http, host4, expectedCar6, args, events, started7, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlDeviceBenchTest.bench.3
                                                    public final Boolean invoke(JSONObject it3) throws JSONException {
                                                        Intrinsics.checkNotNullParameter(it3, "it");
                                                        JSONObject p = it3.getJSONObject("drivePlan");
                                                        return Boolean.valueOf(p.getInt("fiPermille") == 125 && p.getInt("steeringNormalized") == -300);
                                                    }
                                                });
                                                c.input(-500, 400);
                                                bench$awaitStatus(http, host4, expectedCar4, args, events, started7, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlDeviceBenchTest.bench.4
                                                    public final Boolean invoke(JSONObject it3) throws JSONException {
                                                        Intrinsics.checkNotNullParameter(it3, "it");
                                                        JSONObject p = it3.getJSONObject("drivePlan");
                                                        return Boolean.valueOf(p.getInt("biPermille") == 125 && p.getInt("steeringNormalized") == 400);
                                                    }
                                                });
                                                c.close();
                                                long started8 = started7;
                                                try {
                                                    bench$record(events, started8, "closed", c.snapshot().toString());
                                                    bench$awaitStatus(http, host4, expectedCar4, args, events, started8, AnonymousClass5.INSTANCE);
                                                    Assert.assertEquals("Stopped", c.snapshot().getReason());
                                                    started2 = started8;
                                                    http = http;
                                                    host = host4;
                                                    str2 = str5;
                                                    client = client4;
                                                } catch (Throwable th9) {
                                                    started = started8;
                                                    http = http;
                                                    activity2 = activity;
                                                    expectedCar = expectedCar4;
                                                    host = host4;
                                                    e = th9;
                                                    str2 = str5;
                                                    client = client4;
                                                }
                                            } catch (Throwable th10) {
                                                expectedCar = expectedCar6;
                                                host = host4;
                                                started = started7;
                                                http = http;
                                                activity2 = activity;
                                                e = th10;
                                                str2 = str5;
                                                client = client4;
                                            }
                                        } else {
                                            host = host4;
                                            expectedCar4 = expectedCar2;
                                            try {
                                                if (Intrinsics.areEqual(str5, "link") ? true : Intrinsics.areEqual(str5, "wifi-mode") ? true : Intrinsics.areEqual(str5, "sustained")) {
                                                    try {
                                                        int failures2 = 0;
                                                        int sessions = Intrinsics.areEqual(str5, "sustained") ? 1 : 6;
                                                        long durationMs = Intrinsics.areEqual(str5, "sustained") ? 120000 : 5000;
                                                        String strM1212String$arg0$callrecord$branch1$when$try$funbench$classUdpControlDeviceBenchTest = "trial_plan";
                                                        JSONObject jSONObjectPut = new JSONObject().put("sessions", sessions).put("durationMs", durationMs);
                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut, "JSONObject().put(\"sessio…\"durationMs\", durationMs)");
                                                        long started9 = started7;
                                                        try {
                                                            bench$record(events, started9, strM1212String$arg0$callrecord$branch1$when$try$funbench$classUdpControlDeviceBenchTest, jSONObjectPut);
                                                            int failures3 = failures2;
                                                            int i3 = 0;
                                                            while (i3 < sessions) {
                                                                int trial = i3;
                                                                if (Intrinsics.areEqual(str5, "wifi-mode")) {
                                                                    try {
                                                                        if (!(Build.VERSION.SDK_INT < 34)) {
                                                                            throw new IllegalArgumentException("HIGH_PERF aliases LOW_LATENCY since API34".toString());
                                                                        }
                                                                        wifiLock.release();
                                                                        char lockMode3 = CollectionsKt.listOf(new Integer[]{Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(5)}).contains(Integer.valueOf(trial)) ? (char) 3 : (char) 4;
                                                                        try {
                                                                            WifiManager.WifiLock wifiLockCreateWifiLock2 = wifiManager2.createWifiLock(lockMode3, "RaceRemote:udp-bench");
                                                                            try {
                                                                                wifiLockCreateWifiLock2.acquire();
                                                                                lockMode = lockMode3;
                                                                                wifiLock2 = wifiLockCreateWifiLock2;
                                                                            } catch (Throwable th11) {
                                                                                wifiLock = wifiLockCreateWifiLock2;
                                                                                started = started9;
                                                                                http = http;
                                                                                activity2 = activity;
                                                                                expectedCar = expectedCar4;
                                                                                e = th11;
                                                                                str2 = str5;
                                                                                client = client4;
                                                                                host = host;
                                                                            }
                                                                        } catch (Throwable th12) {
                                                                            started = started9;
                                                                            http = http;
                                                                            activity2 = activity;
                                                                            expectedCar = expectedCar4;
                                                                            e = th12;
                                                                            str2 = str5;
                                                                            client = client4;
                                                                            host = host;
                                                                        }
                                                                    } catch (Throwable th13) {
                                                                        started = started9;
                                                                        http = http;
                                                                        activity2 = activity;
                                                                        expectedCar = expectedCar4;
                                                                        host = host;
                                                                        e = th13;
                                                                        str2 = str5;
                                                                        client = client4;
                                                                    }
                                                                } else {
                                                                    wifiLock2 = wifiLock;
                                                                    lockMode = lockMode2;
                                                                }
                                                                try {
                                                                    WifiManager wifiManager3 = wifiManager2;
                                                                    try {
                                                                        String strM1217String$arg0$callrecord$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest = "wifi_lock";
                                                                        int i4 = i3;
                                                                        int failures4 = failures3;
                                                                        JSONObject jSONObjectPut2 = new JSONObject().put("trial", trial).put("mode", (int) lockMode).put("held", wifiLock2.isHeld()).put("uid", Process.myUid());
                                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut2, "JSONObject().put(\"trial\"…droid.os.Process.myUid())");
                                                                        bench$record(events, started9, strM1217String$arg0$callrecord$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest, jSONObjectPut2);
                                                                        boolean passed3 = false;
                                                                        try {
                                                                            UdpControlClient c2 = bench$connect(host, chip, client4, wifi2, events, started9);
                                                                            long end = SystemClock.elapsedRealtime() + durationMs;
                                                                            try {
                                                                                try {
                                                                                    try {
                                                                                        try {
                                                                                            while (SystemClock.elapsedRealtime() < end) {
                                                                                                try {
                                                                                                    if (c2.snapshot().getPhase() != UdpControlSession.Phase.ACTIVE) {
                                                                                                        break;
                                                                                                    } else {
                                                                                                        SystemClock.sleep(10);
                                                                                                    }
                                                                                                } catch (AssertionError e3) {
                                                                                                    wifiLock3 = wifiLock2;
                                                                                                    e2 = e3;
                                                                                                } catch (Throwable th14) {
                                                                                                    th2 = th14;
                                                                                                    wifiLock3 = wifiLock2;
                                                                                                    lockMode = lockMode;
                                                                                                    client2 = client4;
                                                                                                    try {
                                                                                                        udpControlClient = (UdpControlClient) client2.element;
                                                                                                        if (udpControlClient == null) {
                                                                                                            client2.element = null;
                                                                                                            started9 = started9;
                                                                                                            client = client2;
                                                                                                            bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                            if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                                throw th2;
                                                                                                            }
                                                                                                            response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                                            Response it3 = response;
                                                                                                            Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it3.code()));
                                                                                                            responseBodyBody = it3.body();
                                                                                                            if (responseBodyBody == null) {
                                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                            }
                                                                                                            JSONObject trace = new JSONObject(responseBodyBody.string());
                                                                                                            CloseableKt.closeFinally(response, (Throwable) null);
                                                                                                            Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace.getInt("version")));
                                                                                                            Assert.assertEquals("udp-v1", trace.getString("transport"));
                                                                                                            String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest = "firmware_trace";
                                                                                                            JSONObject jSONObjectPut3 = new JSONObject().put("trial", trial).put("trace", trace);
                                                                                                            Intrinsics.checkNotNullExpressionValue(jSONObjectPut3, str7);
                                                                                                            bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest, jSONObjectPut3);
                                                                                                            throw th2;
                                                                                                            throw th3;
                                                                                                        }
                                                                                                        try {
                                                                                                            udpControlClient.close();
                                                                                                            Unit unit = Unit.INSTANCE;
                                                                                                            client2.element = null;
                                                                                                            started9 = started9;
                                                                                                            client = client2;
                                                                                                            try {
                                                                                                                bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                                try {
                                                                                                                    if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                                        throw th2;
                                                                                                                    }
                                                                                                                    response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                                                    try {
                                                                                                                        Response it4 = response;
                                                                                                                        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it4.code()));
                                                                                                                        responseBodyBody = it4.body();
                                                                                                                        if (responseBodyBody == null) {
                                                                                                                            try {
                                                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                                            } catch (Throwable th15) {
                                                                                                                                th3 = th15;
                                                                                                                                throw th3;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        JSONObject trace2 = new JSONObject(responseBodyBody.string());
                                                                                                                        CloseableKt.closeFinally(response, (Throwable) null);
                                                                                                                        Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace2.getInt("version")));
                                                                                                                        Assert.assertEquals("udp-v1", trace2.getString("transport"));
                                                                                                                        String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest2 = "firmware_trace";
                                                                                                                        JSONObject jSONObjectPut4 = new JSONObject().put("trial", trial).put("trace", trace2);
                                                                                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut4, str7);
                                                                                                                        bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest2, jSONObjectPut4);
                                                                                                                        throw th2;
                                                                                                                    } catch (Throwable th16) {
                                                                                                                        th3 = th16;
                                                                                                                    }
                                                                                                                    try {
                                                                                                                        throw th3;
                                                                                                                    } catch (Throwable th17) {
                                                                                                                        CloseableKt.closeFinally(response, th3);
                                                                                                                        throw th17;
                                                                                                                    }
                                                                                                                } catch (Throwable th18) {
                                                                                                                    str2 = scenario;
                                                                                                                    e = th18;
                                                                                                                    started = started9;
                                                                                                                    activity2 = activity;
                                                                                                                    expectedCar = expectedCar4;
                                                                                                                    wifiLock = wifiLock3;
                                                                                                                }
                                                                                                            } catch (Throwable th19) {
                                                                                                                str2 = scenario;
                                                                                                                e = th19;
                                                                                                                activity2 = activity;
                                                                                                                expectedCar = expectedCar4;
                                                                                                                started = started9;
                                                                                                                wifiLock = wifiLock3;
                                                                                                            }
                                                                                                        } catch (Throwable th20) {
                                                                                                            str2 = scenario;
                                                                                                            e = th20;
                                                                                                            client = client2;
                                                                                                            activity2 = activity;
                                                                                                            expectedCar = expectedCar4;
                                                                                                            started = started9;
                                                                                                            wifiLock = wifiLock3;
                                                                                                        }
                                                                                                    } catch (Throwable th21) {
                                                                                                        client = client2;
                                                                                                        str2 = scenario;
                                                                                                        e = th21;
                                                                                                        started = started9;
                                                                                                        activity2 = activity;
                                                                                                        expectedCar = expectedCar4;
                                                                                                        wifiLock = wifiLock3;
                                                                                                    }
                                                                                                    bench$record(events, started, "failure", e.toString());
                                                                                                    throw e;
                                                                                                }
                                                                                                try {
                                                                                                    passed = passed3;
                                                                                                    try {
                                                                                                        bench$record(events, started9, "trial_error", e2.toString());
                                                                                                        try {
                                                                                                            udpControlClient2 = (UdpControlClient) client4.element;
                                                                                                            if (udpControlClient2 != null) {
                                                                                                                udpControlClient2.close();
                                                                                                                Unit unit2 = Unit.INSTANCE;
                                                                                                            }
                                                                                                            client4.element = null;
                                                                                                            wifi = wifi2;
                                                                                                            started3 = started9;
                                                                                                            client4 = client4;
                                                                                                            str3 = str7;
                                                                                                            http = http;
                                                                                                            failures = failures4;
                                                                                                            host = host;
                                                                                                            try {
                                                                                                                bench$awaitStatus(http, host, expectedCar4, args, events, started3, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                                if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                                    response2 = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                                                    try {
                                                                                                                        Response it5 = response2;
                                                                                                                        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it5.code()));
                                                                                                                        responseBodyBody2 = it5.body();
                                                                                                                        if (responseBodyBody2 == null) {
                                                                                                                            JSONObject trace3 = new JSONObject(responseBodyBody2.string());
                                                                                                                            CloseableKt.closeFinally(response2, (Throwable) null);
                                                                                                                            Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace3.getInt("version")));
                                                                                                                            Assert.assertEquals("udp-v1", trace3.getString("transport"));
                                                                                                                            String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest3 = "firmware_trace";
                                                                                                                            JSONObject jSONObjectPut5 = new JSONObject().put("trial", trial).put("trace", trace3);
                                                                                                                            Intrinsics.checkNotNullExpressionValue(jSONObjectPut5, str3);
                                                                                                                            started3 = started3;
                                                                                                                            bench$record(events, started3, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest3, jSONObjectPut5);
                                                                                                                        } else {
                                                                                                                            try {
                                                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                                            } catch (Throwable th22) {
                                                                                                                                th4 = th22;
                                                                                                                            }
                                                                                                                        }
                                                                                                                    } catch (Throwable th23) {
                                                                                                                        th4 = th23;
                                                                                                                    }
                                                                                                                    try {
                                                                                                                        throw th4;
                                                                                                                    } catch (Throwable th24) {
                                                                                                                        CloseableKt.closeFinally(response2, th4);
                                                                                                                        throw th24;
                                                                                                                    }
                                                                                                                }
                                                                                                                started3 = started3;
                                                                                                                passed2 = passed;
                                                                                                                if (passed2) {
                                                                                                                    failures3 = failures;
                                                                                                                } else {
                                                                                                                    failures3 = failures + 1;
                                                                                                                }
                                                                                                                SystemClock.sleep(100);
                                                                                                                i3 = i4 + 1;
                                                                                                                str5 = scenario;
                                                                                                                host = host;
                                                                                                                started9 = started3;
                                                                                                                http = http;
                                                                                                                durationMs = durationMs;
                                                                                                                lockMode2 = lockMode;
                                                                                                                wifi2 = wifi;
                                                                                                                sessions = sessions;
                                                                                                                initial = initial;
                                                                                                                str7 = str3;
                                                                                                                wifiLock = wifiLock3;
                                                                                                                wifiManager2 = wifiManager3;
                                                                                                            } catch (Throwable th25) {
                                                                                                                str2 = scenario;
                                                                                                                e = th25;
                                                                                                                client = client4;
                                                                                                                activity2 = activity;
                                                                                                                expectedCar = expectedCar4;
                                                                                                                started = started3;
                                                                                                                wifiLock = wifiLock3;
                                                                                                            }
                                                                                                        } catch (Throwable th26) {
                                                                                                            http = http;
                                                                                                            host = host;
                                                                                                            client = client4;
                                                                                                            started = started9;
                                                                                                            activity2 = activity;
                                                                                                            expectedCar = expectedCar4;
                                                                                                            str2 = scenario;
                                                                                                            wifiLock = wifiLock3;
                                                                                                            e = th26;
                                                                                                        }
                                                                                                    } catch (Throwable th27) {
                                                                                                        client4 = client4;
                                                                                                        th2 = th27;
                                                                                                        passed3 = passed;
                                                                                                        client2 = client4;
                                                                                                        udpControlClient = (UdpControlClient) client2.element;
                                                                                                        if (udpControlClient == null) {
                                                                                                            client2.element = null;
                                                                                                            started9 = started9;
                                                                                                            client = client2;
                                                                                                            bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                            if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                                throw th2;
                                                                                                            }
                                                                                                            response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                                            Response it6 = response;
                                                                                                            Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it6.code()));
                                                                                                            responseBodyBody = it6.body();
                                                                                                            if (responseBodyBody == null) {
                                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                            }
                                                                                                            JSONObject trace4 = new JSONObject(responseBodyBody.string());
                                                                                                            CloseableKt.closeFinally(response, (Throwable) null);
                                                                                                            Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace4.getInt("version")));
                                                                                                            Assert.assertEquals("udp-v1", trace4.getString("transport"));
                                                                                                            String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest4 = "firmware_trace";
                                                                                                            JSONObject jSONObjectPut6 = new JSONObject().put("trial", trial).put("trace", trace4);
                                                                                                            Intrinsics.checkNotNullExpressionValue(jSONObjectPut6, str7);
                                                                                                            bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest4, jSONObjectPut6);
                                                                                                            throw th2;
                                                                                                            throw th3;
                                                                                                        }
                                                                                                        udpControlClient.close();
                                                                                                        Unit unit3 = Unit.INSTANCE;
                                                                                                        client2.element = null;
                                                                                                        started9 = started9;
                                                                                                        client = client2;
                                                                                                        bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                        if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                            throw th2;
                                                                                                        }
                                                                                                        response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                                        Response it7 = response;
                                                                                                        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it7.code()));
                                                                                                        responseBodyBody = it7.body();
                                                                                                        if (responseBodyBody == null) {
                                                                                                            throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                        }
                                                                                                        JSONObject trace5 = new JSONObject(responseBodyBody.string());
                                                                                                        CloseableKt.closeFinally(response, (Throwable) null);
                                                                                                        Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace5.getInt("version")));
                                                                                                        Assert.assertEquals("udp-v1", trace5.getString("transport"));
                                                                                                        String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest5 = "firmware_trace";
                                                                                                        JSONObject jSONObjectPut7 = new JSONObject().put("trial", trial).put("trace", trace5);
                                                                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut7, str7);
                                                                                                        bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest5, jSONObjectPut7);
                                                                                                        throw th2;
                                                                                                        throw th3;
                                                                                                    }
                                                                                                } catch (Throwable th28) {
                                                                                                    client4 = client4;
                                                                                                    th2 = th28;
                                                                                                }
                                                                                            }
                                                                                            UdpControlClient udpControlClient3 = (UdpControlClient) client3.element;
                                                                                            if (udpControlClient3 != null) {
                                                                                                try {
                                                                                                    udpControlClient3.close();
                                                                                                    Unit unit4 = Unit.INSTANCE;
                                                                                                    client3.element = null;
                                                                                                    i4 = i4;
                                                                                                    started4 = started9;
                                                                                                    durationMs = durationMs;
                                                                                                    lockMode = lockMode;
                                                                                                    try {
                                                                                                        bench$awaitStatus(http, host, expectedCar4, args, events, started4, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                        if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                            host3 = host;
                                                                                                            try {
                                                                                                                http2 = http;
                                                                                                                try {
                                                                                                                    response3 = (Closeable) http2.newCall(new Request.Builder().url("http://" + host3 + "/debug/udp-trace").build()).execute();
                                                                                                                    try {
                                                                                                                        Response it8 = response3;
                                                                                                                        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it8.code()));
                                                                                                                        responseBodyBody3 = it8.body();
                                                                                                                        if (responseBodyBody3 == null) {
                                                                                                                            try {
                                                                                                                                JSONObject trace6 = new JSONObject(responseBodyBody3.string());
                                                                                                                                CloseableKt.closeFinally(response3, (Throwable) null);
                                                                                                                                Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace6.getInt("version")));
                                                                                                                                Assert.assertEquals("udp-v1", trace6.getString("transport"));
                                                                                                                                String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest6 = "firmware_trace";
                                                                                                                                JSONObject jSONObjectPut8 = new JSONObject().put("trial", trial).put("trace", trace6);
                                                                                                                                str4 = str7;
                                                                                                                                Intrinsics.checkNotNullExpressionValue(jSONObjectPut8, str4);
                                                                                                                                started5 = started4;
                                                                                                                                bench$record(events, started5, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest6, jSONObjectPut8);
                                                                                                                            } catch (Throwable th29) {
                                                                                                                                started5 = started4;
                                                                                                                                th5 = th29;
                                                                                                                                throw th5;
                                                                                                                            }
                                                                                                                        } else {
                                                                                                                            started5 = started4;
                                                                                                                            try {
                                                                                                                                try {
                                                                                                                                    throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                                                } catch (Throwable th30) {
                                                                                                                                    th5 = th30;
                                                                                                                                }
                                                                                                                            } catch (Throwable th31) {
                                                                                                                                th5 = th31;
                                                                                                                            }
                                                                                                                        }
                                                                                                                    } catch (Throwable th32) {
                                                                                                                        started5 = started4;
                                                                                                                        th5 = th32;
                                                                                                                    }
                                                                                                                    try {
                                                                                                                        throw th5;
                                                                                                                    } catch (Throwable th33) {
                                                                                                                        CloseableKt.closeFinally(response3, th5);
                                                                                                                        throw th33;
                                                                                                                    }
                                                                                                                } catch (Throwable th34) {
                                                                                                                    client = client3;
                                                                                                                    http = http2;
                                                                                                                    host = host3;
                                                                                                                    activity2 = activity;
                                                                                                                    expectedCar = expectedCar4;
                                                                                                                    started = started4;
                                                                                                                    str2 = scenario;
                                                                                                                    wifiLock = wifiLock3;
                                                                                                                    e = th34;
                                                                                                                }
                                                                                                            } catch (Throwable th35) {
                                                                                                                client = client3;
                                                                                                                host = host3;
                                                                                                                http = http;
                                                                                                                activity2 = activity;
                                                                                                                expectedCar = expectedCar4;
                                                                                                                started = started4;
                                                                                                                str2 = scenario;
                                                                                                                wifiLock = wifiLock3;
                                                                                                                e = th35;
                                                                                                            }
                                                                                                        } else {
                                                                                                            str4 = str7;
                                                                                                            http2 = http;
                                                                                                            host3 = host;
                                                                                                            started5 = started4;
                                                                                                        }
                                                                                                        wifi = wifi2;
                                                                                                        client4 = client3;
                                                                                                        failures = failures4;
                                                                                                        str3 = str4;
                                                                                                        host = host3;
                                                                                                        passed2 = passed3;
                                                                                                        http = http2;
                                                                                                        started3 = started5;
                                                                                                        if (passed2) {
                                                                                                            failures3 = failures + 1;
                                                                                                        } else {
                                                                                                            failures3 = failures;
                                                                                                        }
                                                                                                        SystemClock.sleep(100);
                                                                                                        i3 = i4 + 1;
                                                                                                        str5 = scenario;
                                                                                                        host = host;
                                                                                                        started9 = started3;
                                                                                                        http = http;
                                                                                                        durationMs = durationMs;
                                                                                                        lockMode2 = lockMode;
                                                                                                        wifi2 = wifi;
                                                                                                        sessions = sessions;
                                                                                                        initial = initial;
                                                                                                        str7 = str3;
                                                                                                        wifiLock = wifiLock3;
                                                                                                        wifiManager2 = wifiManager3;
                                                                                                    } catch (Throwable th36) {
                                                                                                        client = client3;
                                                                                                        http = http;
                                                                                                        activity2 = activity;
                                                                                                        expectedCar = expectedCar4;
                                                                                                        host = host;
                                                                                                        started = started4;
                                                                                                        str2 = scenario;
                                                                                                        wifiLock = wifiLock3;
                                                                                                        e = th36;
                                                                                                    }
                                                                                                } catch (Throwable th37) {
                                                                                                    client = client3;
                                                                                                    started = started9;
                                                                                                    wifiLock = wifiLock3;
                                                                                                    http = http;
                                                                                                    activity2 = activity;
                                                                                                    expectedCar = expectedCar4;
                                                                                                    host = host;
                                                                                                    str2 = scenario;
                                                                                                    e = th37;
                                                                                                }
                                                                                            } else {
                                                                                                client3.element = null;
                                                                                                i4 = i4;
                                                                                                started4 = started9;
                                                                                                durationMs = durationMs;
                                                                                                lockMode = lockMode;
                                                                                                bench$awaitStatus(http, host, expectedCar4, args, events, started4, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                                if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                    host3 = host;
                                                                                                    http2 = http;
                                                                                                    response3 = (Closeable) http2.newCall(new Request.Builder().url("http://" + host3 + "/debug/udp-trace").build()).execute();
                                                                                                    Response it9 = response3;
                                                                                                    Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it9.code()));
                                                                                                    responseBodyBody3 = it9.body();
                                                                                                    if (responseBodyBody3 == null) {
                                                                                                        started5 = started4;
                                                                                                        throw new IllegalArgumentException("Required value was null.".toString());
                                                                                                    }
                                                                                                    JSONObject trace7 = new JSONObject(responseBodyBody3.string());
                                                                                                    CloseableKt.closeFinally(response3, (Throwable) null);
                                                                                                    Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace7.getInt("version")));
                                                                                                    Assert.assertEquals("udp-v1", trace7.getString("transport"));
                                                                                                    String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest7 = "firmware_trace";
                                                                                                    JSONObject jSONObjectPut9 = new JSONObject().put("trial", trial).put("trace", trace7);
                                                                                                    str4 = str7;
                                                                                                    Intrinsics.checkNotNullExpressionValue(jSONObjectPut9, str4);
                                                                                                    started5 = started4;
                                                                                                    bench$record(events, started5, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest7, jSONObjectPut9);
                                                                                                    throw th5;
                                                                                                }
                                                                                                str4 = str7;
                                                                                                http2 = http;
                                                                                                host3 = host;
                                                                                                started5 = started4;
                                                                                                wifi = wifi2;
                                                                                                client4 = client3;
                                                                                                failures = failures4;
                                                                                                str3 = str4;
                                                                                                host = host3;
                                                                                                passed2 = passed3;
                                                                                                http = http2;
                                                                                                started3 = started5;
                                                                                                if (passed2) {
                                                                                                    failures3 = failures + 1;
                                                                                                } else {
                                                                                                    failures3 = failures;
                                                                                                }
                                                                                                SystemClock.sleep(100);
                                                                                                i3 = i4 + 1;
                                                                                                str5 = scenario;
                                                                                                host = host;
                                                                                                started9 = started3;
                                                                                                http = http;
                                                                                                durationMs = durationMs;
                                                                                                lockMode2 = lockMode;
                                                                                                wifi2 = wifi;
                                                                                                sessions = sessions;
                                                                                                initial = initial;
                                                                                                str7 = str3;
                                                                                                wifiLock = wifiLock3;
                                                                                                wifiManager2 = wifiManager3;
                                                                                            }
                                                                                        } catch (Throwable th38) {
                                                                                            client = client3;
                                                                                            started = started9;
                                                                                            http = http;
                                                                                            activity2 = activity;
                                                                                            expectedCar = expectedCar4;
                                                                                            host = host;
                                                                                            str2 = scenario;
                                                                                            wifiLock = wifiLock3;
                                                                                            e = th38;
                                                                                        }
                                                                                        JSONObject jSONObjectPut10 = new JSONObject().put("trial", trial).put("passed", passed3).put("wifiMode", (int) lockMode).put("snapshot", c2.snapshot().toString());
                                                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut10, "JSONObject().put(\"trial\"… c.snapshot().toString())");
                                                                                        bench$record(events, started9, strM1220String$arg0$callrecord$try$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest, jSONObjectPut10);
                                                                                        client3 = client4;
                                                                                    } catch (AssertionError e4) {
                                                                                        e2 = e4;
                                                                                        passed3 = passed3;
                                                                                        passed = passed3;
                                                                                        bench$record(events, started9, "trial_error", e2.toString());
                                                                                        udpControlClient2 = (UdpControlClient) client4.element;
                                                                                        if (udpControlClient2 != null) {
                                                                                            udpControlClient2.close();
                                                                                            Unit unit5 = Unit.INSTANCE;
                                                                                        }
                                                                                        client4.element = null;
                                                                                        wifi = wifi2;
                                                                                        started3 = started9;
                                                                                        client4 = client4;
                                                                                        str3 = str7;
                                                                                        http = http;
                                                                                        failures = failures4;
                                                                                        host = host;
                                                                                        bench$awaitStatus(http, host, expectedCar4, args, events, started3, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                        if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                            response2 = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                            Response it10 = response2;
                                                                                            Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it10.code()));
                                                                                            responseBodyBody2 = it10.body();
                                                                                            if (responseBodyBody2 == null) {
                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                            }
                                                                                            JSONObject trace8 = new JSONObject(responseBodyBody2.string());
                                                                                            CloseableKt.closeFinally(response2, (Throwable) null);
                                                                                            Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace8.getInt("version")));
                                                                                            Assert.assertEquals("udp-v1", trace8.getString("transport"));
                                                                                            String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest8 = "firmware_trace";
                                                                                            JSONObject jSONObjectPut11 = new JSONObject().put("trial", trial).put("trace", trace8);
                                                                                            Intrinsics.checkNotNullExpressionValue(jSONObjectPut11, str3);
                                                                                            started3 = started3;
                                                                                            bench$record(events, started3, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest8, jSONObjectPut11);
                                                                                            throw th4;
                                                                                        }
                                                                                        started3 = started3;
                                                                                        passed2 = passed;
                                                                                        if (passed2) {
                                                                                            failures3 = failures + 1;
                                                                                        } else {
                                                                                            failures3 = failures;
                                                                                        }
                                                                                        SystemClock.sleep(100);
                                                                                        i3 = i4 + 1;
                                                                                        str5 = scenario;
                                                                                        host = host;
                                                                                        started9 = started3;
                                                                                        http = http;
                                                                                        durationMs = durationMs;
                                                                                        lockMode2 = lockMode;
                                                                                        wifi2 = wifi;
                                                                                        sessions = sessions;
                                                                                        initial = initial;
                                                                                        str7 = str3;
                                                                                        wifiLock = wifiLock3;
                                                                                        wifiManager2 = wifiManager3;
                                                                                    } catch (Throwable th39) {
                                                                                        lockMode = lockMode;
                                                                                        th2 = th39;
                                                                                        passed3 = passed3;
                                                                                        client2 = client4;
                                                                                        udpControlClient = (UdpControlClient) client2.element;
                                                                                        if (udpControlClient == null) {
                                                                                            client2.element = null;
                                                                                            started9 = started9;
                                                                                            client = client2;
                                                                                            bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                            if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                                throw th2;
                                                                                            }
                                                                                            response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                            Response it11 = response;
                                                                                            Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it11.code()));
                                                                                            responseBodyBody = it11.body();
                                                                                            if (responseBodyBody == null) {
                                                                                                throw new IllegalArgumentException("Required value was null.".toString());
                                                                                            }
                                                                                            JSONObject trace9 = new JSONObject(responseBodyBody.string());
                                                                                            CloseableKt.closeFinally(response, (Throwable) null);
                                                                                            Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace9.getInt("version")));
                                                                                            Assert.assertEquals("udp-v1", trace9.getString("transport"));
                                                                                            String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest9 = "firmware_trace";
                                                                                            JSONObject jSONObjectPut12 = new JSONObject().put("trial", trial).put("trace", trace9);
                                                                                            Intrinsics.checkNotNullExpressionValue(jSONObjectPut12, str7);
                                                                                            bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest9, jSONObjectPut12);
                                                                                            throw th2;
                                                                                            throw th3;
                                                                                        }
                                                                                        udpControlClient.close();
                                                                                        Unit unit6 = Unit.INSTANCE;
                                                                                        client2.element = null;
                                                                                        started9 = started9;
                                                                                        client = client2;
                                                                                        bench$awaitStatus(http, host, expectedCar4, args, events, started9, UdpControlDeviceBenchTest$bench$6$2.INSTANCE);
                                                                                        if (Intrinsics.areEqual(args.getString("udpBenchFirmwareTrace"), "true")) {
                                                                                            throw th2;
                                                                                        }
                                                                                        response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/debug/udp-trace").build()).execute();
                                                                                        Response it12 = response;
                                                                                        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it12.code()));
                                                                                        responseBodyBody = it12.body();
                                                                                        if (responseBodyBody == null) {
                                                                                            throw new IllegalArgumentException("Required value was null.".toString());
                                                                                        }
                                                                                        JSONObject trace10 = new JSONObject(responseBodyBody.string());
                                                                                        CloseableKt.closeFinally(response, (Throwable) null);
                                                                                        Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(trace10.getInt("version")));
                                                                                        Assert.assertEquals("udp-v1", trace10.getString("transport"));
                                                                                        String strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest10 = "firmware_trace";
                                                                                        JSONObject jSONObjectPut13 = new JSONObject().put("trial", trial).put("trace", trace10);
                                                                                        Intrinsics.checkNotNullExpressionValue(jSONObjectPut13, str7);
                                                                                        bench$record(events, started9, strM1209String$arg0$callrecord$branch$if$finally$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest10, jSONObjectPut13);
                                                                                        throw th2;
                                                                                        throw th3;
                                                                                    }
                                                                                } catch (AssertionError e5) {
                                                                                    e2 = e5;
                                                                                } catch (Throwable th40) {
                                                                                    lockMode = lockMode;
                                                                                    th2 = th40;
                                                                                }
                                                                                strM1220String$arg0$callrecord$try$fun$anonymous$$arg1$callrepeat$branch1$when$try$funbench$classUdpControlDeviceBenchTest = "trial";
                                                                                wifiLock3 = wifiLock2;
                                                                            } catch (AssertionError e6) {
                                                                                wifiLock3 = wifiLock2;
                                                                                e2 = e6;
                                                                            } catch (Throwable th41) {
                                                                                wifiLock3 = wifiLock2;
                                                                                lockMode = lockMode;
                                                                                th2 = th41;
                                                                            }
                                                                            passed3 = c2.snapshot().getPhase() == UdpControlSession.Phase.ACTIVE;
                                                                        } catch (AssertionError e7) {
                                                                            wifiLock3 = wifiLock2;
                                                                            e2 = e7;
                                                                        } catch (Throwable th42) {
                                                                            wifiLock3 = wifiLock2;
                                                                            lockMode = lockMode;
                                                                            th2 = th42;
                                                                        }
                                                                        str2 = scenario;
                                                                        e = th;
                                                                        started = started3;
                                                                        client = client4;
                                                                        activity2 = activity;
                                                                        expectedCar = expectedCar4;
                                                                        wifiLock = wifiLock3;
                                                                    } catch (Throwable th43) {
                                                                        WifiManager.WifiLock wifiLock4 = wifiLock2;
                                                                        client = client4;
                                                                        http = http;
                                                                        host = host;
                                                                        str2 = scenario;
                                                                        started = started9;
                                                                        activity2 = activity;
                                                                        expectedCar = expectedCar4;
                                                                        wifiLock = wifiLock4;
                                                                        e = th43;
                                                                    }
                                                                } catch (Throwable th44) {
                                                                    WifiManager.WifiLock wifiLock5 = wifiLock2;
                                                                    client = client4;
                                                                    http = http;
                                                                    host = host;
                                                                    str2 = scenario;
                                                                    started = started9;
                                                                    activity2 = activity;
                                                                    expectedCar = expectedCar4;
                                                                    wifiLock = wifiLock5;
                                                                    e = th44;
                                                                }
                                                            }
                                                            int failures5 = failures3;
                                                            int sessions2 = sessions;
                                                            client = client4;
                                                            http = http;
                                                            host = host;
                                                            long durationMs2 = started9;
                                                            try {
                                                                String strM1223String$arg0$callrecord1$branch1$when$try$funbench$classUdpControlDeviceBenchTest = "summary";
                                                                JSONObject jSONObjectPut14 = new JSONObject().put("sessions", sessions2).put("failures", failures5);
                                                                Intrinsics.checkNotNullExpressionValue(jSONObjectPut14, "JSONObject().put(\"sessio…put(\"failures\", failures)");
                                                                bench$record(events, durationMs2, strM1223String$arg0$callrecord1$branch1$when$try$funbench$classUdpControlDeviceBenchTest, jSONObjectPut14);
                                                                Assert.assertEquals("UDP sessions failed", Integer.valueOf(0), Integer.valueOf(failures5));
                                                                str2 = scenario;
                                                                started2 = durationMs2;
                                                            } catch (Throwable th45) {
                                                                str2 = scenario;
                                                                e = th45;
                                                                started = durationMs2;
                                                                activity2 = activity;
                                                                expectedCar = expectedCar4;
                                                            }
                                                        } catch (Throwable th46) {
                                                            client = client4;
                                                            http = http;
                                                            host = host;
                                                            str2 = scenario;
                                                            started = started9;
                                                            activity2 = activity;
                                                            expectedCar = expectedCar4;
                                                            e = th46;
                                                        }
                                                    } catch (Throwable th47) {
                                                        client = client4;
                                                        http = http;
                                                        host = host;
                                                        str2 = scenario;
                                                        started = started7;
                                                        activity2 = activity;
                                                        expectedCar = expectedCar4;
                                                        e = th47;
                                                    }
                                                } else {
                                                    client = client4;
                                                    long started10 = started7;
                                                    http = http;
                                                    host = host;
                                                    try {
                                                        str2 = scenario;
                                                        try {
                                                            if (Intrinsics.areEqual(str2, "watchdog")) {
                                                                try {
                                                                    try {
                                                                        DatagramSocket datagramSocket = new DatagramSocket();
                                                                        try {
                                                                            DatagramSocket raw = datagramSocket;
                                                                            try {
                                                                                wifi2.bindSocket(raw);
                                                                                raw.connect(InetAddress.getByName(host), 3334);
                                                                                raw.setSoTimeout(200);
                                                                                UdpControlPacket open = new UdpControlPacket(0, 305419896, 0L, 0L, 0, 0, chip, 0L, 128, (DefaultConstructorMarker) null);
                                                                                UdpControlPacket hello = bench$lambda$11$exchange(raw, chip, events, started10, open);
                                                                                Assert.assertEquals((Object) 128, (Object) Integer.valueOf(hello.getKind()));
                                                                                UdpControlPacket arm = UdpControlPacket.copy$default(open, 1, 0L, hello.getToken(), 1, 0, 0, 0L, 0L, 242, (Object) null);
                                                                                Assert.assertEquals(1, bench$lambda$11$exchange(raw, chip, events, started10, arm).getSequence());
                                                                                UdpControlPacket drive = UdpControlPacket.copy$default(arm, 2, 0L, 0L, 2, 500, 300, 0L, 1, 70, (Object) null);
                                                                                Assert.assertEquals(2, bench$lambda$11$exchange(raw, chip, events, started10, drive).getSequence());
                                                                                started2 = started10;
                                                                                try {
                                                                                    JSONObject active = bench$status(http, host, expectedCar4, args, events, started10);
                                                                                    Assert.assertTrue(active.getBoolean("armed"));
                                                                                    Assert.assertEquals(Integer.valueOf(500), Integer.valueOf(active.getInt("throttle")));
                                                                                    try {
                                                                                        bench$record(events, started2, "silence_started", "No further UDP traffic or STOP");
                                                                                        SystemClock.sleep(350);
                                                                                        started2 = started2;
                                                                                        Assert.assertTrue("Board did not expire silent peer", bench$neutral(bench$status(http, host, expectedCar4, args, events, started2)));
                                                                                        Unit unit7 = Unit.INSTANCE;
                                                                                        CloseableKt.closeFinally(datagramSocket, (Throwable) null);
                                                                                    } catch (Throwable th48) {
                                                                                        started2 = started2;
                                                                                        th = th48;
                                                                                        try {
                                                                                            throw th;
                                                                                        } catch (Throwable th49) {
                                                                                            CloseableKt.closeFinally(datagramSocket, th);
                                                                                            throw th49;
                                                                                        }
                                                                                    }
                                                                                } catch (Throwable th50) {
                                                                                    th = th50;
                                                                                }
                                                                            } catch (Throwable th51) {
                                                                                started2 = started10;
                                                                                th = th51;
                                                                            }
                                                                        } catch (Throwable th52) {
                                                                            started2 = started10;
                                                                            th = th52;
                                                                        }
                                                                    } catch (Throwable th53) {
                                                                        e = th53;
                                                                        activity2 = activity;
                                                                        expectedCar = expectedCar4;
                                                                        started = started2;
                                                                    }
                                                                } catch (Throwable th54) {
                                                                    e = th54;
                                                                    started = started10;
                                                                    activity2 = activity;
                                                                    expectedCar = expectedCar4;
                                                                }
                                                            } else {
                                                                started2 = started10;
                                                            }
                                                        } catch (Throwable th55) {
                                                            th = th55;
                                                            started = started10;
                                                            expectedCar = expectedCar4;
                                                            e = th;
                                                            activity2 = activity;
                                                        }
                                                    } catch (Throwable th56) {
                                                        th = th56;
                                                        str2 = scenario;
                                                    }
                                                }
                                            } catch (Throwable th57) {
                                                started = started7;
                                                http = http;
                                                expectedCar = expectedCar4;
                                                host = host;
                                                str2 = str5;
                                                client = client4;
                                                e = th57;
                                                activity2 = activity;
                                            }
                                        }
                                        try {
                                            boolean complete2 = true;
                                            UdpControlClient udpControlClient4 = (UdpControlClient) client.element;
                                            if (udpControlClient4 != null) {
                                                udpControlClient4.close();
                                                Unit unit8 = Unit.INSTANCE;
                                            }
                                            if (wifiLock.isHeld()) {
                                                wifiLock.release();
                                            }
                                            if (activity != null) {
                                                activity.close();
                                                Unit unit9 = Unit.INSTANCE;
                                            }
                                            http.connectionPool().evictAll();
                                            http.dispatcher().executorService().shutdown();
                                            JSONObject jSONObjectPut15 = new JSONObject().put("scenario", str2).put("complete", complete2).put("elapsedMs", SystemClock.elapsedRealtime() - started2).put("host", host).put("carId", expectedCar4);
                                            String strM1208String$arg0$callput$valreport$finally$funbench$classUdpControlDeviceBenchTest = "events";
                                            Intrinsics.checkNotNullExpressionValue(events, "events");
                                            JSONObject report = jSONObjectPut15.put(strM1208String$arg0$callput$valreport$finally$funbench$classUdpControlDeviceBenchTest, new JSONArray((Collection) CollectionsKt.toList(events)));
                                            File file = new File(context.getFilesDir(), "udp-control-" + str2 + ".json");
                                            String string3 = report.toString(2);
                                            Intrinsics.checkNotNullExpressionValue(string3, str);
                                            FilesKt.writeText$default(file, string3, (Charset) null, 2, (Object) null);
                                            return;
                                        } catch (Throwable th58) {
                                            expectedCar = expectedCar4;
                                            started = started2;
                                            e = th58;
                                            activity2 = activity;
                                        }
                                    } catch (Throwable th59) {
                                        started = started7;
                                        http = http;
                                        activity2 = activity;
                                        expectedCar = expectedCar3;
                                        host = host2;
                                        e = th59;
                                        str2 = str5;
                                        client = client4;
                                    }
                                } catch (Throwable th60) {
                                    host = host4;
                                    expectedCar = expectedCar2;
                                    started = started7;
                                    http = http;
                                    str2 = str5;
                                    client = client4;
                                    e = th60;
                                    activity2 = activity;
                                }
                            } else {
                                host = host4;
                                expectedCar = expectedCar5;
                                started = started7;
                                http = http;
                                str2 = str5;
                                client = client4;
                                try {
                                    throw new IllegalArgumentException("Failed requirement.".toString());
                                } catch (Throwable th61) {
                                    e = th61;
                                    activity2 = activity3;
                                }
                            }
                        } catch (Throwable th62) {
                            host = host4;
                            expectedCar = expectedCar5;
                            started = started7;
                            http = http;
                            str2 = str5;
                            client = client4;
                            e = th62;
                            activity2 = activity3;
                        }
                    } catch (Throwable th63) {
                        wifiLock = wifiLockCreateWifiLock;
                        events = events2;
                        context = context2;
                        host = host4;
                        expectedCar = expectedCar5;
                        str2 = str5;
                        http = http3;
                        client = client4;
                        started = started6;
                        e = th63;
                        activity2 = activity3;
                    }
                } catch (Throwable th64) {
                    wifiLock = wifiLockCreateWifiLock;
                    events = events2;
                    context = context2;
                    host = host4;
                    expectedCar = expectedCar5;
                    str2 = str5;
                    http = http3;
                    client = client4;
                    started = started6;
                    e = th64;
                    activity2 = activity3;
                }
            } catch (Throwable th65) {
                wifiLock = wifiLockCreateWifiLock;
                events = events2;
                context = context2;
                host = host4;
                expectedCar = expectedCar5;
                str2 = str5;
                http = http3;
                client = client4;
                started = started6;
                e = th65;
                activity2 = activity3;
            }
        } catch (Throwable th66) {
            wifiLock = wifiLockCreateWifiLock;
            events = events2;
            context = context2;
            host = host4;
            expectedCar = expectedCar5;
            str2 = str5;
            http = http3;
            client = client4;
            started = started6;
            e = th66;
        }
        try {
            bench$record(events, started, "failure", e.toString());
            throw e;
        } catch (Throwable th67) {
            UdpControlClient udpControlClient5 = (UdpControlClient) client.element;
            if (udpControlClient5 != null) {
                udpControlClient5.close();
                Unit unit10 = Unit.INSTANCE;
            }
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            if (activity2 != null) {
                activity2.close();
                Unit unit11 = Unit.INSTANCE;
            }
            http.connectionPool().evictAll();
            http.dispatcher().executorService().shutdown();
            JSONObject jSONObjectPut16 = new JSONObject().put("scenario", str2).put("complete", complete).put("elapsedMs", SystemClock.elapsedRealtime() - started).put("host", host).put("carId", expectedCar);
            String strM1208String$arg0$callput$valreport$finally$funbench$classUdpControlDeviceBenchTest2 = "events";
            Intrinsics.checkNotNullExpressionValue(events, "events");
            JSONObject report2 = jSONObjectPut16.put(strM1208String$arg0$callput$valreport$finally$funbench$classUdpControlDeviceBenchTest2, new JSONArray((Collection) CollectionsKt.toList(events)));
            File file2 = new File(context.getFilesDir(), "udp-control-" + str2 + ".json");
            String string4 = report2.toString(2);
            Intrinsics.checkNotNullExpressionValue(string4, str);
            FilesKt.writeText$default(file2, string4, (Charset) null, 2, (Object) null);
            throw th67;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bench$record(List<JSONObject> list, long started, String kind, Object value) {
        list.add(new JSONObject().put("elapsedMs", SystemClock.elapsedRealtime() - started).put("kind", kind).put("value", value));
    }

    /* JADX WARN: Code duplicated, block: B:25:0x013f  */
    private static final JSONObject bench$status(OkHttpClient http, String host, String expectedCar, Bundle args, List<JSONObject> list, long started) throws JSONException {
        Response response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/status").build()).execute();
        try {
            Response it = response;
            Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it.code()));
            ResponseBody responseBodyBody = it.body();
            if (responseBodyBody != null) {
                JSONObject value = new JSONObject(responseBodyBody.string());
                CloseableKt.closeFinally(response, (Throwable) null);
                bench$record(list, started, "status", value);
                Assert.assertEquals(expectedCar, value.getString("carId"));
                Assert.assertEquals("udp-v1", value.getString("controlTransport"));
                Assert.assertEquals(Integer.valueOf(3334), Integer.valueOf(value.getInt("controlPort")));
                String it2 = args.getString("udpBenchRadioBitmap");
                if (it2 != null) {
                    Assert.assertEquals("Wrong radio configuration", Integer.valueOf(Integer.parseInt(it2)), Integer.valueOf(value.getInt("radioProtocolBitmap")));
                }
                Assert.assertFalse("Refusing physical outputs", value.getBoolean("outputsEnabled"));
                JSONObject plan = value.getJSONObject("drivePlan");
                int fi = plan.getInt("fiPermille");
                int bi = plan.getInt("biPermille");
                boolean z = true;
                if (fi <= 250 && 0 <= fi) {
                    if (!(bi <= 250 && 0 <= bi) || (fi != 0 && bi != 0)) {
                        z = false;
                    }
                } else {
                    z = false;
                }
                Assert.assertTrue(z);
                return value;
            }
            throw new IllegalArgumentException("Required value was null.".toString());
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(response, th);
                throw th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean bench$neutral(JSONObject value) throws JSONException {
        if (value.getBoolean("armed")) {
            return false;
        }
        JSONObject it = value.getJSONObject("drivePlan");
        return it.getInt("fiPermille") == 0 && it.getInt("biPermille") == 0 && it.getInt("steeringNormalized") == 0;
    }

    private static final void bench$awaitStatus(OkHttpClient http, String host, String expectedCar, Bundle args, List<JSONObject> list, long started, Function1<? super JSONObject, Boolean> function1) {
        long end = SystemClock.elapsedRealtime() + ((long) 3000);
        while (!((Boolean) function1.invoke(bench$status(http, host, expectedCar, args, list, started))).booleanValue()) {
            SystemClock.sleep(30);
            if (SystemClock.elapsedRealtime() >= end) {
                Assert.fail("Expected drive plan not observed");
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bench$lambda$6(MainActivity it) {
        it.getWindow().addFlags(128);
    }

    private static final UdpControlClient bench$connect(String host, long chip, Ref.ObjectRef<UdpControlClient> objectRef, final Network wifi, final List<JSONObject> list, final long started) {
        UdpControlClient c = new UdpControlClient(host, chip, new Function1<DatagramSocket, Unit>() { // from class: vea.raceremote.UdpControlDeviceBenchTest$bench$connect$c$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(1);
            }

            public /* bridge */ /* synthetic */ Object invoke(Object p1) throws IOException {
                invoke((DatagramSocket) p1);
                return Unit.INSTANCE;
            }

            public final void invoke(DatagramSocket it) throws IOException {
                Intrinsics.checkNotNullParameter(it, "it");
                wifi.bindSocket(it);
            }
        }, new Function1<String, Unit>() { // from class: vea.raceremote.UdpControlDeviceBenchTest$bench$connect$c$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(1);
            }

            public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                invoke((String) p1);
                return Unit.INSTANCE;
            }

            public final void invoke(String it) {
                Intrinsics.checkNotNullParameter(it, "it");
                UdpControlDeviceBenchTest.bench$record(list, started, "client", it);
            }
        }, (Function0) null, 16, (DefaultConstructorMarker) null);
        objectRef.element = c;
        long end = SystemClock.elapsedRealtime() + ((long) 4500);
        while (c.snapshot().getPhase() != UdpControlSession.Phase.ACTIVE && SystemClock.elapsedRealtime() < end) {
            Assert.assertNotEquals("Handshake failed: " + c.snapshot(), UdpControlSession.Phase.CLOSED, c.snapshot().getPhase());
            SystemClock.sleep(5);
        }
        Assert.assertEquals(UdpControlSession.Phase.ACTIVE, c.snapshot().getPhase());
        return c;
    }

    /* JADX INFO: renamed from: vea.raceremote.UdpControlDeviceBenchTest$bench$5, reason: invalid class name */
    /* JADX INFO: compiled from: UdpControlDeviceBenchTest.kt */
    /* synthetic */ class AnonymousClass5 extends FunctionReferenceImpl implements Function1<JSONObject, Boolean> {
        public static final AnonymousClass5 INSTANCE = new AnonymousClass5();

        AnonymousClass5() {
            super(1, Intrinsics.Kotlin.class, "neutral", "bench$neutral(Lorg/json/JSONObject;)Z", 0);
        }

        public final Boolean invoke(JSONObject p0) {
            Intrinsics.checkNotNullParameter(p0, "p0");
            return Boolean.valueOf(UdpControlDeviceBenchTest.bench$neutral(p0));
        }
    }

    private static final UdpControlPacket bench$lambda$11$exchange(DatagramSocket $raw, long chip, List<JSONObject> list, long started, UdpControlPacket p) throws IOException {
        byte[] bytes = p.encode();
        $raw.send(new DatagramPacket(bytes, bytes.length));
        DatagramPacket response = new DatagramPacket(new byte[256], 256);
        $raw.receive(response);
        UdpControlPacket.Companion companion = UdpControlPacket.Companion;
        byte[] data = response.getData();
        Intrinsics.checkNotNullExpressionValue(data, "response.data");
        byte[] bArrCopyOf = Arrays.copyOf(data, response.getLength());
        Intrinsics.checkNotNullExpressionValue(bArrCopyOf, "copyOf(this, newSize)");
        UdpControlPacket it = companion.decode(bArrCopyOf);
        if (it == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Assert.assertEquals(chip, it.getChipId());
        Assert.assertEquals(p.getAttempt(), it.getAttempt());
        bench$record(list, started, "raw_reply", it.toString());
        return it;
    }
}
