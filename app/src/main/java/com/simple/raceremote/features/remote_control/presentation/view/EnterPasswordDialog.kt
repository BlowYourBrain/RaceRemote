package com.simple.raceremote.features.remote_control.presentation.view

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.simple.raceremote.R

@Composable
fun EnterPasswordDialog(title: String, onConfirm: ((String) -> Unit), onDismiss: () -> Unit) {
    val text = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        confirmButton = {
            DialogButton(text = stringResource(id = R.string.apply_dialog)) {
                onConfirm.invoke(
                    text.value
                )
            }
        },
        dismissButton = {
            DialogButton(text = stringResource(id = R.string.dismiss_dialog)) {
                onDismiss.invoke()
            }
        },
        title = { Text(text = title) },
        text = {
            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
    )
}

@Composable
fun DialogButton(text: String, onClick: (() -> Unit)) {
    TextButton(onClick = { onClick.invoke() }) {
        Text(text)
    }
}