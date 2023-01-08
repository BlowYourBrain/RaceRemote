package com.simple.raceremote.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding

private val BUTTONS_RANGE = 64.dp

@Composable
fun BluetoothPermissionRationale(
    onApply: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = CornerShapes.HugeItem,
                elevation = Elevation.onSurface,
            ) {
                Text(
                    modifier = Modifier.padding(Padding.Content),
                    text = stringResource(id = R.string.bluetooth_permissions_explanation),
                    style = MaterialTheme.typography.h5,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(Padding.Content),
                horizontalArrangement = Arrangement.spacedBy(BUTTONS_RANGE)
            ) {
                TextButton(text = stringResource(id = R.string.decline_permission)) { onDismiss?.invoke() }
                TextButton(text = stringResource(id = R.string.apply_permission)) { onApply?.invoke() }
            }
        }
    }
}

@Composable
private fun TextButton(
    modifier: Modifier = Modifier,
    text: String,
    action: (() -> Unit)? = null
) {
    Button(
        modifier = modifier,
        onClick = { action?.invoke() },
        shape = CornerShapes.SmallItem,
        colors = ButtonDefaults.buttonColors(
            MaterialTheme.colors.surface,
            MaterialTheme.colors.onSurface
        ),
        contentPadding = PaddingValues(Padding.Content)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h6
        )
    }
}
