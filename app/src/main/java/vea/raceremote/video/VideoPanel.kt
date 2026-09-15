package vea.raceremote.video

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okhttp3.OkHttpClient

@Composable
fun VideoPanel(carHost: String, modifier: Modifier = Modifier, showSettings: Boolean = true, onActiveChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activeCallback by rememberUpdatedState(onActiveChanged)
    var url by rememberSaveable { mutableStateOf("http://$carHost:81/stream") }
    var message by remember { mutableStateOf("Видео выключено") }
    var active by remember { mutableStateOf(false) }
    val http = remember { OkHttpClient() }
    val view = remember { MjpegVideoView(context) }
    DisposableEffect(view, lifecycle) {
        view.onStatus = { status, running -> message = status; active = running; activeCallback(running) }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) view.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer); view.close()
            http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll()
        }
    }
    Column(modifier) {
        if (showSettings) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(url, { url = it }, enabled = !active, singleLine = true,
                label = { Text("Адрес видео") }, modifier = Modifier.weight(1f).testTag("video_address"))
            Button(onClick = {
                if (active) view.stop() else {
                    val manager = context.getSystemService(ConnectivityManager::class.java)
                    val wifi = manager.allNetworks.firstOrNull {
                        manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    }
                    val transport = if (wifi == null) http else http.newBuilder().socketFactory(wifi.socketFactory).build()
                    view.start(url.trim(), transport)
                }
            }, modifier = Modifier.testTag("toggle_video")) { Text(if (active) "Выключить" else "Смотреть") }
        }
        Text(message, Modifier.testTag("video_status"), style = MaterialTheme.typography.caption)
        AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds().testTag("video_image"))
    }
}
