package vea.raceremote.control

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

data class CarConnectionState(
    val message: String = "Не подключено",
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val throttle: Int = 0,
    val steering: Int = 0,
)

/** One connection, one in-flight command. Never replay input after reconnect. */
class CarControlClient(
    private val diagnostics: (String) -> Unit = {},
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutableState = MutableStateFlow(CarConnectionState())
    val state = mutableState.asStateFlow()
    private val timer = Executors.newSingleThreadScheduledExecutor()
    private var socket: WebSocket? = null
    private var generation = 0L
    private var token = 0L
    private var sequence = 0L
    private var pending: Long? = null
    private var sentAt = 0L
    private var throttle = 0
    private var steering = 0
    private var opened = false

    init { timer.scheduleAtFixedRate({ synchronized(this) { tick() } }, 20, 20, TimeUnit.MILLISECONDS) }

    @Synchronized
    fun connect(host: String, httpClient: OkHttpClient, port: Int = 1337, expectedChipId: Long? = null) {
        disconnect()
        // The prototype intentionally permits only local IPv4 destinations.
        val octets = host.split('.').map { it.toIntOrNull() }
        val local = octets.size == 4 && octets.all { it != null && it in 0..255 } &&
            (octets[0] == 10 || octets[0] == 127 ||
                (octets[0] == 192 && octets[1] == 168) ||
                (octets[0] == 172 && octets[1]!! in 16..31))
        if (!local || port !in 1..65535) { mutableState.value = CarConnectionState("Укажи локальный IPv4-адрес машинки"); return }
        if (expectedChipId != null && expectedChipId !in 1..0xffffffffffffL) {
            mutableState.value = CarConnectionState("Некорректный ID машинки"); return
        }
        val current = generation
        var identityToken: Long? = null
        sentAt = clockMs()
        mutableState.value = CarConnectionState("Подключение…", connecting = true)
        val transport = httpClient.newBuilder().socketFactory(ControlSocketFactory(httpClient.socketFactory())).build()
        val path = if (expectedChipId == null) "/control" else IdentityPacket.PATH
        socket = transport.newWebSocket(Request.Builder().url("ws://$host:$port$path").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = synchronized(this@CarControlClient) {
                diagnostics("WebSocket opened")
                if (current == generation) opened = true else webSocket.cancel()
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = synchronized(this@CarControlClient) {
                if (current != generation) return@synchronized
                if (expectedChipId != null && identityToken == null) {
                    diagnostics("Received identity bytes=${bytes.size()}")
                    val identity = IdentityPacket.decode(bytes.toByteArray())
                    when {
                        clockMs() - sentAt >= 4000 -> disconnect("Нет свежего ответа — остановлено")
                        identity == null -> disconnect("Машинка не подтвердила свой ID")
                        identity.chipId != expectedChipId -> disconnect("По этому адресу другая машинка")
                        else -> identityToken = identity.token
                    }
                    return@synchronized // Identity alone never sends ARM or resets the deadline.
                }
                val p = ControlPacket.decode(bytes.toByteArray())
                if (token == 0L || sequence <= 30) diagnostics("Received bytes=${bytes.size()} kind=${p?.kind} sequence=${p?.sequence} rttMs=${clockMs() - sentAt}")
                if (p == null) { disconnect("Ошибка протокола"); return@synchronized }
                if (token == 0L && p.kind == ControlPacket.HELLO && p.sequence == 0L && p.throttle == 0 && p.steering == 0) {
                    // A receive callback can run before the next timer tick.
                    // Reject expired handshakes before assigning a token or sending ARM.
                    if (clockMs() - sentAt >= 4000) { disconnect("Нет свежего ответа — остановлено"); return@synchronized }
                    if (expectedChipId != null && p.token != identityToken) {
                        disconnect("ID и управление относятся к разным сессиям"); return@synchronized
                    }
                    token = p.token; sequence = 1
                    send(ControlPacket(ControlPacket.ARM, token, sequence))
                } else if (p.kind == ControlPacket.ACK && p.token == token && p.sequence == pending) {
                    if (clockMs() - sentAt >= 200) { disconnect("Ответ задержался — остановлено"); return@synchronized }
                    pending = null
                    mutableState.value = CarConnectionState("Управление подключено", connected = true, throttle = p.throttle, steering = p.steering)
                } else disconnect("Ответ не соответствует сессии")
            }
            override fun onMessage(webSocket: WebSocket, text: String) = synchronized(this@CarControlClient) {
                if (current == generation) disconnect("Неподдерживаемый протокол")
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = synchronized(this@CarControlClient) {
                diagnostics("WebSocket failure: ${t.javaClass.simpleName}: ${t.message}")
                if (current == generation) disconnect("Нет связи — остановлено")
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = synchronized(this@CarControlClient) {
                if (current == generation) disconnect("Соединение закрыто — остановлено")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = synchronized(this@CarControlClient) {
                if (current == generation) disconnect("Соединение закрыто — остановлено")
            }
        })
    }

    @Synchronized
    fun setInput(throttle: Float, steering: Float) {
        if (!throttle.isFinite() || !steering.isFinite()) { disconnect("Некорректное управление"); return }
        if (!mutableState.value.connected) return
        this.throttle = (throttle.coerceIn(-1f, 1f) * 1000).toInt()
        this.steering = (steering.coerceIn(-1f, 1f) * 1000).toInt()
    }

    private fun send(p: ControlPacket) {
        if (p.kind == ControlPacket.ARM) diagnostics("Sending neutral ARM")
        if (p.sequence <= 30) diagnostics("Send kind=${p.kind} sequence=${p.sequence}")
        pending = p.sequence; sentAt = clockMs()
        if (socket?.send(ByteString.of(*p.encode())) != true) disconnect("Не удалось отправить команду")
    }

    private fun tick() {
        if (socket == null) return
        val elapsed = clockMs() - sentAt
        if ((pending != null && elapsed >= 200) || (token == 0L && elapsed >= 4000)) {
            disconnect("Нет свежего ответа — остановлено"); return
        }
        if (opened && mutableState.value.connected && pending == null) {
            sequence = (sequence + 1) and 0xffffffffL
            send(ControlPacket(ControlPacket.DRIVE, token, sequence, throttle, steering))
        }
    }

    @Synchronized
    fun disconnect(message: String = "Остановлено") {
        diagnostics("Disconnect: $message")
        generation++
        val old = socket; socket = null
        if (token != 0L) old?.send(ByteString.of(*ControlPacket(ControlPacket.STOP, token, sequence).encode()))
        old?.cancel() // Controller watchdog is authoritative if STOP cannot be delivered.
        token = 0; sequence = 0; pending = null; throttle = 0; steering = 0; opened = false
        mutableState.value = CarConnectionState(message)
    }

    @Synchronized
    fun close() { disconnect(); timer.shutdownNow() }
}
