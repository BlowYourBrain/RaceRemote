package vea.raceremote.control;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.CharsKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.json.JSONObject;

/* JADX INFO: compiled from: ControlEndpointProbe.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ControlEndpointProbe {
    public static final ControlEndpointProbe INSTANCE = new ControlEndpointProbe();
    public static final int $stable = 0;

    private ControlEndpointProbe() {
    }

    public final ControlEndpoint load(String host, OkHttpClient http, int port) throws IOException {
        Intrinsics.checkNotNullParameter(host, "host");
        Intrinsics.checkNotNullParameter(http, "http");
        OkHttpClient bounded = http.newBuilder().followRedirects(false).followSslRedirects(false).connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).callTimeout(3, TimeUnit.SECONDS).build();
        Response responseExecute = bounded.newCall(new Request.Builder().url("http://" + host + ":" + port + "/status").build()).execute();
        try {
            Response response = responseExecute;
            if (!(response.code() == 200)) {
                throw new IllegalStateException("Check failed.".toString());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalArgumentException("Required value was null.".toString());
            }
            Intrinsics.checkNotNullExpressionValue(body, "requireNotNull(response.body())");
            BufferedSource source = body.source();
            if (!(!source.request(8193))) {
                throw new IllegalStateException("Status too large".toString());
            }
            JSONObject value = new JSONObject(source.readUtf8());
            if (!(value.getInt("protocol") == 1)) {
                throw new IllegalStateException("Check failed.".toString());
            }
            if (!value.getBoolean("acceptingControl")) {
                throw new IllegalStateException("Check failed.".toString());
            }
            String string = value.getString("controlTransport");
            Intrinsics.checkNotNullExpressionValue(string, "value.getString(\"controlTransport\")");
            int i = value.getInt("controlPort");
            String string2 = value.getString("chipId");
            Intrinsics.checkNotNullExpressionValue(string2, "value.getString(\"chipId\")");
            ControlEndpoint controlEndpoint = new ControlEndpoint(string, i, Long.parseLong(string2, CharsKt.checkRadix(16)));
            CloseableKt.closeFinally(responseExecute, null);
            return controlEndpoint;
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(responseExecute, th);
                throw th2;
            }
        }
    }
}
