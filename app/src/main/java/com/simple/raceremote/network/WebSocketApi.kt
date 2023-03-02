package com.simple.raceremote.network

import com.simple.raceremote.utils.debug
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class WebSocketApi : WebSocketListener() {

    enum class WebSocketState {
        OPEN,
        MESSAGE,
        CLOSING,
        FAILURE
    }

    companion object {
        private const val BASE_URl = "ws://192.168.1.100:1337/"
        private const val READ_TIMEOUT = 10L
        private const val CONNECTION_TIMEOUT = 10L
    }

    private var webSocket: WebSocket? = null
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    var onUpdateSocketState: ((WebSocket, WebSocketState) -> Unit)? = null

    fun initWebSocket() {
        val request = Request.Builder()
            .url(BASE_URl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, this)
    }

    fun cancelWebSocket() {
        webSocket?.cancel()
    }

    fun updateValue(steeringWheel: Int, engine: Int) {

    }

    fun sendMessage(message: String) {
        webSocket?.let { socket ->
            debug("size: ${socket.queueSize()} \nmessage: $message")
            val isSend = socket.send(message)
            debug("isSend = $isSend\n")
        }
    }

    fun sendMessage(message: ByteString) {
        webSocket?.let { socket ->
            debug("size: ${socket.queueSize()} \nmessage: $message")
            val isSend = socket.send(message)
            debug("isSend = $isSend\n")
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onUpdateSocketState?.invoke(webSocket, WebSocketState.OPEN)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onUpdateSocketState?.invoke(webSocket, WebSocketState.MESSAGE)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onUpdateSocketState?.invoke(webSocket, WebSocketState.CLOSING)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onUpdateSocketState?.invoke(webSocket, WebSocketState.FAILURE)
    }
}