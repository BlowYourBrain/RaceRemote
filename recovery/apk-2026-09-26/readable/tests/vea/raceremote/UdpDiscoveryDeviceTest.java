package vea.raceremote;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.compose.ui.test.ActionsKt;
import androidx.compose.ui.test.AssertionsKt;
import androidx.compose.ui.test.FindersKt;
import androidx.compose.ui.test.SemanticsNodeInteraction;
import androidx.compose.ui.test.SemanticsNodeInteractionCollection;
import androidx.compose.ui.test.junit4.AndroidComposeTestRule;
import androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.io.CloseableKt;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
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
import org.junit.Rule;
import org.junit.Test;
import vea.raceremote.app.MainActivity;
import vea.raceremote.control.CarDiscovery;
import vea.raceremote.control.DiscoveredCar;
import vea.raceremote.control.DiscoveryState;

/* JADX INFO: compiled from: UdpDiscoveryDeviceTest.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class UdpDiscoveryDeviceTest {
    public static final int $stable = AndroidComposeTestRule.$stable;
    private final AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> compose = AndroidComposeTestRule_androidKt.createAndroidComposeRule(MainActivity.class);

    @Rule
    public final AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> getCompose() {
        return this.compose;
    }

    /* JADX WARN: Undo finally extract visitor
    jadx.core.utils.exceptions.JadxRuntimeException: Code variable not set in r16v0 ??
        at jadx.core.dex.instructions.args.SSAVar.getCodeVar(SSAVar.java:236)
        at jadx.core.dex.instructions.args.RegisterArg.sameCodeVar(RegisterArg.java:193)
        at jadx.core.dex.visitors.finaly.SameInstructionsStrategyImpl.isSameArgs(SameInstructionsStrategyImpl.java:66)
        at jadx.core.dex.visitors.finaly.SameInstructionsStrategyImpl.sameInsns(SameInstructionsStrategyImpl.java:47)
        at jadx.core.dex.visitors.finaly.traverser.visitors.comparator.InstructionBlockComparatorTraverserVisitor.visit(InstructionBlockComparatorTraverserVisitor.java:152)
        at jadx.core.dex.visitors.finaly.traverser.handlers.InstructionActivePathTraverserHandler.handle(InstructionActivePathTraverserHandler.java:39)
        at jadx.core.dex.visitors.finaly.traverser.handlers.AbstractActivePathTraverserHandler.process(AbstractActivePathTraverserHandler.java:19)
        at jadx.core.dex.visitors.finaly.traverser.TraverserController.processHandlerImplementations(TraverserController.java:43)
        at jadx.core.dex.visitors.finaly.traverser.TraverserController.advance(TraverserController.java:156)
        at jadx.core.dex.visitors.finaly.traverser.TraverserController.process(TraverserController.java:79)
        at jadx.core.dex.visitors.finaly.MarkFinallyVisitor.findCommonInsns(MarkFinallyVisitor.java:404)
        at jadx.core.dex.visitors.finaly.MarkFinallyVisitor.extractFinally(MarkFinallyVisitor.java:284)
        at jadx.core.dex.visitors.finaly.MarkFinallyVisitor.processTryBlock(MarkFinallyVisitor.java:202)
        at jadx.core.dex.visitors.finaly.MarkFinallyVisitor.visit(MarkFinallyVisitor.java:135)
     */
    @Test
    public final void searchSelectWithoutArmThenConnectAndReportBusy() throws JSONException {
        Object element$iv;
        JSONArray events;
        OkHttpClient http;
        long started;
        CarDiscovery observer;
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue(Intrinsics.areEqual(args.getString("driveBench"), "disabled-outputs"));
        String host = args.getString("driveBenchHost");
        if (host == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(host, "requireNotNull(args.getString(\"driveBenchHost\"))");
        String carId = args.getString("driveBenchCarId");
        if (carId == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(carId, "requireNotNull(args.getString(\"driveBenchCarId\"))");
        MainActivity context = getActivity();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        Object[] allNetworks = cm.getAllNetworks();
        Intrinsics.checkNotNullExpressionValue(allNetworks, "cm.allNetworks");
        Object[] $this$firstOrNull$iv = allNetworks;
        int length = $this$firstOrNull$iv.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                element$iv = null;
                break;
            }
            element$iv = $this$firstOrNull$iv[i];
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities((Network) element$iv);
            if (networkCapabilities != null && networkCapabilities.hasTransport(1) == true) {
                break;
            } else {
                i++;
            }
        }
        if (element$iv == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        final Network wifi = (Network) element$iv;
        OkHttpClient http2 = new OkHttpClient.Builder().socketFactory(wifi.getSocketFactory()).connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS).callTimeout(2, TimeUnit.SECONDS).build();
        JSONArray events2 = new JSONArray();
        long started2 = SystemClock.elapsedRealtime();
        final CarDiscovery observer2 = new CarDiscovery();
        boolean complete = false;
        try {
            JSONObject initial = searchSelectWithoutArmThenConnectAndReportBusy$status(http2, host, carId, events2, started2);
            Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$neutral(initial));
            String string = initial.getString("chipId");
            Intrinsics.checkNotNullExpressionValue(string, "initial.getString(\"chipId\")");
            final long chip = Long.parseLong(string, CharsKt.checkRadix(16));
            String before = searchSelectWithoutArmThenConnectAndReportBusy$get(http2, host, "debug/udp-trace").getJSONArray("events").toString();
            Intrinsics.checkNotNullExpressionValue(before, "get(\"debug/udp-trace\").g…rray(\"events\").toString()");
            ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "find_cars", false, 2, null));
            waitUntil(5000, new Function0<Boolean>() { // from class: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy.1
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super(0);
                }

                /* JADX INFO: renamed from: invoke, reason: merged with bridge method [inline-methods] */
                public final Boolean m1544invoke() {
                    return Boolean.valueOf(SemanticsNodeInteractionCollection.fetchSemanticsNodes$default(FindersKt.onAllNodesWithTag$default(UdpDiscoveryDeviceTest.this.getCompose(), new StringBuilder().append("discovered_").append(chip).toString(), false, 2, null), false, null, 3, null).size() == 1);
                }
            });
            events = events2;
            try {
                Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$neutral(searchSelectWithoutArmThenConnectAndReportBusy$status(http2, host, carId, events2, started2)));
                ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "discovered_" + chip, false, 2, null));
                AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default(this.compose, "car_address", false, 2, null), host, false, false, 6, null);
                SemanticsNodeInteraction semanticsNodeInteractionOnNodeWithTag$default = FindersKt.onNodeWithTag$default(this.compose, "selected_car_identity", false, 2, null);
                String string2 = Long.toString(chip, CharsKt.checkRadix(16));
                Intrinsics.checkNotNullExpressionValue(string2, "toString(this, checkRadix(radix))");
                AssertionsKt.assertTextContains$default(semanticsNodeInteractionOnNodeWithTag$default, StringsKt.padStart(string2, 12, '0'), true, false, 4, null);
                AssertionsKt.assertIsNotEnabled(FindersKt.onNodeWithTag$default(this.compose, "throttle", false, 2, null));
                Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$neutral(searchSelectWithoutArmThenConnectAndReportBusy$status(http2, host, carId, events, started2)));
                try {
                    Assert.assertEquals("Discovery and selection must not send control", before, searchSelectWithoutArmThenConnectAndReportBusy$get(http2, host, "debug/udp-trace").getJSONArray("events").toString());
                    try {
                        searchSelectWithoutArmThenConnectAndReportBusy$record(events, started2, "passive_discovery_and_selection", Boolean.valueOf(true));
                        ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "connect_car", false, 2, null));
                        waitUntil(5000, new Function0<Boolean>() { // from class: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy.2
                            {
                                super(0);
                            }

                            /* JADX INFO: renamed from: invoke, reason: merged with bridge method [inline-methods] */
                            public final Boolean m1545invoke() {
                                Object obj;
                                UdpDiscoveryDeviceTest $this$invoke_u24lambda_u240 = UdpDiscoveryDeviceTest.this;
                                try {
                                    Result.Companion companion = Result.Companion;
                                    obj = Result.constructor-impl(AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default($this$invoke_u24lambda_u240.getCompose(), "connection_status", false, 2, null), "Управление подключено", false, false, 6, null));
                                } catch (Throwable th) {
                                    Result.Companion companion2 = Result.Companion;
                                    obj = Result.constructor-impl(ResultKt.createFailure(th));
                                }
                                return Boolean.valueOf(Result.isSuccess-impl(obj));
                            }
                        });
                        http = http2;
                        try {
                            Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$status(http2, host, carId, events, started2).getBoolean("armed"));
                            AssertionsKt.assertIsNotEnabled(FindersKt.onNodeWithTag$default(this.compose, "find_cars", false, 2, null));
                            try {
                                CarDiscovery.search$default(observer2, (List) null, new Function1<DatagramSocket, Unit>() { // from class: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy.3
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
                                }, 1, (Object) null);
                                waitUntil(5000, new Function0<Boolean>() { // from class: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy.4
                                    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                                    {
                                        super(0);
                                    }

                                    /* JADX INFO: renamed from: invoke, reason: merged with bridge method [inline-methods] */
                                    public final Boolean m1546invoke() {
                                        return Boolean.valueOf(!((DiscoveryState) observer2.getState().getValue()).getSearching());
                                    }
                                });
                                Iterable $this$single$iv = ((DiscoveryState) observer2.getState().getValue()).getCars();
                                Object single$iv = null;
                                boolean found$iv = false;
                                for (Object element$iv2 : $this$single$iv) {
                                    try {
                                        DiscoveredCar it = (DiscoveredCar) element$iv2;
                                        if (it.getChipId() == chip && Intrinsics.areEqual(it.getHost(), host)) {
                                            if (found$iv) {
                                                throw new IllegalArgumentException("Collection contains more than one matching element.");
                                            }
                                            single$iv = element$iv2;
                                            found$iv = true;
                                        }
                                    } catch (Throwable th) {
                                        error = th;
                                        observer = observer2;
                                        started = started2;
                                    }
                                }
                                if (found$iv) {
                                    try {
                                        DiscoveredCar discovered = (DiscoveredCar) single$iv;
                                        Assert.assertTrue(discovered.getBusy());
                                        Assert.assertEquals("udp-v1", discovered.getTransport());
                                        Assert.assertEquals(Integer.valueOf(3334), Integer.valueOf(discovered.getPort()));
                                        Assert.assertEquals(Integer.valueOf(2), Integer.valueOf(discovered.getDiscoveryVersion()));
                                        try {
                                            searchSelectWithoutArmThenConnectAndReportBusy$record(events, started2, "busy_search", discovered.toString());
                                            try {
                                                AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default(this.compose, "connection_status", false, 2, null), "Управление подключено", false, false, 6, null);
                                                observer = observer2;
                                                try {
                                                    Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$status(http, host, carId, events, started2).getBoolean("armed"));
                                                    ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "stop_car", false, 2, null));
                                                    long until = SystemClock.elapsedRealtime() + ((long) 3000);
                                                    while (!searchSelectWithoutArmThenConnectAndReportBusy$neutral(searchSelectWithoutArmThenConnectAndReportBusy$status(http, host, carId, events, started2))) {
                                                        try {
                                                            if (SystemClock.elapsedRealtime() >= until) {
                                                                break;
                                                            } else {
                                                                SystemClock.sleep(30);
                                                            }
                                                        } catch (Throwable th2) {
                                                            error = th2;
                                                            started = started2;
                                                        }
                                                    }
                                                    Assert.assertTrue(searchSelectWithoutArmThenConnectAndReportBusy$neutral(searchSelectWithoutArmThenConnectAndReportBusy$status(http, host, carId, events, started2)));
                                                    JSONObject trace = searchSelectWithoutArmThenConnectAndReportBusy$get(http, host, "debug/udp-trace");
                                                    try {
                                                        searchSelectWithoutArmThenConnectAndReportBusy$record(events, started2, "control_trace", trace);
                                                        Assert.assertTrue(trace.has("maxDiscoveryUs"));
                                                        boolean complete2 = true;
                                                        observer.close();
                                                        try {
                                                            ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.STARTED);
                                                            File file = new File(context.getFilesDir(), "udp-discovery-device.json");
                                                            String string3 = new JSONObject().put("complete", complete2).put("host", host).put("elapsedMs", SystemClock.elapsedRealtime() - started2).put("events", events).toString(2);
                                                            Intrinsics.checkNotNullExpressionValue(string3, "JSONObject().put(\"comple…nts\", events).toString(2)");
                                                            FilesKt.writeText$default(file, string3, (Charset) null, 2, (Object) null);
                                                            http.connectionPool().evictAll();
                                                            http.dispatcher().executorService().shutdown();
                                                            return;
                                                        } catch (Throwable th3) {
                                                            File file2 = new File(context.getFilesDir(), "udp-discovery-device.json");
                                                            String string4 = new JSONObject().put("complete", complete2).put("host", host).put("elapsedMs", SystemClock.elapsedRealtime() - started2).put("events", events).toString(2);
                                                            Intrinsics.checkNotNullExpressionValue(string4, "JSONObject().put(\"comple…nts\", events).toString(2)");
                                                            FilesKt.writeText$default(file2, string4, (Charset) null, 2, (Object) null);
                                                            http.connectionPool().evictAll();
                                                            http.dispatcher().executorService().shutdown();
                                                            throw th3;
                                                        }
                                                    } catch (Throwable th4) {
                                                        error = th4;
                                                        started = started2;
                                                    }
                                                } catch (Throwable th5) {
                                                    error = th5;
                                                    started = started2;
                                                }
                                            } catch (Throwable th6) {
                                                error = th6;
                                                observer = observer2;
                                                started = started2;
                                            }
                                        } catch (Throwable th7) {
                                            error = th7;
                                            observer = observer2;
                                            started = started2;
                                        }
                                    } catch (Throwable th8) {
                                        error = th8;
                                        observer = observer2;
                                        started = started2;
                                    }
                                } else {
                                    observer = observer2;
                                    started = started2;
                                    try {
                                        throw new NoSuchElementException("Collection contains no element matching the predicate.");
                                    } catch (Throwable th9) {
                                        error = th9;
                                    }
                                }
                            } catch (Throwable th10) {
                                error = th10;
                                observer = observer2;
                                started = started2;
                            }
                        } catch (Throwable th11) {
                            error = th11;
                            observer = observer2;
                            started = started2;
                        }
                    } catch (Throwable th12) {
                        error = th12;
                        http = http2;
                        observer = observer2;
                        started = started2;
                    }
                } catch (Throwable th13) {
                    error = th13;
                    http = http2;
                    started = started2;
                    observer = observer2;
                }
            } catch (Throwable th14) {
                error = th14;
                http = http2;
                started = started2;
                observer = observer2;
            }
        } catch (Throwable th15) {
            error = th15;
            events = events2;
            http = http2;
            started = started2;
            observer = observer2;
        }
        try {
            searchSelectWithoutArmThenConnectAndReportBusy$record(events, started, "failure", error.toString());
            throw error;
        } catch (Throwable error) {
            observer.close();
            try {
                ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.STARTED);
                File file3 = new File(context.getFilesDir(), "udp-discovery-device.json");
                String string5 = new JSONObject().put("complete", complete).put("host", host).put("elapsedMs", SystemClock.elapsedRealtime() - started).put("events", events).toString(2);
                Intrinsics.checkNotNullExpressionValue(string5, "JSONObject().put(\"comple…nts\", events).toString(2)");
                FilesKt.writeText$default(file3, string5, (Charset) null, 2, (Object) null);
                http.connectionPool().evictAll();
                http.dispatcher().executorService().shutdown();
                throw error;
            } catch (Throwable th16) {
                File file4 = new File(context.getFilesDir(), "udp-discovery-device.json");
                String string6 = new JSONObject().put("complete", complete).put("host",
                /*  JADX ERROR: Method code generation error
                    jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0692: INVOKE (r9v13 'string6' java.lang.String) =
                      (wrap org.json.JSONObject:0x0688: INVOKE
                      (wrap org.json.JSONObject:0x067e: INVOKE
                      (wrap org.json.JSONObject:0x066e: INVOKE
                      (wrap org.json.JSONObject:0x0662: INVOKE
                      (wrap org.json.JSONObject:0x0659: CONSTRUCTOR  A[MD:():void (c), WRAPPED] call: org.json.JSONObject.<init>():void type: CONSTRUCTOR)
                      (wrap java.lang.String:0x065e: INVOKE
                      (wrap vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt:0x065c: SGET  A[WRAPPED] vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.INSTANCE vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt)
                     VIRTUAL call: vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.String$arg-0$call-put$$this$call-put$$this$call-put$$this$call-put$$this$call-toString$arg-0$call-writeText$finally$finally$fun-searchSelectWithoutArmThenConnectAndReportBusy$class-UdpDiscoveryDeviceTest():java.lang.String A[MD:():java.lang.String (m), WRAPPED])
                      (r7v1 'complete' boolean A[D('complete' boolean)])
                     VIRTUAL call: org.json.JSONObject.put(java.lang.String, boolean):org.json.JSONObject A[MD:(java.lang.String, boolean):org.json.JSONObject throws org.json.JSONException (c), WRAPPED])
                      (wrap java.lang.String:0x0668: INVOKE
                      (wrap vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt:0x0666: SGET  A[WRAPPED] (LINE:102) vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.INSTANCE vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt)
                     VIRTUAL call: vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.String$arg-0$call-put$$this$call-put$$this$call-put$$this$call-toString$arg-0$call-writeText$finally$finally$fun-searchSelectWithoutArmThenConnectAndReportBusy$class-UdpDiscoveryDeviceTest():java.lang.String A[MD:():java.lang.String (m), WRAPPED] (LINE:102))
                      (r5v1 java.lang.Object A[D('host' java.lang.String)])
                     VIRTUAL call: org.json.JSONObject.put(java.lang.String, java.lang.Object):org.json.JSONObject A[MD:(java.lang.String, java.lang.Object):org.json.JSONObject throws org.json.JSONException (c), WRAPPED] (LINE:102))
                      (wrap java.lang.String:0x0674: INVOKE
                      (wrap vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt:0x0672: SGET  A[WRAPPED] vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.INSTANCE vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt)
                     VIRTUAL call: vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.String$arg-0$call-put$$this$call-put$$this$call-toString$arg-0$call-writeText$finally$finally$fun-searchSelectWithoutArmThenConnectAndReportBusy$class-UdpDiscoveryDeviceTest():java.lang.String A[MD:():java.lang.String (m), WRAPPED])
                      (wrap long:0x067c: ARITH (wrap long:0x0678: INVOKE  STATIC call: android.os.SystemClock.elapsedRealtime():long A[MD:():long (c), WRAPPED]) - (r5v3 'started' long A[D('started' long)]) A[WRAPPED])
                     VIRTUAL call: org.json.JSONObject.put(java.lang.String, long):org.json.JSONObject A[MD:(java.lang.String, long):org.json.JSONObject throws org.json.JSONException (c), WRAPPED] (LINE:102))
                      (wrap java.lang.String:0x0684: INVOKE
                      (wrap vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt:0x0682: SGET  A[WRAPPED] vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.INSTANCE vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt)
                     VIRTUAL call: vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.String$arg-0$call-put$$this$call-toString$arg-0$call-writeText$finally$finally$fun-searchSelectWithoutArmThenConnectAndReportBusy$class-UdpDiscoveryDeviceTest():java.lang.String A[MD:():java.lang.String (m), WRAPPED])
                      (r3v3 'events' org.json.JSONArray A[D('events' org.json.JSONArray)])
                     VIRTUAL call: org.json.JSONObject.put(java.lang.String, java.lang.Object):org.json.JSONObject A[MD:(java.lang.String, java.lang.Object):org.json.JSONObject throws org.json.JSONException (c), WRAPPED] (LINE:102))
                      (wrap int:0x068e: INVOKE
                      (wrap vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt:0x068c: SGET  A[WRAPPED] vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.INSTANCE vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt)
                     VIRTUAL call: vea.raceremote.LiveLiterals$UdpDiscoveryDeviceTestKt.Int$arg-0$call-toString$arg-0$call-writeText$finally$finally$fun-searchSelectWithoutArmThenConnectAndReportBusy$class-UdpDiscoveryDeviceTest():int A[MD:():int (m), WRAPPED])
                     VIRTUAL call: org.json.JSONObject.toString(int):java.lang.String A[DECLARE_VAR, MD:(int):java.lang.String throws org.json.JSONException (c)] (LINE:102) in method: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy():void, file: classes4.dex
                        at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:310)
                        at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:273)
                        at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:94)
                        at jadx.core.dex.nodes.IBlock.generate(IBlock.java:15)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:83)
                        at jadx.core.codegen.RegionGen.makeCatchBlock(RegionGen.java:383)
                        at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:335)
                        at jadx.core.dex.regions.TryCatchRegion.generate(TryCatchRegion.java:85)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:83)
                        at jadx.core.codegen.RegionGen.makeCatchBlock(RegionGen.java:383)
                        at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:335)
                        at jadx.core.dex.regions.TryCatchRegion.generate(TryCatchRegion.java:85)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.dex.regions.Region.generate(Region.java:35)
                        at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:291)
                        at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:270)
                        at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:420)
                        at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:345)
                        at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$3(ClassGen.java:299)
                        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(Unknown Source)
                        at java.base/java.util.ArrayList.forEach(Unknown Source)
                        at java.base/java.util.stream.SortedOps$RefSortingSink.end(Unknown Source)
                        at java.base/java.util.stream.Sink$ChainedReference.end(Unknown Source)
                        at java.base/java.util.stream.ReferencePipeline$7$1FlatMap.end(Unknown Source)
                        at java.base/java.util.stream.AbstractPipeline.copyInto(Unknown Source)
                        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(Unknown Source)
                        at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(Unknown Source)
                        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(Unknown Source)
                        at java.base/java.util.stream.AbstractPipeline.evaluate(Unknown Source)
                        at java.base/java.util.stream.ReferencePipeline.forEach(Unknown Source)
                        at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:295)
                        at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:284)
                        at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:268)
                        at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:160)
                        at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:104)
                        at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:45)
                        at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:34)
                        at jadx.core.codegen.CodeGen.generate(CodeGen.java:22)
                        at jadx.core.ProcessClass.process(ProcessClass.java:89)
                        at jadx.core.ProcessClass.generateCode(ProcessClass.java:127)
                        at jadx.core.dex.nodes.ClassNode.generateClassCode(ClassNode.java:405)
                        at jadx.core.dex.nodes.ClassNode.decompile(ClassNode.java:393)
                        at jadx.core.dex.nodes.ClassNode.getCode(ClassNode.java:343)
                    Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Code variable not set in r5v1 java.lang.Object
                        at jadx.core.dex.instructions.args.SSAVar.getCodeVar(SSAVar.java:236)
                    */
                /*
                    Method dump skipped, instruction units count: 1743
                    To view this dump change 'Code comments level' option to 'DEBUG'
                */
                throw new UnsupportedOperationException("Method not decompiled: vea.raceremote.UdpDiscoveryDeviceTest.searchSelectWithoutArmThenConnectAndReportBusy():void");
            }

            private static final void searchSelectWithoutArmThenConnectAndReportBusy$record(JSONArray events, long started, String kind, Object value) {
                events.put(new JSONObject().put("elapsedMs", SystemClock.elapsedRealtime() - started).put("kind", kind).put("value", value));
            }

            private static final JSONObject searchSelectWithoutArmThenConnectAndReportBusy$get(OkHttpClient http, String host, String path) {
                Response response = (Closeable) http.newCall(new Request.Builder().url("http://" + host + "/" + path).build()).execute();
                try {
                    Response it = response;
                    Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(it.code()));
                    ResponseBody responseBodyBody = it.body();
                    if (responseBodyBody != null) {
                        JSONObject jSONObject = new JSONObject(responseBodyBody.string());
                        CloseableKt.closeFinally(response, (Throwable) null);
                        return jSONObject;
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

            private static final JSONObject searchSelectWithoutArmThenConnectAndReportBusy$status(OkHttpClient http, String host, String carId, JSONArray events, long started) {
                JSONObject it = searchSelectWithoutArmThenConnectAndReportBusy$get(http, host, "status");
                Assert.assertEquals(carId, it.getString("carId"));
                Assert.assertFalse(it.getBoolean("outputsEnabled"));
                Assert.assertEquals("udp-v1", it.getString("controlTransport"));
                searchSelectWithoutArmThenConnectAndReportBusy$record(events, started, "status", it);
                return it;
            }

            private static final boolean searchSelectWithoutArmThenConnectAndReportBusy$neutral(JSONObject s) throws JSONException {
                if (s.getBoolean("armed")) {
                    return false;
                }
                JSONObject it = s.getJSONObject("drivePlan");
                return it.getInt("fiPermille") == 0 && it.getInt("biPermille") == 0 && it.getInt("steeringNormalized") == 0;
            }
        }
