package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/** The selectable designs: the original night blue and the sober Material variants (CP-181). */
enum class ThemeStyle(val label: String) {
    NIGHT("Nachtblau"),
    BLUE("Blau"),
    PURPLE("Lila"),
}

/** Light or dark; the night-blue design is always dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Hell"),
    DARK("Dunkel"),
}

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** The Material scheme of a sober [style], or `null` for the night-blue design. */
fun soberColorScheme(style: ThemeStyle, dark: Boolean): ColorScheme? = when (style) {
    ThemeStyle.NIGHT -> null
    ThemeStyle.BLUE -> if (dark) planerDarkColors else planerLightColors
    ThemeStyle.PURPLE -> if (dark) planerPurpleDarkColors else planerPurpleLightColors
}

/** Die Haupt-Theme-Composable der Anwendung; ohne Angaben das Nachtblau-Design (ADR-0014). */
@Composable
fun AppTheme(
    style: ThemeStyle = ThemeStyle.NIGHT,
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val sober = soberColorScheme(style, dark)
    val palette = sober?.let { paletteOf(it, dark) } ?: nightPalette
    // Written before the content reads the tokens, so the whole tree composes with the new palette in one pass.
    if (NightColors.palette != palette) NightColors.palette = palette
    MaterialTheme(
        colorScheme = sober ?: nightColorScheme(),
        typography = if (sober == null) nightTypography() else Typography(),
        shapes = if (sober == null) nightShapes() else Shapes(),
        content = content,
    )
}
