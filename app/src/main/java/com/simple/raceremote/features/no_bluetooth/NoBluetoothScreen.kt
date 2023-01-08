package com.simple.raceremote.features.no_bluetooth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.simple.raceremote.R
import com.simple.raceremote.ui.views.TextButton
import com.simple.raceremote.utils.debug

// TODO закрывать экран при нажатии на кнопку Выйти
@Composable
fun NoBluetoothScreen(navController: NavHostController) {
    NoBluetoothView()
}

@Preview
@Composable
private fun PreviewNoBluetoothView() {
    NoBluetoothView()
}

@Composable
private fun NoBluetoothView() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.bluetooth_not_found),
                style = MaterialTheme.typography.h4
            )

            Spacer(modifier = Modifier.height(64.dp))

            TextButton(
                text = stringResource(id = R.string.exit),
                onClick = { finishAppLog() }
            )
        }
    }
}

private fun finishAppLog() {
    debug("finish app")
}
