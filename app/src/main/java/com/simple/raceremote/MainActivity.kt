package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simple.raceremote.ui.theme.RaceRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    RaceRemoteTheme {
        val xStr = remember { mutableStateOf("") }
        val yStr = remember { mutableStateOf("") }

        Row() {
            Joystick(
                modifier = Modifier
                    .padding(all = 100.dp)
                    .weight(1f),
                onUpdateStickPosition = { x, y ->
                    xStr.value = "x = $x"
                    yStr.value = "y = $y"
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(xStr.value)
                Text(yStr.value)
            }
        }

    }
}


@Preview
@Composable
fun AppPreview() {
    App()
}