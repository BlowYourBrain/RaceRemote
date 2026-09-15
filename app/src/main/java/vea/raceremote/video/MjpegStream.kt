package vea.raceremote.video

import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Independent video transport. It has no reference to control or its session. */
class MjpegStream(private val onFrame: (ByteArray) -> Unit, private val onEnd: (String) -> Unit) {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    private var call: Call? = null

    @Synchronized
    fun start(url: String, http: OkHttpClient) {
        check(call == null && !closed) { "Use a new stream for each connection" }
        val parsed = HttpUrl.parse(url)
        if (parsed == null || parsed.scheme() !in listOf("http", "https") ||
            parsed.username().isNotEmpty() || parsed.password().isNotEmpty()) {
            onEnd("Проверь адрес видеопотока"); close(); return
        }
        val transport = http.newBuilder().connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val request = transport.newCall(Request.Builder().url(parsed).header("Accept", "multipart/x-mixed-replace").build())
        call = request
        worker.execute {
            try {
                request.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
                    val body = response.body() ?: throw IOException("Empty video response")
                    val reader = MjpegReader(body.byteStream(), response.header("Content-Type") ?: "")
                    val freshness = FrameFreshness()
                    while (!closed) {
                        val frame = reader.nextPart() ?: break
                        if (!closed && freshness.accept(frame)) onFrame(frame.bytes)
                    }
                }
                if (!closed) onEnd("Видеопоток завершён")
            } catch (error: Exception) {
                if (!closed) onEnd("Видео недоступно: ${error.message ?: error.javaClass.simpleName}")
            } finally { close() }
        }
    }

    @Synchronized
    fun close() { closed = true; call?.cancel(); worker.shutdownNow() }
}
