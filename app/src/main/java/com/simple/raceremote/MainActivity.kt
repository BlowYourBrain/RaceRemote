package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    RaceRemoteTheme(darkTheme = true) {
        Row() {
            Slider(
                modifier = Modifier.weight(1f)
            ) {

            }
            Slider(modifier = Modifier.weight(1f), Orientation.Vertical) {

            }
        }

    }
}


@Preview
@Composable
fun AppPreview() {
    App()
}