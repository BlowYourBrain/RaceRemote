package com.simple.raceremote.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simple.raceremote.features.BluetoothPermissionRationale
import com.simple.raceremote.features.remote_control.presentation.ActionsViewModel
import com.simple.raceremote.features.remote_control.presentation.model.WifiEnterPasswordDialog
import com.simple.raceremote.features.remote_control.presentation.view.EnterPasswordDialog
import com.simple.raceremote.features.remote_control.presentation.view.RemoteControlScreen
import com.simple.raceremote.ui.views.SidePanel
import com.simple.raceremote.utils.bluetooth.hasBluetoothPermissions
import org.koin.androidx.compose.getViewModel

private const val CONTENT_TOP_PADDING = 12
private const val CONTENT_BOTTOM_PADDING = 24

@Composable
fun AppScaffold() {
    val actionsViewModel = getViewModel<ActionsViewModel>()
    val sidePanelContent: MutableState<@Composable () -> Unit> = remember {
        mutableStateOf(value = {})
    }
    val isPanelOpen: State<Boolean> = actionsViewModel.isPanelOpen.collectAsState(initial = false)
    val openDialog =
        actionsViewModel.openEnterPasswordDialog.collectAsState(initial = WifiEnterPasswordDialog.Close)

    Scaffold() {
        openDialog.value.let {
            if (it is WifiEnterPasswordDialog.Open) {
                EnterPasswordDialog(
                    it.title,
                    onConfirm = { password ->
                        actionsViewModel.connectWifi(it.title, password)
                        actionsViewModel.closeEnterPasswordDialog()
                    },
                    onDismiss = {
                        actionsViewModel.closeEnterPasswordDialog()
                    }
                )
            }
        }

        SidePanel(
            isOpened = isPanelOpen,
            updateIsOpened = { actionsViewModel.isOpened(it) },
            itemProvider = sidePanelContent
        ) {
            Column {
                Box(
                    Modifier.padding(
                        top = CONTENT_TOP_PADDING.dp,
                        bottom = CONTENT_BOTTOM_PADDING.dp
                    )
                ) {
                    AppScaffold(
                        startScreen = getStartScreen(),
                        sidePanelContent = sidePanelContent,
                    )
                }
            }
        }
    }
}

@Composable
fun AppScaffold(
    startScreen: Screens,
    sidePanelContent: MutableState<@Composable () -> Unit>,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startScreen.name
    ) {
        composable(Screens.RemoteControl.name) {
            RemoteControlScreen(
                sidePanelContent = sidePanelContent
            )
        }
        composable(Screens.BluetoothPermissionsRationale.name) {
            BluetoothPermissionRationale(
                onApply = {
                    navController.navigate(
                        route = Screens.RemoteControl.name,
                        navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build()
                    )
                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun getStartScreen(): Screens = with(LocalContext.current) {
    when {
        hasBluetoothPermissions() -> Screens.RemoteControl
        else -> Screens.BluetoothPermissionsRationale
    }
}