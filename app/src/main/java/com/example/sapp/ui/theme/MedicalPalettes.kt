package com.example.sapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Selectable palettes for the patient and caretaker app.
 *
 * All text/background pairs target WCAG AA contrast (4.5:1 for body text),
 * and no meaning is conveyed by color alone (always pair with text or icons).
 */
enum class MedicalThemeVariant {
    /** Calm blue and teal. Default. Clinical but not cold. */
    CALM_CLINICAL,

    /** Soft lavender and sage. Gentler look for long-term care contexts. */
    WARM_CARE,

    /** Maximum contrast on white/black for low vision or bright environments. */
    HIGH_CONTRAST
}

// ---------- Calm Clinical ----------
private val CalmClinicalLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF0A2E5C),
    secondary = Color(0xFF00695C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC3EEE6),
    onSecondaryContainer = Color(0xFF00332C),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E8EF),
    onSurfaceVariant = Color(0xFF42505C),
    outline = Color(0xFF73808C),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val CalmClinicalDark = darkColorScheme(
    primary = Color(0xFF9CC8FF),
    onPrimary = Color(0xFF00325E),
    primaryContainer = Color(0xFF00497F),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF7FD3C6),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFC3EEE6),
    tertiary = Color(0xFFFFB86C),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFE2E8ED),
    surface = Color(0xFF1A2226),
    onSurface = Color(0xFFE2E8ED),
    surfaceVariant = Color(0xFF3F4A52),
    onSurfaceVariant = Color(0xFFC0C8D0),
    outline = Color(0xFF8A949C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

// ---------- Warm Care ----------
private val WarmCareLight = lightColorScheme(
    primary = Color(0xFF5E4B9B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DEFF),
    onPrimaryContainer = Color(0xFF1F0F4D),
    secondary = Color(0xFF386A20),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB8F397),
    onSecondaryContainer = Color(0xFF082100),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFEDE0E4),
    onSurfaceVariant = Color(0xFF4D444D),
    outline = Color(0xFF7F747E),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val WarmCareDark = darkColorScheme(
    primary = Color(0xFFC8BEFF),
    onPrimary = Color(0xFF2F1F66),
    primaryContainer = Color(0xFF46368A),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF9CD67C),
    onSecondary = Color(0xFF133800),
    secondaryContainer = Color(0xFF2A4F13),
    onSecondaryContainer = Color(0xFFB8F397),
    tertiary = Color(0xFFF0C048),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF171214),
    onBackground = Color(0xFFEDE0E2),
    surface = Color(0xFF201A1D),
    onSurface = Color(0xFFEDE0E2),
    surfaceVariant = Color(0xFF4D444D),
    onSurfaceVariant = Color(0xFFD0C3CD),
    outline = Color(0xFF998F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ---------- High Contrast ----------
private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF003A8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF005A3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9EF2C8),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF6B4E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF1F1500),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF1A1A1A),
    outline = Color(0xFF595959),
    error = Color(0xFF8C0009),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410001)
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFA9C8FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF004A9E),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFF8FE3B8),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF005236),
    onSecondaryContainer = Color(0xFF9EF2C8),
    tertiary = Color(0xFFFFD24D),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF4D3800),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFE6E6E6),
    outline = Color(0xFFA6A6A6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4)
)

internal fun medicalColorScheme(variant: MedicalThemeVariant, darkTheme: Boolean): ColorScheme =
    when (variant) {
        MedicalThemeVariant.CALM_CLINICAL -> if (darkTheme) CalmClinicalDark else CalmClinicalLight
        MedicalThemeVariant.WARM_CARE -> if (darkTheme) WarmCareDark else WarmCareLight
        MedicalThemeVariant.HIGH_CONTRAST -> if (darkTheme) HighContrastDark else HighContrastLight
    }
