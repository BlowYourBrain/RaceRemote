package com.simple.raceremote.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.simple.raceremote.R
import com.simple.raceremote.screens.bluetooth_devices.presentation.BluetoothEntity
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Elevation
import com.simple.raceremote.ui.theme.Padding
import com.simple.raceremote.ui.theme.Size


@Preview
@Composable
fun BluetoothItemCardPreview() {
    BluetoothItemCard(entity = BluetoothEntity("hello", "world", true))
}

@Composable
fun BluetoothItemCard(modifier: Modifier = Modifier, entity: BluetoothEntity) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { entity.onClick?.invoke() },
        elevation = Elevation.onSurface,
        shape = CornerShapes.SmallItem,
    ) {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(modifier = Modifier.padding(Padding.Content)) {
                Text(
                    text = entity.name,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = entity.macAddress,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body1
                )
            }
            Box(
                modifier = Modifier
                    .padding(Padding.Content)
                    .size(Size.Icon)
            ) {
                if (entity.isPaired) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_link_24),
                        contentDescription = null
                    )
                }
            }
        }
    }
}