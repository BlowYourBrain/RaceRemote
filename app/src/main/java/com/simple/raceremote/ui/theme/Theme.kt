package com.simple.raceremote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = DARK_PRIMARY,
    onPrimary = DARK_ON_PRIMARY,
    secondary = DARK_SECONDARY,
    onSecondary = DARK_ON_SECONDARY,
    background = DARK_BACKGROUND,
    onBackground = DARK_ON_BACKGROUND,
    surface = DARK_SURFACE,
    onSurface = DARK_ON_SURFACE,
    error = DARK_ERROR,
    onError = DARK_ON_ERROR,
)

private val LightColorPalette = lightColors(
    primary = LIGHT_PRIMARY,
    onPrimary = LIGHT_ON_PRIMARY,
    secondary = LIGHT_SECONDARY,
    onSecondary = LIGHT_ON_SECONDARY,
    background = LIGHT_BACKGROUND,
    onBackground = LIGHT_ON_BACKGROUND,
    surface = LIGHT_SURFACE,
    onSurface = LIGHT_ON_SURFACE,
    error = LIGHT_ERROR,
    onError = LIGHT_ON_ERROR,
)

private val LegacyDarkColorPalette = darkColors(
    primary = Color.White,
    primaryVariant = Purple700,
    secondary = Teal200,
    background = DarkGreyColor,
    onBackground = LightGrey,
    surface = DarkColor,
    onSurface = LightColor,
)

private val LegacyLightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun RaceRemoteTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
