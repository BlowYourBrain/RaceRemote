package vea.raceremote.app

import androidx.compose.runtime.Composable
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import vea.raceremote.features.remote_control.presentation.view.Actions
import vea.raceremote.control.CarControlScreen
import vea.raceremote.ui.theme.RaceRemoteTheme

@Preview
@Composable
fun AppPreview() {
    App()
}

@Composable
fun App() {
    RaceRemoteTheme(darkTheme = true) {
        Surface { CarControlScreen() }
    }
}

@Preview
@Composable
private fun ActionsPreview() {
    Actions(Modifier, emptyList())
}
