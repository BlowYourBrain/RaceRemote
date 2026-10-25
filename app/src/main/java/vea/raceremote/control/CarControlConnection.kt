package vea.raceremote.control

import java.net.DatagramSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

/** Status selects the transport; only an explicit connect can open a control session. */
class CarControlConnection(
    private val resolve: (String, OkHttpClient, Int) -> ControlEndpoint = ControlEndpointProbe::load,
    private val diagnostics: (String) -> Unit = {},
) {
    private val mutableState = MutableStateFlow(CarConnectionState())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var generation = 0L
    private var closed = false
    private var udp: UdpControlClient? = null
    private var websocket: CarControlClient? = null

    @Synchronized
    fun connect(
        host: String,
        http: OkHttpClient,
        bindUdp: (DatagramSocket) -> Unit = {},
        expectedPort: Int? = null,
        expectedChipId: Long? = null,
        statusPort: Int = 80,
        expectedTransport: String? = null,
    ) {
        if (closed) return
        disconnect()
        if (!isLocalControlAddress(host) || statusPort !in 1..65535) {
            mutableState.value = CarConnectionState("Укажи локальный IPv4-адрес машинки")
            return
        }
        val current = generation
        mutableState.value = CarConnectionState("Проверка машинки…", connecting = true)
        job = scope.launch {
            try {
                val endpoint = resolve(host, http, statusPort)
                synchronized(this@CarControlConnection) {
                    if (current != generation || closed) return@launch
                    require(endpoint.chipId in 1..0xffffffffffffL && endpoint.port in 1..65535)
                    if (expectedChipId != null && endpoint.chipId != expectedChipId) {
                        disconnect("По этому адресу другая машинка"); return@launch
                    }
                    if (expectedPort != null && endpoint.port != expectedPort) {
                        disconnect("Настройки машинки изменились — выбери её заново"); return@launch
                    }
                    if (expectedTransport != null && endpoint.transport != expectedTransport) {
                        disconnect("Способ управления изменился — выбери машинку заново"); return@launch
                    }
                    when (endpoint.transport) {
                        "udp-v1" -> {
                            require(endpoint.port == UdpControlPacket.PORT)
                            udp = UdpControlClient(host, endpoint.chipId, bindUdp, diagnostics)
                        }
                        "websocket-v1" -> websocket = CarControlClient(diagnostics).also {
                            it.connect(host, http, endpoint.port, endpoint.chipId)
                        }
                        else -> { disconnect("Версия управления не поддерживается"); return@launch }
                    }
                }
                while (isActive) {
                    synchronized(this@CarControlConnection) {
                        if (current != generation || closed) return@launch
                        val value = websocket?.state?.value ?: udp?.snapshot()?.let {
                            when (it.phase) {
                                UdpControlSession.Phase.OPENING, UdpControlSession.Phase.ARMING ->
                                    CarConnectionState("Подключение…", connecting = true)
                                UdpControlSession.Phase.ACTIVE -> CarConnectionState("Управление подключено",
                                    connected = true, throttle = it.acknowledgedThrottle, steering = it.acknowledgedSteering)
                                UdpControlSession.Phase.CLOSED -> CarConnectionState("Нет свежего ответа — остановлено")
                            }
                        } ?: return@launch
                        mutableState.value = value
                        if (!value.connected && !value.connecting) { disconnect(value.message); return@launch }
                    }
                    delay(20)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                synchronized(this@CarControlConnection) {
                    if (current == generation && !closed) {
                        diagnostics("Connection failed: ${e.javaClass.simpleName}")
                        disconnect("Не удалось проверить машинку — остановлено")
                    }
                }
            }
        }
    }

    @Synchronized fun setInput(throttle: Float, steering: Float) {
        if (!throttle.isFinite() || !steering.isFinite()) { disconnect("Некорректное управление"); return }
        if (state.value.connected) {
            udp?.input((throttle.coerceIn(-1f, 1f) * 1000).toInt(), (steering.coerceIn(-1f, 1f) * 1000).toInt())
            websocket?.setInput(throttle, steering)
        }
    }

    @Synchronized fun disconnect(message: String = "Остановлено") {
        generation++
        mutableState.value = CarConnectionState(message)
        job?.cancel(); job = null
        udp?.close(); udp = null
        websocket?.close(); websocket = null
    }

    @Synchronized fun close() {
        closed = true
        disconnect()
        scope.cancel()
    }
}
