package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color



// ============================================================================
// 1. Eigene Farb-Datenklasse für zusätzliche Variablen (außerhalb von Material 3)
// ============================================================================
@Immutable
data class CustomColors(
    val brandCustom: Color,
    val onBrandCustom: Color,
    val success: Color,
    val successContainer: Color,
    val werner1: Color,
    val werner2: Color,
    val btnContainerColor: Color,
    val btnContentColor: Color,
    val btnContainerColorDisabled: Color,
    val btnContentColorDisabled: Color,
)

// Erstellt den Speicherort im Compose-Baum
val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        brandCustom = BaseColors().baseColor1,
        onBrandCustom = BaseColors().baseColor2,
        success = Color.Unspecified,
        successContainer = Color.Unspecified,
        werner1 = Darkening().darkening_14,
        werner2 = Darkening().darkening_10,
        btnContainerColor = ColorTone().brandTwilightIndigo700,
        btnContentColor = Color.Unspecified,
        btnContainerColorDisabled = Color.Unspecified,
        btnContentColorDisabled = Color.Unspecified
    )
}

// ============================================================================
// 2. KOTLIN ERWEITERUNG: Erlaubt den Aufruf über "MaterialTheme.customColors"
// ============================================================================
val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColors.current

// ============================================================================
// 3. Zentrales Konfigurationsobjekt (für Previews oder Unit-Tests)
// ============================================================================
object AppTheme {
    fun colorScheme(isDark: Boolean): ColorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    fun typography(): Typography = Typography() // Nutzt M3-Standard oder Ihre 'createTypography()'
}

// ============================================================================
// 4. Die Haupt-Theme-Composable für Ihre Anwendung
// ============================================================================
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = AppTheme.colorScheme(useDarkTheme)
    val customColors = if (useDarkTheme) darkCustomColors() else lightCustomColors()

    // 1. Stellt Ihre eigenen Farben bereit
    CompositionLocalProvider(LocalCustomColors provides customColors) {
        // 2. Initialisiert das offizielle Material 3 Theme
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTheme.typography(),
            content = content,
        )
    }
}

// ============================================================================
// 5. Farbpaletten-Definitionen (M3 & Custom)
// ============================================================================

private fun lightCustomColors() = CustomColors(
    brandCustom = Color(0xFF3F51B5),
    onBrandCustom = Color(0xFFFFFFFF),
    success = Color(0xFF2E7D32),
    successContainer = Color(0xFFE8F5E9),
    werner1 = Lightening().lightening_14,
    werner2 = Lightening().lightening_7,
    btnContainerColor = ColorTone().brandTwilightIndigo100,
    btnContentColor = Color.Unspecified,
    btnContainerColorDisabled = Color.Unspecified,
    btnContentColorDisabled = Color.Unspecified
)


private fun darkCustomColors() = CustomColors(
    brandCustom = Color(0xFF757DE8),
    onBrandCustom = Color(0xFF000000),
    success = Color(0xFF81C784),
    successContainer = Color(0xFF0C2411),
    werner1 = Darkening().darkening_12,
    werner2 = Darkening().darkening_14,
    btnContainerColor = Darkening().darkening_10,
    btnContentColor = ColorTone().brandTwilightIndigo200,
    btnContainerColorDisabled = Color.Unspecified,
    btnContentColorDisabled = ColorTone().brandTwilightIndigo200

)

private fun lightColorScheme(): ColorScheme = lightColorScheme(
    primary = lightCustomColors().brandCustom,
    onPrimary = lightCustomColors().onBrandCustom,
    primaryContainer = lightCustomColors().onBrandCustom,
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

private fun darkColorScheme(): ColorScheme = darkColorScheme(
    primary = darkCustomColors().brandCustom,
    onPrimary = darkCustomColors().onBrandCustom,
    primaryContainer = darkCustomColors().onBrandCustom,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD9E3),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
)
