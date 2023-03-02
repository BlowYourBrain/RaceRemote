package com.simple.raceremote.network

import com.simple.raceremote.utils.debug
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class WebSocketApi : WebSocketListener() {

    companion object {
        private const val BASE_URl = "ws://192.168.1.100:1337/"
        private const val CONNECTION_TIMEOUT = 10L
        private const val READ_TIMEOUT = 10L
    }

    private val webSocket: WebSocket? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    fun sendMessage(message: ByteString) {
        getOrCreateWebSocket().let { socket ->
            debug("size: ${socket.queueSize()} \nmessage: $message")
            val isSend = socket.send(message)
            debug("isSend = $isSend\n")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = webSocket.cancel()

    fun cancelWebSocket() {
        webSocket?.cancel()
    }

    private fun getOrCreateWebSocket(): WebSocket {
        return webSocket ?: kotlin.run {
            val request = Request.Builder()
                .url(BASE_URl)
                .build()

            okHttpClient.newWebSocket(request, this)
        }
    }
}