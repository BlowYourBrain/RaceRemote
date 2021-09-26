package com.simple.raceremote.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.simple.raceremote.R
import com.simple.raceremote.ui.theme.Padding

@Composable
fun NavigationPanel(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            modifier = Modifier
                .padding(Padding.Content)
                .wrapContentWidth(),
            text = stringResource(id = R.string.back_button),
            icon = R.drawable.ic_baseline_arrow_back_ios_24
        ) { onBackClick?.invoke() }

        content?.invoke()
    }

}