package vea.raceremote.control

import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import vea.raceremote.video.VideoPanel

@Composable
fun CarControlScreen() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val client = remember { CarControlClient(diagnostics = { android.util.Log.d("RaceRemoteControl", it) }) }
    val state by client.state.collectAsState()
    var host by rememberSaveable { mutableStateOf("192.168.4.1") }
    var controlPort by rememberSaveable { mutableStateOf(1337) }
    var throttle by remember { mutableStateOf(0f) }
    var steering by remember { mutableStateOf(0f) }
    var showVideo by rememberSaveable { mutableStateOf(false) }
    var videoActive by remember { mutableStateOf(false) }
    var driving by remember { mutableStateOf(false) }
    val http = remember { OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build() }
    val wifiLock = remember {
        context.applicationContext.getSystemService(WifiManager::class.java)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "RaceRemote:control")
            .apply { setReferenceCounted(false) }
    }
    LaunchedEffect(state.connected, state.connecting, videoActive) {
        if (state.connected || state.connecting || videoActive) {
            if (!wifiLock.isHeld) wifiLock.acquire()
        } else if (wifiLock.isHeld) wifiLock.release()
    }
    DisposableEffect(lifecycle, client) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                throttle = 0f; steering = 0f; client.disconnect("Приложение не активно — остановлено")
                if (wifiLock.isHeld) wifiLock.release()
            }
        }
        lifecycle.addObserver(observer)
        (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            lifecycle.removeObserver(observer); client.close()
            if (wifiLock.isHeld) wifiLock.release()
            http.dispatcher().executorService().shutdown()
            http.connectionPool().evictAll()
            (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(state.connected) { if (!state.connected) { throttle = 0f; steering = 0f } }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!driving) {
            OutlinedTextField(host, { host = it; controlPort = 1337 }, label = { Text("Адрес машинки") }, singleLine = true,
                enabled = !state.connected && !state.connecting, modifier = Modifier.weight(1f).testTag("car_address"))
            CarPicker(enabled = !state.connected && !state.connecting) { car -> host = car.host; controlPort = car.port }
            Button(onClick = {
                throttle = 0f; steering = 0f
                wifiLock.acquire()
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val wifi = manager.allNetworks.firstOrNull { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
                val networkClient = if (wifi != null) http.newBuilder().socketFactory(wifi.socketFactory).build() else http
                client.connect(host.trim(), networkClient, controlPort)
            }, enabled = !state.connected && !state.connecting, modifier = Modifier.testTag("connect_car")) { Text("Подключить") }
            } else {
                Text(state.message, Modifier.weight(1f).testTag("driving_connection_status"))
            }
            Button(onClick = {
                throttle = 0f; steering = 0f; client.setInput(0f, 0f)
                driving = !driving
            }, modifier = Modifier.testTag("driving_mode")) { Text(if (driving) "Настройка" else "Заезд") }
            Button(onClick = { throttle = 0f; steering = 0f; client.disconnect() }, modifier = Modifier.testTag("stop_car"),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)) { Text("СТОП") }
            if (!driving) Column {
                Text("Видео")
                Checkbox(showVideo, { showVideo = it }, modifier = Modifier.testTag("show_video"))
            }
        }
        if (!driving) {
            Text(state.message, Modifier.testTag("connection_status"))
            Text("Газ: ${state.throttle / 10}%   Руль: ${state.steering / 10}%", Modifier.testTag("acknowledged_input"))
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (driving) DrivingPad("Газ", true, throttle, state.connected, Modifier.width(104.dp).fillMaxHeight()) {
                throttle = it; client.setInput(throttle, steering)
            }
            if (showVideo) key(host.trim(), controlPort) {
                // A different control target owns a fresh video session. Dispose
                // the previous stream/view before showing another car's camera.
                VideoPanel(carHost = host.trim(), modifier = Modifier.weight(1f).fillMaxHeight(), showSettings = !driving) { videoActive = it }
            }
            else if (driving) Text("Газ слева, руль справа.\nОтпусти палец для возврата в нейтраль.", Modifier.weight(1f))
            if (driving) DrivingPad("Руль", false, steering, state.connected, Modifier.width(160.dp).fillMaxHeight()) {
                steering = it; client.setInput(throttle, steering)
            }
            else Column(if (showVideo) Modifier.width(240.dp) else Modifier.fillMaxWidth()) {
                Text("Газ · отпусти для остановки")
                Slider(value = throttle, onValueChange = { throttle = it; client.setInput(throttle, steering) },
                    onValueChangeFinished = { throttle = 0f; client.setInput(throttle, steering) },
                    valueRange = -1f..1f, enabled = state.connected, modifier = Modifier.fillMaxWidth().testTag("throttle"))
                Text("Руль · отпусти для возврата в центр")
                Slider(value = steering, onValueChange = { steering = it; client.setInput(throttle, steering) },
                    onValueChangeFinished = { steering = 0f; client.setInput(throttle, steering) },
                    valueRange = -1f..1f, enabled = state.connected, modifier = Modifier.fillMaxWidth().testTag("steering"))
                if (!showVideo) Text("Подключи машинку и телефон к одной сети или используй точку доступа телефона. Нажми «Найти».")
            }
        }
    }
}
