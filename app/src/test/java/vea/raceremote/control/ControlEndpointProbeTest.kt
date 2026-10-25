package vea.raceremote.control

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class ControlEndpointProbeTest {
    private val valid = """{"protocol":1,"acceptingControl":true,"controlTransport":"udp-v1","controlPort":3334,"chipId":"5008d93f1fb8"}"""

    @Test fun readsAdvertisedTransportAndIdentity() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(valid))
            server.start()
            assertEquals(ControlEndpoint("udp-v1", 3334, 0x5008d93f1fb8L),
                ControlEndpointProbe.load("127.0.0.1", OkHttpClient(), server.port))
            assertEquals("/status", server.takeRequest().path)
        }
    }

    @Test fun malformedOversizedDisabledAndRedirectedStatusFail() {
        MockWebServer().use { server ->
            server.start()
            val bodies = listOf("{}", valid.replace("true", "false"), valid.replace("\"protocol\":1", "\"protocol\":2"),
                valid.replace("5008d93f1fb8", "invalid"), " ".repeat(8193))
            for (body in bodies) {
                server.enqueue(MockResponse().setBody(body))
                assertTrue(runCatching { ControlEndpointProbe.load("127.0.0.1", OkHttpClient(), server.port) }.isFailure)
            }
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/other")))
            assertTrue(runCatching { ControlEndpointProbe.load("127.0.0.1", OkHttpClient(), server.port) }.isFailure)
            assertEquals(bodies.size + 1, server.requestCount)
        }
    }
}
