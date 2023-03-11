package com.simple.raceremote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class Palette(
    val colors: Colors,
    val customColors: CustomColors,
)

class CustomColors(
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color
)

private val _DarkColorPalette = darkColors(
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

private val _CustomDarkColorPalette = CustomColors(
    primaryContainer = DARK_PRIMARY_CONTAINER,
    onPrimaryContainer = DARK_ON_PRIMARY_CONTAINER,
    secondaryContainer = DARK_SECONDARY_CONTAINER,
    onSecondaryContainer = DARK_ON_SECONDARY_CONTAINER,
    tertiary = DARK_TERTIARY,
    onTertiary = DARK_ON_TERTIARY,
    tertiaryContainer = DARK_TERTIARY_CONTAINER,
    onTertiaryContainer = DARK_ON_TERTIARY_CONTAINER,
    errorContainer = DARK_ERROR_CONTAINER,
    onErrorContainer = DARK_ON_ERROR_CONTAINER,
    surfaceVariant = DARK_SURFACE_VARIANT,
    onSurfaceVariant = DARK_ON_SURFACE_VARIANT,
    outline = DARK_OUTLINE,
    outlineVariant = DARK_OUTLINE_VARIANT
)

private val _LightColorPalette = lightColors(
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


private val _CustomLightColorPalette = CustomColors(
    primaryContainer = LIGHT_PRIMARY_CONTAINER,
    onPrimaryContainer = LIGHT_ON_PRIMARY_CONTAINER,
    secondaryContainer = LIGHT_SECONDARY_CONTAINER,
    onSecondaryContainer = LIGHT_ON_SECONDARY_CONTAINER,
    tertiary = LIGHT_TERTIARY,
    onTertiary = LIGHT_ON_TERTIARY,
    tertiaryContainer = LIGHT_TERTIARY_CONTAINER,
    onTertiaryContainer = LIGHT_ON_TERTIARY_CONTAINER,
    errorContainer = LIGHT_ERROR_CONTAINER,
    onErrorContainer = LIGHT_ON_ERROR_CONTAINER,
    surfaceVariant = LIGHT_SURFACE_VARIANT,
    onSurfaceVariant = LIGHT_ON_SURFACE_VARIANT,
    outline = LIGHT_OUTLINE,
    outlineVariant = LIGHT_OUTLINE_VARIANT
)

private val DarkColorPalette = Palette(
    colors = _DarkColorPalette,
    customColors = _CustomDarkColorPalette
)

private val LightColorPalette = Palette(
    colors = _LightColorPalette,
    customColors = _CustomLightColorPalette
)

val palette: Palette
    @Composable
    get() =
        if (isSystemInDarkTheme())
            DarkColorPalette
        else
            LightColorPalette

fun getPalette(isSystemInDarkTheme: Boolean): Palette =
    if (isSystemInDarkTheme)
        DarkColorPalette
    else
        LightColorPalette

@Composable
fun RaceRemoteTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val androidThemeColors = getPalette(darkTheme).colors

    MaterialTheme(
        colors = androidThemeColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
