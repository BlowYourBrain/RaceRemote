package vea.raceremote;

import androidx.activity.ComponentActivity;
import androidx.activity.compose.ComponentActivityKt;
import androidx.compose.runtime.CompositionContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import vea.raceremote.app.MainActivity;

/* JADX INFO: compiled from: ControlStatusFixture.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ControlStatusFixtureKt {
    public static final long FIXTURE_CHIP = 1193046;

    public static final void enqueueControlStatus(MockWebServer $this$enqueueControlStatus) {
        Intrinsics.checkNotNullParameter($this$enqueueControlStatus, "<this>");
        $this$enqueueControlStatus.enqueue(new MockResponse().setBody("{\"protocol\":1,\"acceptingControl\":true,\"controlTransport\":\"websocket-v1\",\"controlPort\":1337,\"chipId\":\"123456\"}"));
    }

    public static final void sendFixtureIdentity(WebSocket $this$sendFixtureIdentity, long token) {
        Intrinsics.checkNotNullParameter($this$sendFixtureIdentity, "<this>");
        byte[] bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).put(new byte[]{82, 67, 1, (byte) 130}).putInt((int) token).putLong(1193046L).array();
        $this$sendFixtureIdentity.send(ByteString.of(Arrays.copyOf(bytes, bytes.length)));
    }

    public static final void showControlFixture(MainActivity $this$showControlFixture) {
        Intrinsics.checkNotNullParameter($this$showControlFixture, "<this>");
        ComponentActivityKt.setContent$default((ComponentActivity) $this$showControlFixture, (CompositionContext) null, ComposableSingletons$ControlStatusFixtureKt.INSTANCE.m494getLambda2$app_debug(), 1, (Object) null);
    }
}
