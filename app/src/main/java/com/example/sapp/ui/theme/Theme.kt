package com.example.sapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun MedicalAppTheme(
    darkTheme: Boolean = false,
    variant: MedicalThemeVariant = MedicalThemeVariant.CALM_CLINICAL,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = medicalColorScheme(variant, darkTheme),
        typography = Typography, // from Type.kt
        content = content
    )
}
