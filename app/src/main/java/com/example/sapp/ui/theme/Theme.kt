package com.example.sapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    secondary = TealSecondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color.Black,
    surface = SurfaceLight,
    onSurface = Color.Black,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color.Black,
    secondary = TealSecondaryDark,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    error = ErrorRedDark,
    onError = Color.Black
)

@Composable
fun MedicalAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography, // from Typography.kt we defined earlier
        content = content
    )
}
