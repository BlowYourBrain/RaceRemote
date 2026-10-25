package vea.raceremote;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.compose.ui.geometry.Offset;
import androidx.compose.ui.geometry.OffsetKt;
import androidx.compose.ui.geometry.Rect;
import androidx.compose.ui.test.ActionsKt;
import androidx.compose.ui.test.AssertionsKt;
import androidx.compose.ui.test.FindersKt;
import androidx.compose.ui.test.SemanticsNodeInteraction;
import androidx.compose.ui.test.TextActionsKt;
import androidx.compose.ui.test.TouchInjectionScope;
import androidx.compose.ui.test.junit4.AndroidComposeTestRule;
import androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.IntIterator;
import kotlin.io.CloseableKt;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import vea.raceremote.app.MainActivity;

/* JADX INFO: compiled from: UdpControlScreenDeviceTest.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class UdpControlScreenDeviceTest {
    public static final int $stable = AndroidComposeTestRule.$stable;
    private final AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> compose = AndroidComposeTestRule_androidKt.createAndroidComposeRule(MainActivity.class);

    @Rule
    public final AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> getCompose() {
        return this.compose;
    }

    @Test
    public final void controlsVideoFailureBackgroundStopAndExplicitReconnect() throws JSONException, IOException {
        Object element$iv;
        boolean complete;
        JSONArray events;
        final MainActivity context;
        MockWebServer video;
        long started;
        File file;
        String string;
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue(Intrinsics.areEqual(args.getString("driveBench"), "disabled-outputs"));
        String host = args.getString("driveBenchHost");
        if (host == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(host, "requireNotNull(args.getString(\"driveBenchHost\"))");
        String expectedCar = args.getString("driveBenchCarId");
        if (expectedCar == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Intrinsics.checkNotNullExpressionValue(expectedCar, "requireNotNull(args.getString(\"driveBenchCarId\"))");
        MainActivity context2 = getActivity();
        ConnectivityManager cm = (ConnectivityManager) context2.getSystemService(ConnectivityManager.class);
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
            Network it = (Network) element$iv;
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(it);
            if (networkCapabilities != null && networkCapabilities.hasTransport(1) == true) {
                break;
            } else {
                i++;
            }
        }
        if (element$iv == null) {
            throw new IllegalArgumentException("Required value was null.".toString());
        }
        Network wifi = (Network) element$iv;
        OkHttpClient http = new OkHttpClient.Builder().socketFactory(wifi.getSocketFactory()).connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS).callTimeout(2, TimeUnit.SECONDS).build();
        JSONArray events2 = new JSONArray();
        long started2 = SystemClock.elapsedRealtime();
        boolean complete2 = false;
        MockWebServer video2 = new MockWebServer();
        video2.enqueue(new MockResponse().setResponseCode(503));
        video2.start();
        try {
            Assert.assertTrue(controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(controlsVideoFailureBackgroundStopAndExplicitReconnect$status(http, host, expectedCar, events2, started2)));
            TextActionsKt.performTextReplacement(FindersKt.onNodeWithTag$default(this.compose, "car_address", false, 2, null), host);
            ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "connect_car", false, 2, null));
            controlsVideoFailureBackgroundStopAndExplicitReconnect$connected(this);
            complete = complete2;
            context = context2;
            try {
                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.1
                    public final Boolean invoke(JSONObject it2) {
                        Intrinsics.checkNotNullParameter(it2, "it");
                        return Boolean.valueOf(it2.getBoolean("armed") && it2.getInt("throttle") == 0 && it2.getInt("steering") == 0);
                    }
                });
                ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "show_video", false, 2, null));
                SemanticsNodeInteraction semanticsNodeInteractionOnNodeWithTag$default = FindersKt.onNodeWithTag$default(this.compose, "video_address", false, 2, null);
                try {
                    String string2 = video2.url("/stream").toString();
                    Intrinsics.checkNotNullExpressionValue(string2, "video.url(\"/stream\").toString()");
                    TextActionsKt.performTextReplacement(semanticsNodeInteractionOnNodeWithTag$default, string2);
                    ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "toggle_video", false, 2, null));
                    waitUntil(5000, new Function0<Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.2
                        {
                            super(0);
                        }

                        /* JADX INFO: renamed from: invoke, reason: merged with bridge method [inline-methods] */
                        public final Boolean m1541invoke() {
                            Object obj;
                            UdpControlScreenDeviceTest $this$invoke_u24lambda_u240 = UdpControlScreenDeviceTest.this;
                            try {
                                Result.Companion companion = Result.Companion;
                                obj = Result.constructor-impl(AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default($this$invoke_u24lambda_u240.getCompose(), "video_status", false, 2, null), "Видео недоступно", true, false, 4, null));
                            } catch (Throwable th) {
                                Result.Companion companion2 = Result.Companion;
                                obj = Result.constructor-impl(ResultKt.createFailure(th));
                            }
                            return Boolean.valueOf(Result.isSuccess-impl(obj));
                        }
                    });
                    controlsVideoFailureBackgroundStopAndExplicitReconnect$connected(this);
                    Assert.assertTrue(controlsVideoFailureBackgroundStopAndExplicitReconnect$status(http, host, expectedCar, events2, started2).getBoolean("armed"));
                    try {
                        controlsVideoFailureBackgroundStopAndExplicitReconnect$record(events2, started2, "video_failure_control_preserved", Boolean.valueOf(true));
                        try {
                            runOnIdle(new Function0<Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.3
                                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                                {
                                    super(0);
                                }

                                public final Unit invoke() {
                                    View currentFocus = context.getCurrentFocus();
                                    if (currentFocus != null) {
                                        currentFocus.clearFocus();
                                    }
                                    WindowInsetsController insetsController = context.getWindow().getInsetsController();
                                    if (insetsController == null) {
                                        return null;
                                    }
                                    insetsController.hide(WindowInsets.Type.ime());
                                    return Unit.INSTANCE;
                                }
                            });
                            ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "driving_mode", false, 2, null));
                            final Rect gas = SemanticsNodeInteraction.fetchSemanticsNode$default(FindersKt.onNodeWithTag$default(this.compose, "drive_throttle", false, 2, null), null, 1, null).getBoundsInRoot();
                            final Rect wheel = SemanticsNodeInteraction.fetchSemanticsNode$default(FindersKt.onNodeWithTag$default(this.compose, "drive_steering", false, 2, null), null, 1, null).getBoundsInRoot();
                            ActionsKt.performTouchInput(FindersKt.onRoot$default(this.compose, false, 1, null), new Function1<TouchInjectionScope, Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.4
                                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                                {
                                    super(1);
                                }

                                public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                                    invoke((TouchInjectionScope) p1);
                                    return Unit.INSTANCE;
                                }

                                public final void invoke(TouchInjectionScope $this$performTouchInput) {
                                    Intrinsics.checkNotNullParameter($this$performTouchInput, "$this$performTouchInput");
                                    $this$performTouchInput.mo352downUv8p0NA(0, gas.getCenter-F1C5BW0());
                                    $this$performTouchInput.mo352downUv8p0NA(1, wheel.getCenter-F1C5BW0());
                                    TouchInjectionScope.m350moveTod4ec7I$default($this$performTouchInput, 0, Offset.plus-MK-Hz9U(gas.getCenter-F1C5BW0(), OffsetKt.Offset(0.0f, (-gas.getHeight()) * 0.3f)), 0L, 4, null);
                                    TouchInjectionScope.m350moveTod4ec7I$default($this$performTouchInput, 1, Offset.plus-MK-Hz9U(wheel.getCenter-F1C5BW0(), OffsetKt.Offset(wheel.getWidth() * 0.3f, 0.0f)), 0L, 4, null);
                                }
                            });
                            context = context;
                            video = video2;
                            try {
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.5
                                    public final Boolean invoke(JSONObject it2) {
                                        Intrinsics.checkNotNullParameter(it2, "it");
                                        return Boolean.valueOf(it2.getInt("throttle") > 200 && it2.getInt("steering") > 200 && it2.getJSONObject("drivePlan").getInt("fiPermille") > 0);
                                    }
                                });
                                ActionsKt.performTouchInput(FindersKt.onRoot$default(this.compose, false, 1, null), new Function1<TouchInjectionScope, Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.6
                                    public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                                        invoke((TouchInjectionScope) p1);
                                        return Unit.INSTANCE;
                                    }

                                    public final void invoke(TouchInjectionScope $this$performTouchInput) {
                                        Intrinsics.checkNotNullParameter($this$performTouchInput, "$this$performTouchInput");
                                        $this$performTouchInput.up(0);
                                    }
                                });
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.7
                                    public final Boolean invoke(JSONObject it2) {
                                        Intrinsics.checkNotNullParameter(it2, "it");
                                        return Boolean.valueOf(it2.getInt("throttle") == 0 && it2.getInt("steering") > 200);
                                    }
                                });
                                ActionsKt.performTouchInput(FindersKt.onRoot$default(this.compose, false, 1, null), new Function1<TouchInjectionScope, Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.8
                                    public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                                        invoke((TouchInjectionScope) p1);
                                        return Unit.INSTANCE;
                                    }

                                    public final void invoke(TouchInjectionScope $this$performTouchInput) {
                                        Intrinsics.checkNotNullParameter($this$performTouchInput, "$this$performTouchInput");
                                        TouchInjectionScope.cancel$default($this$performTouchInput, 0L, 1, null);
                                    }
                                });
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.9
                                    public final Boolean invoke(JSONObject it2) {
                                        Intrinsics.checkNotNullParameter(it2, "it");
                                        return Boolean.valueOf(it2.getInt("throttle") == 0 && it2.getInt("steering") == 0);
                                    }
                                });
                                ActionsKt.performTouchInput(FindersKt.onNodeWithTag$default(this.compose, "drive_throttle", false, 2, null), new Function1<TouchInjectionScope, Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.10
                                    public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                                        invoke((TouchInjectionScope) p1);
                                        return Unit.INSTANCE;
                                    }

                                    public final void invoke(TouchInjectionScope $this$performTouchInput) {
                                        Intrinsics.checkNotNullParameter($this$performTouchInput, "$this$performTouchInput");
                                        $this$performTouchInput.m353downk4lQ0M($this$performTouchInput.mo96getCenterF1C5BW0());
                                        TouchInjectionScope.m349moveTo3MmeM6k$default($this$performTouchInput, Offset.plus-MK-Hz9U($this$performTouchInput.mo96getCenterF1C5BW0(), OffsetKt.Offset(0.0f, $this$performTouchInput.getHeight() * 0.3f)), 0L, 2, null);
                                    }
                                });
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.11
                                    public final Boolean invoke(JSONObject it2) {
                                        Intrinsics.checkNotNullParameter(it2, "it");
                                        return Boolean.valueOf(it2.getInt("throttle") < -200 && it2.getJSONObject("drivePlan").getInt("biPermille") > 0);
                                    }
                                });
                                ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.STARTED);
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, AnonymousClass12.INSTANCE);
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$trace(http, host, events2, started2, "background_trace");
                                ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.RESUMED);
                                AssertionsKt.assertIsNotEnabled(FindersKt.onNodeWithTag$default(this.compose, "drive_throttle", false, 2, null));
                                AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default(this.compose, "driving_connection_status", false, 2, null), "остановлено", true, false, 4, null);
                                ActionsKt.performTouchInput(FindersKt.onNodeWithTag$default(this.compose, "drive_throttle", false, 2, null), new Function1<TouchInjectionScope, Unit>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.13
                                    public /* bridge */ /* synthetic */ Object invoke(Object p1) {
                                        invoke((TouchInjectionScope) p1);
                                        return Unit.INSTANCE;
                                    }

                                    public final void invoke(TouchInjectionScope $this$performTouchInput) {
                                        Intrinsics.checkNotNullParameter($this$performTouchInput, "$this$performTouchInput");
                                        TouchInjectionScope.cancel$default($this$performTouchInput, 0L, 1, null);
                                    }
                                });
                                Assert.assertTrue(controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(controlsVideoFailureBackgroundStopAndExplicitReconnect$status(http, host, expectedCar, events2, started2)));
                                ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "driving_mode", false, 2, null));
                                ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "connect_car", false, 2, null));
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$connected(this);
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, new Function1<JSONObject, Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect.14
                                    public final Boolean invoke(JSONObject it2) {
                                        Intrinsics.checkNotNullParameter(it2, "it");
                                        return Boolean.valueOf(it2.getBoolean("armed") && it2.getInt("throttle") == 0 && it2.getInt("steering") == 0);
                                    }
                                });
                                ActionsKt.performClick(FindersKt.onNodeWithTag$default(this.compose, "stop_car", false, 2, null));
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(http, host, expectedCar, events2, started2, AnonymousClass15.INSTANCE);
                                controlsVideoFailureBackgroundStopAndExplicitReconnect$trace(http, host, events2, started2, "button_stop_trace");
                                AssertionsKt.assertIsNotEnabled(FindersKt.onNodeWithTag$default(this.compose, "throttle", false, 2, null));
                                TextActionsKt.performTextReplacement(FindersKt.onNodeWithTag$default(this.compose, "car_address", false, 2, null), "192.168.1.123");
                                AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default(this.compose, "video_address", false, 2, null), "http://192.168.1.123:81/stream", false, false, 6, null);
                                AssertionsKt.assertTextContains$default(FindersKt.onNodeWithTag$default(this.compose, "video_status", false, 2, null), "Видео выключено", false, false, 6, null);
                                Assert.assertTrue(controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(controlsVideoFailureBackgroundStopAndExplicitReconnect$status(http, host, expectedCar, events2, started2)));
                                started = started2;
                                events = events2;
                                try {
                                    controlsVideoFailureBackgroundStopAndExplicitReconnect$record(events, started, "explicit_reconnect_and_target_change", Boolean.valueOf(true));
                                    boolean complete3 = true;
                                    try {
                                        ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.STARTED);
                                    } finally {
                                        File file2 = new File(context.getFilesDir(), "udp-screen-device.json");
                                        String string3 = new JSONObject().put("complete", complete3).put("host", host).put("elapsedMs", SystemClock.elapsedRealtime() - started).put("events", events).toString(2);
                                        Intrinsics.checkNotNullExpressionValue(string3, "JSONObject().put(\"comple…nts\", events).toString(2)");
                                        FilesKt.writeText$default(file2, string3, (Charset) null, 2, (Object) null);
                                        video.shutdown();
                                        http.connectionPool().evictAll();
                                        http.dispatcher().executorService().shutdown();
                                    }
                                } catch (Throwable th) {
                                    error = th;
                                    try {
                                        controlsVideoFailureBackgroundStopAndExplicitReconnect$record(events, started, "failure", error.toString());
                                        throw error;
                                    } catch (Throwable error) {
                                        try {
                                            ((ActivityScenarioRule) getActivityRule()).getScenario().moveToState(Lifecycle.State.STARTED);
                                            file = new File(context.getFilesDir(), "udp-screen-device.json");
                                            JSONObject jSONObject = new JSONObject();
                                            String strM1352String$arg0$callput$$this$callput$$this$callput$$this$callput$$this$calltoString$arg0$callwriteText$finally$finally$funcontrolsVideoFailureBackgroundStopAndExplicitReconnect$classUdpControlScreenDeviceTest = "complete";
                                            boolean complete4 = complete;
                                            throw error;
                                        } finally {
                                            file = new File(context.getFilesDir(), "udp-screen-device.json");
                                            string = new JSONObject().put("complete", complete).put("host", host).put("elapsedMs", SystemClock.elapsedRealtime() - started).put("events", events).toString(2);
                                            Intrinsics.checkNotNullExpressionValue(string, "JSONObject().put(\"comple…nts\", events).toString(2)");
                                            FilesKt.writeText$default(file, string, (Charset) null, 2, (Object) null);
                                            video.shutdown();
                                            http.connectionPool().evictAll();
                                            http.dispatcher().executorService().shutdown();
                                        }
                                    }
                                }
                            } catch (Throwable th2) {
                                error = th2;
                                started = started2;
                                events = events2;
                            }
                        } catch (Throwable th3) {
                            error = th3;
                            started = started2;
                            events = events2;
                            context = context;
                            video = video2;
                        }
                    } catch (Throwable th4) {
                        error = th4;
                        started = started2;
                        events = events2;
                        video = video2;
                    }
                } catch (Throwable th5) {
                    error = th5;
                    video = video2;
                    events = events2;
                    started = started2;
                }
            } catch (Throwable th6) {
                error = th6;
                events = events2;
                video = video2;
                started = started2;
            }
        } catch (Throwable th7) {
            error = th7;
            complete = complete2;
            events = events2;
            context = context2;
            video = video2;
            started = started2;
        }
    }

    private static final void controlsVideoFailureBackgroundStopAndExplicitReconnect$record(JSONArray events, long started, String kind, Object value) {
        events.put(new JSONObject().put("elapsedMs", SystemClock.elapsedRealtime() - started).put("kind", kind).put("value", value));
    }

    private static final JSONObject controlsVideoFailureBackgroundStopAndExplicitReconnect$get(OkHttpClient http, String host, String path) {
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

    private static final JSONObject controlsVideoFailureBackgroundStopAndExplicitReconnect$status(OkHttpClient http, String host, String expectedCar, JSONArray events, long started) {
        JSONObject it = controlsVideoFailureBackgroundStopAndExplicitReconnect$get(http, host, "status");
        Assert.assertEquals(expectedCar, it.getString("carId"));
        Assert.assertFalse("Physical outputs refused", it.getBoolean("outputsEnabled"));
        Assert.assertEquals("udp-v1", it.getString("controlTransport"));
        controlsVideoFailureBackgroundStopAndExplicitReconnect$record(events, started, "status", it);
        return it;
    }

    private static final void controlsVideoFailureBackgroundStopAndExplicitReconnect$awaitStatus(OkHttpClient http, String host, String expectedCar, JSONArray events, long started, Function1<? super JSONObject, Boolean> function1) {
        long until = SystemClock.elapsedRealtime() + ((long) 3000);
        while (!((Boolean) function1.invoke(controlsVideoFailureBackgroundStopAndExplicitReconnect$status(http, host, expectedCar, events, started))).booleanValue()) {
            SystemClock.sleep(30);
            if (SystemClock.elapsedRealtime() >= until) {
                Assert.fail("Expected board state not observed");
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(JSONObject s) throws JSONException {
        if (s.getBoolean("armed")) {
            return false;
        }
        JSONObject it = s.getJSONObject("drivePlan");
        return it.getInt("fiPermille") == 0 && it.getInt("biPermille") == 0 && it.getInt("steeringNormalized") == 0;
    }

    private static final void controlsVideoFailureBackgroundStopAndExplicitReconnect$connected(final UdpControlScreenDeviceTest this$0) {
        this$0.compose.waitUntil(5000, new Function0<Boolean>() { // from class: vea.raceremote.UdpControlScreenDeviceTest$controlsVideoFailureBackgroundStopAndExplicitReconnect$connected$1
            {
                super(0);
            }

            /* JADX INFO: renamed from: invoke, reason: merged with bridge method [inline-methods] */
            public final Boolean m1543invoke() {
                Object obj;
                UdpControlScreenDeviceTest $this$invoke_u24lambda_u240 = this.this$0;
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
    }

    private static final void controlsVideoFailureBackgroundStopAndExplicitReconnect$trace(OkHttpClient http, String host, JSONArray events, long started, String label) throws JSONException {
        boolean z;
        JSONObject value = controlsVideoFailureBackgroundStopAndExplicitReconnect$get(http, host, "debug/udp-trace");
        controlsVideoFailureBackgroundStopAndExplicitReconnect$record(events, started, label, value);
        JSONArray rows = value.getJSONArray("events");
        String strM1296String$arg0$callassertTrue$funtrace$funcontrolsVideoFailureBackgroundStopAndExplicitReconnect$classUdpControlScreenDeviceTest = "STOP must reach board";
        Iterable $this$any$iv = RangesKt.until(0, rows.length());
        if (($this$any$iv instanceof Collection) && ((Collection) $this$any$iv).isEmpty()) {
            z = false;
        } else {
            IntIterator it = $this$any$iv.iterator();
            while (it.hasNext()) {
                int element$iv = it.nextInt();
                JSONObject value2 = value;
                if (rows.getJSONArray(element$iv).getInt(7) == 5) {
                    z = true;
                } else {
                    value = value2;
                }
            }
            z = false;
        }
        Assert.assertTrue(strM1296String$arg0$callassertTrue$funtrace$funcontrolsVideoFailureBackgroundStopAndExplicitReconnect$classUdpControlScreenDeviceTest, z);
    }

    /* JADX INFO: renamed from: vea.raceremote.UdpControlScreenDeviceTest$controlsVideoFailureBackgroundStopAndExplicitReconnect$12, reason: invalid class name */
    /* JADX INFO: compiled from: UdpControlScreenDeviceTest.kt */
    /* synthetic */ class AnonymousClass12 extends FunctionReferenceImpl implements Function1<JSONObject, Boolean> {
        public static final AnonymousClass12 INSTANCE = new AnonymousClass12();

        AnonymousClass12() {
            super(1, Intrinsics.Kotlin.class, "neutral", "controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(Lorg/json/JSONObject;)Z", 0);
        }

        public final Boolean invoke(JSONObject p0) {
            Intrinsics.checkNotNullParameter(p0, "p0");
            return Boolean.valueOf(UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(p0));
        }
    }

    /* JADX INFO: renamed from: vea.raceremote.UdpControlScreenDeviceTest$controlsVideoFailureBackgroundStopAndExplicitReconnect$15, reason: invalid class name */
    /* JADX INFO: compiled from: UdpControlScreenDeviceTest.kt */
    /* synthetic */ class AnonymousClass15 extends FunctionReferenceImpl implements Function1<JSONObject, Boolean> {
        public static final AnonymousClass15 INSTANCE = new AnonymousClass15();

        AnonymousClass15() {
            super(1, Intrinsics.Kotlin.class, "neutral", "controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(Lorg/json/JSONObject;)Z", 0);
        }

        public final Boolean invoke(JSONObject p0) {
            Intrinsics.checkNotNullParameter(p0, "p0");
            return Boolean.valueOf(UdpControlScreenDeviceTest.controlsVideoFailureBackgroundStopAndExplicitReconnect$neutral(p0));
        }
    }
}
