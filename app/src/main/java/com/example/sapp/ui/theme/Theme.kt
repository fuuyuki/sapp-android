package com.example.sapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedicalAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    variant: MedicalThemeVariant = MedicalThemeVariant.CALM_CLINICAL,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = medicalColorScheme(variant, darkTheme),
        typography = Typography, // from Type.kt
        shapes = AppShapes,
        content = content
    )
}
