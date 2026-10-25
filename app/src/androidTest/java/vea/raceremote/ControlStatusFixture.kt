package vea.raceremote

import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import okhttp3.WebSocket
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import vea.raceremote.app.MainActivity
import vea.raceremote.control.CarControlScreen

internal const val FIXTURE_CHIP = 0x123456L

internal fun MockWebServer.enqueueControlStatus() = enqueue(MockResponse().setBody(
    """{"protocol":1,"acceptingControl":true,"controlTransport":"websocket-v1","controlPort":1337,"chipId":"123456"}"""))

internal fun WebSocket.sendFixtureIdentity(token: Long, chip: Long = FIXTURE_CHIP) {
    val bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        .put(byteArrayOf(82, 67, 1, 130.toByte())).putInt(token.toInt()).putLong(chip).array()
    send(ByteString.of(*bytes))
}

internal fun MainActivity.showControlFixture() = setContent {
    MaterialTheme { CarControlScreen(statusPort = 1337) }
}
