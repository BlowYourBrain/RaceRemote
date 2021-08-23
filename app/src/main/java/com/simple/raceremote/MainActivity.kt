package com.simple.raceremote

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.simple.raceremote.navigation.AppNavHost
import com.simple.raceremote.navigation.Screens
import com.simple.raceremote.screens.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme
import com.simple.raceremote.utils.BluetoothHelper
import com.simple.raceremote.utils.enableBluetooth
import com.simple.raceremote.utils.hasBluetoothPermissions
import com.simple.raceremote.utils.isBluetoothEnabled


private const val PERMISSION_CODE = 42

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BluetoothHelper.registerReceiver(this)
        window?.apply {
            //TODO сменить на актуальное API
            decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        SYSTEM_UI_FLAG_FULLSCREEN
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        window?.statusBarColor = Color.TRANSPARENT

        if (!isBluetoothEnabled()) {
            enableBluetooth(this)
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            if (
//                ContextCompat.checkSelfPermission(
//                    baseContext,
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//                    PERMISSION_CODE
//                )
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothHelper.unregisterReceiver(this)
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//
//        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() &&
//            grantResults[0] == PackageManager.PERMISSION_GRANTED
//        ) {
//            setupBluetooth()
//        } else {
//            Toast.makeText(this, "permissions not granted", Toast.LENGTH_SHORT).show()
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }
}


@Preview
@Composable
fun AppPreview() {
    App()
}

@Composable
fun App() {
    RaceRemoteTheme(darkTheme = true) {
        AppNavHost(startScreen = getStartScreen())
    }
}

@Composable
fun getStartScreen(): Screens = with(LocalContext.current) {
    if (hasBluetoothPermissions()) {
        Screens.RemoteControl
    } else {
        Screens.BluetoothPermissionsRationale
    }
}

@Preview
@Composable
private fun ActionsPreview() {
    Actions(Modifier, null)
}