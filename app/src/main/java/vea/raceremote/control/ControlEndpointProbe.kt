package vea.raceremote.control

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ControlEndpoint(val transport: String, val port: Int, val chipId: Long)

object ControlEndpointProbe {
    fun load(host: String, http: OkHttpClient, port: Int = 80): ControlEndpoint {
        require(isLocalControlAddress(host) && port in 1..65535)
        val bounded = http.newBuilder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS).build()
        bounded.newCall(Request.Builder().url("http://$host:$port/status").build()).execute().use { response ->
            check(response.code() == 200)
            val source = requireNotNull(response.body()).source()
            check(!source.request(8193)) { "Status too large" }
            val value = JSONObject(source.readUtf8())
            check(value.getInt("protocol") == 1 && value.getBoolean("acceptingControl"))
            return ControlEndpoint(value.getString("controlTransport"), value.getInt("controlPort"),
                value.getString("chipId").toLong(16))
        }
    }
}
