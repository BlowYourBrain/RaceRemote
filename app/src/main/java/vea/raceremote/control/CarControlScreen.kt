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

@Composable
fun CarControlScreen() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val client = remember { CarControlClient(diagnostics = { android.util.Log.d("RaceRemoteControl", it) }) }
    val state by client.state.collectAsState()
    var host by rememberSaveable { mutableStateOf("192.168.4.1") }
    var throttle by remember { mutableStateOf(0f) }
    var steering by remember { mutableStateOf(0f) }
    val http = remember { OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build() }
    val wifiLock = remember {
        context.applicationContext.getSystemService(WifiManager::class.java)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "RaceRemote:control")
            .apply { setReferenceCounted(false) }
    }
    LaunchedEffect(state.connected, state.connecting) {
        if (!state.connected && !state.connecting && wifiLock.isHeld) wifiLock.release()
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
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(host, { host = it }, label = { Text("Адрес машинки") }, singleLine = true,
                enabled = !state.connected && !state.connecting, modifier = Modifier.weight(1f).testTag("car_address"))
            Button(onClick = {
                throttle = 0f; steering = 0f
                wifiLock.acquire()
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val wifi = manager.allNetworks.firstOrNull { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
                val networkClient = if (wifi != null) http.newBuilder().socketFactory(wifi.socketFactory).build() else http
                client.connect(host.trim(), networkClient)
            }, enabled = !state.connected && !state.connecting, modifier = Modifier.testTag("connect_car")) { Text("Подключить") }
            Button(onClick = { throttle = 0f; steering = 0f; client.disconnect() }, modifier = Modifier.testTag("stop_car"),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)) { Text("СТОП") }
        }
        Text(state.message, Modifier.testTag("connection_status"))
        Text("Газ: ${state.throttle / 10}%   Руль: ${state.steering / 10}%", Modifier.testTag("acknowledged_input"))
        Spacer(Modifier.height(12.dp))
        Text("Газ · отпусти для остановки")
        Slider(value = throttle, onValueChange = { throttle = it; client.setInput(throttle, steering) },
            onValueChangeFinished = { throttle = 0f; client.setInput(throttle, steering) },
            valueRange = -1f..1f, enabled = state.connected, modifier = Modifier.fillMaxWidth().testTag("throttle"))
        Text("Руль · отпусти для возврата в центр")
        Slider(value = steering, onValueChange = { steering = it; client.setInput(throttle, steering) },
            onValueChangeFinished = { steering = 0f; client.setInput(throttle, steering) },
            valueRange = -1f..1f, enabled = state.connected, modifier = Modifier.fillMaxWidth().testTag("steering"))
        Text("Первое подключение: выбери Wi-Fi RaceRemote в настройках телефона.")
    }
}
