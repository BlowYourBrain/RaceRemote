package vea.raceremote;

import java.io.IOException;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.text.StringsKt;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Assert;
import org.junit.Test;
import vea.raceremote.control.ControlEndpoint;
import vea.raceremote.control.ControlEndpointProbe;

/* JADX INFO: compiled from: ControlEndpointProbeTest.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ControlEndpointProbeTest {
    public static final int $stable = 0;

    @Test
    public final void realJsonProbeRejectsRedirectOversizeAndInvalidStatus() throws IOException {
        Object obj;
        MockWebServer server = new MockWebServer();
        String valid = "{\"protocol\":1,\"acceptingControl\":true,\"controlTransport\":\"udp-v1\",\"controlPort\":3334,\"chipId\":\"5008d93f1fb8\"}";
        server.enqueue(new MockResponse().setBody(valid));
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/redirected"));
        server.enqueue(new MockResponse().setBody(StringsKt.repeat(" ", 8193)));
        server.enqueue(new MockResponse().setBody(StringsKt.replace$default(valid, "true", "false", false, 4, (Object) null)));
        server.enqueue(new MockResponse().setBody("{}"));
        server.start();
        OkHttpClient http = new OkHttpClient();
        try {
            ControlEndpoint endpoint = ControlEndpointProbe.INSTANCE.load("127.0.0.1", http, server.getPort());
            Assert.assertEquals("udp-v1", endpoint.getTransport());
            Assert.assertEquals(87998934753208L, endpoint.getChipId());
            int iM620Int$arg0$callrepeat$try$funrealJsonProbeRejectsRedirectOversizeAndInvalidStatus$classControlEndpointProbeTest = 4;
            for (int i = 0; i < iM620Int$arg0$callrepeat$try$funrealJsonProbeRejectsRedirectOversizeAndInvalidStatus$classControlEndpointProbeTest; i++) {
                try {
                    Result.Companion companion = Result.Companion;
                    ControlEndpointProbeTest controlEndpointProbeTest = this;
                    obj = Result.constructor-impl(ControlEndpointProbe.INSTANCE.load("127.0.0.1", http, server.getPort()));
                } catch (Throwable th) {
                    Result.Companion companion2 = Result.Companion;
                    obj = Result.constructor-impl(ResultKt.createFailure(th));
                }
                Assert.assertTrue(Result.isFailure-impl(obj));
            }
            Assert.assertEquals(Integer.valueOf(5), Integer.valueOf(server.getRequestCount()));
            server.shutdown();
            http.dispatcher().executorService().shutdown();
            http.connectionPool().evictAll();
        } catch (Throwable th2) {
            server.shutdown();
            http.dispatcher().executorService().shutdown();
            http.connectionPool().evictAll();
            throw th2;
        }
    }
}
