package vea.raceremote

import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextClearance
import androidx.lifecycle.Lifecycle
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vea.raceremote.app.MainActivity
import vea.raceremote.control.CarControlScreen
import vea.raceremote.control.ControlPacket
import vea.raceremote.control.DiscoveredCar
import vea.raceremote.control.IdentityPacket

class CarIdentitySelectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectedCarMustConfirmItsIdentity() = selection(matching = true)
    @Test fun changedIdentityRequiresExplicitManualSelectionAndIsStillVerified() = selection(matching = false)

    private fun selection(matching: Boolean) {
        val chip = 0xa06da69ef0c8L
        val actualChip = if (matching) chip else chip xor 1L
        val token = 0x12345678L
        val drives = AtomicInteger()
        val arms = AtomicInteger()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/status") return MockResponse().setBody(
                    """{"protocol":1,"acceptingControl":true,"controlTransport":"websocket-v1","controlPort":1337,"chipId":"${actualChip.toString(16)}"}""")
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (request.path == IdentityPacket.PATH) {
                        webSocket.sendFixtureIdentity(token, actualChip)
                    }
                    webSocket.send(ByteString.of(*ControlPacket(ControlPacket.HELLO, token, 0).encode()))
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val p = ControlPacket.decode(bytes.toByteArray())!!
                    if (p.kind == ControlPacket.ARM) arms.incrementAndGet()
                    if (p.kind == ControlPacket.DRIVE) drives.incrementAndGet()
                    if (p.kind != ControlPacket.STOP) webSocket.send(ByteString.of(*p.copy(kind = ControlPacket.ACK).encode()))
                }
            })
            }
        }
        server.start(1337)
        try {
            // Replace only discovery's row source; exercise the real selection,
            // displayed identity and connection callback of CarControlScreen.
            compose.runOnUiThread {
                compose.activity.setContent {
                    MaterialTheme {
                        CarControlScreen(carPicker = { enabled, pick ->
                            Button(onClick = { pick(DiscoveredCar("127.0.0.1", 1337, chip, false)) },
                                enabled = enabled, modifier = Modifier.testTag("pick_fixture_car")) { Text("Выбрать") }
                        }, statusPort = 1337)
                    }
                }
            }
            compose.onNodeWithTag("pick_fixture_car").performClick()
            compose.onNodeWithTag("selected_car_identity").assertTextContains("a06da69ef0c8", substring = true)
            compose.onNodeWithTag("connect_car").performClick()
            assertEquals("/status", server.takeRequest(3, TimeUnit.SECONDS)!!.path)
            if (!matching) {
                compose.waitUntil(5000) {
                    runCatching { compose.onNodeWithTag("connection_status").assertTextContains("По этому адресу другая машинка") }.isSuccess
                }
                assertEquals(0, arms.get())
                assertEquals(1, server.requestCount) // No control connection after a discovery/status identity mismatch.
                compose.onNodeWithTag("car_address").performTextClearance()
                compose.onNodeWithTag("car_address").performTextReplacement("127.0.0.1")
                compose.onNodeWithTag("selected_car_identity").assertTextContains("Ручной адрес · ID будет проверен при подключении")
                compose.onNodeWithTag("connect_car").performClick()
                assertEquals("/status", server.takeRequest(3, TimeUnit.SECONDS)!!.path)
            }
            assertEquals(IdentityPacket.PATH, server.takeRequest(3, TimeUnit.SECONDS)!!.path)
            compose.waitUntil(5000) { drives.get() > 3 }
            compose.onNodeWithTag("connection_status").assertTextContains("Управление подключено")
            assertEquals(1, arms.get())
        } finally {
            try { compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED) }
            finally { server.shutdown() }
        }
    }
}
