package com.simple.raceremote.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.simple.raceremote.navigation.AppScaffold
import com.simple.raceremote.features.remote_control.presentation.view.Actions
import com.simple.raceremote.ui.theme.RaceRemoteTheme


@Preview
@Composable
fun AppPreview() {
    App()
}

@Composable
fun App() {
    RaceRemoteTheme(darkTheme = true) {
        AppScaffold()
    }
}


@Preview
@Composable
private fun ActionsPreview() {
    Actions(Modifier, emptyList())
}