package com.simple.raceremote.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.CornerShapes
import com.simple.raceremote.ui.theme.Padding

//TODO вынести в Strings
private const val BACK_BUTTON_TEXT = "Назад"

@Composable
fun NavigationPanel(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            modifier = modifier.padding(Padding.Content),
            onClick = { onBackClick?.invoke() },
            shape = CornerShapes.SmallItem,
            contentPadding = PaddingValues(Padding.Content)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_back_ios_24),
                contentDescription = null
            )
            Text(text = BACK_BUTTON_TEXT)
        }

        content
    }

}