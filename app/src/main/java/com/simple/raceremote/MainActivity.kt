package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simple.raceremote.ui.theme.RaceRemoteTheme

private const val UNDEFINED = -1

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
        Scaffold() {
            Content()
        }
    }
}

@Composable
fun Content(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val activePointer = remember { mutableStateOf(UNDEFINED) }

    Row(modifier = modifier) {
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape),
        ) {
//            Log.d("fuck", "horizontal = $it")
        }
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape),
            orientation = Orientation.Vertical
        ) {
//            Log.d("fuck", "vertical = $it")
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}