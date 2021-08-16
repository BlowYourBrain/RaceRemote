package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.simple.raceremote.ui.theme.RaceRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        window?.statusBarColor = Color.TRANSPARENT
    }

}

@Composable
fun App() {
    RaceRemoteTheme(darkTheme = true) {
        ProvideWindowInsets() {
            Scaffold() {
                Content(Modifier.padding(it))
            }
        }
    }
}

@Composable
fun Content(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)

    Row(modifier = modifier) {
        Box(
            Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape)
        ) {
            Slider() {
                Log.d("fuck", "horizontal = $it")
            }
        }
        Box(
            Modifier
                .weight(1f)
                .padding(
                    top = 96.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
                .clip(shape)
        ) {
            Slider(orientation = Orientation.Vertical) {
                Log.d("fuck", "vertical = $it")
            }
        }

    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}