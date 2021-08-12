package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
        Row() {
            Joystick(
                Modifier
                    .padding(all = 100.dp)
                    .weight(1f)
            )
            Box(
                Modifier.weight(1f)
            ) {

            }
        }

    }
}


@Preview
@Composable
fun AppPreview() {
    App()
}