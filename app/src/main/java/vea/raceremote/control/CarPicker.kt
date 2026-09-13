package vea.raceremote.control

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun CarPicker(enabled: Boolean, onPick: (DiscoveredCar) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val discovery = remember { CarDiscovery() }
    val state by discovery.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    fun search() {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val wifi = manager.allNetworks.firstOrNull {
            manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        discovery.search { socket -> wifi?.bindSocket(socket) }
    }
    DisposableEffect(discovery, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                open = false; discovery.cancel()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); discovery.close() }
    }
    Button(onClick = { open = true; search() }, enabled = enabled, modifier = Modifier.testTag("find_cars")) { Text("Найти") }
    if (open) AlertDialog(
        onDismissRequest = { open = false; discovery.cancel() },
        title = { Text("Машинки в локальной сети") },
        text = {
            Column {
                Text(state.message, Modifier.testTag("discovery_status"))
                LazyColumn(Modifier.heightIn(max = 200.dp)) {
                    items(state.cars, key = { "${it.chipId}/${it.host}" }) { car ->
                        TextButton(onClick = { onPick(car); open = false; discovery.cancel() },
                            modifier = Modifier.fillMaxWidth().testTag("discovered_${car.chipId}")) {
                            Text("${car.name} · ${car.host}\n${if (car.busy) "Занята другим подключением" else "Свободна"}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { search() }, enabled = !state.searching) { Text("Обновить") } },
        dismissButton = { TextButton(onClick = { open = false; discovery.cancel() }) { Text("Закрыть") } },
    )
}
