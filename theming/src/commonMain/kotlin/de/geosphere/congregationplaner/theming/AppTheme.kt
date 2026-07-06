package de.geosphere.congregationplaner.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color

/**
 * Zentrale Theming-Konfiguration für alle Plattformen
 */
object AppTheme {
    
    fun colorScheme(isDark: Boolean): ColorScheme {
        return if (isDark) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }
    
    fun typography(): Typography {
        return createTypography()
    }
}

private fun lightColorScheme(): ColorScheme {
    return androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF6200EE),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005E),
        secondary = Color(0xFF03DAC6),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFFB1F8F1),
        onSecondaryContainer = Color(0xFF00201A),
        tertiary = Color(0xFFFF0266),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9E3),
        onTertiaryContainer = Color(0xFF3E001D),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFFFFBFE),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFBFE),
        onSurface = Color(0xFF1C1B1F),
    )
}

private fun darkColorScheme(): ColorScheme {
    return androidx.compose.material3.darkColorScheme(
        primary = Color(0xFFBB86FC),
        onPrimary = Color(0xFF3700B3),
        primaryContainer = Color(0xFF3700B3),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFF03DAC6),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF005047),
        onSecondaryContainer = Color(0xFFB1F8F1),
        tertiary = Color(0xFFFF0266),
        onTertiary = Color(0xFF53002C),
        tertiaryContainer = Color(0xFF73003F),
        onTertiaryContainer = Color(0xFFFFD9E3),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Color(0xFF1C1B1F),
        onBackground = Color(0xFFE6E1E6),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE6E1E6),
    )
}

private fun createTypography(): Typography {
    return Typography()
}
