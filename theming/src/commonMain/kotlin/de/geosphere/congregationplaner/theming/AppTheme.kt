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



val baseColor1 = Color(0xFF27345B)
val baseColor2 = Color(0xFF34275b)
val baseColor3 = Color(0xFF274e5b)
val baseColor4 = Color(0xFF5b4e27)

data class ColorTone(
    val brandTwilightIndigo50: Color = Color(0xFFDDE2F0),
    val brandTwilightIndigo100: Color = Color(0xFFCBD3E9),
    val brandTwilightIndigo200: Color = Color(0xFFA8B4DA),
    val brandTwilightIndigo300: Color = Color(0xFF8495CA),
    val brandTwilightIndigo400: Color = Color(0xFF6077BB),
    val brandTwilightIndigo500: Color = Color(0xFF465DA2),
    val brandTwilightIndigo600: Color = Color(0xFF36487F),
    val brandTwilightIndigo700: Color = Color(0xFF27345B),
    val brandTwilightIndigo800: Color = Color(0xFF182037),
    val brandTwilightIndigo900: Color = Color(0xFF080B14),
)

data class Lightening(
    val lightening_1: Color = Color(0xFF27345b),
    val lightening_2: Color = Color(0xFF344065),
    val lightening_3: Color = Color(0xFF404c6e),
    val lightening_4: Color = Color(0xFF4d5878),
    val lightening_5: Color = Color(0xFF5a6482),
    val lightening_6: Color = Color(0xFF67708b),
    val lightening_7: Color = Color(0xFF737c95),
    val lightening_8: Color = Color(0xFF80889f),
    val lightening_9: Color = Color(0xFF8d94a8),
    val lightening_10: Color = Color(0xFF999fb2),
    val lightening_11: Color = Color(0xFFa6abbb),
    val lightening_12: Color = Color(0xFFb3b7c5),
    val lightening_13: Color = Color(0xFFbfc3cf),
    val lightening_14: Color = Color(0xFFcccfd8),
    val lightening_15: Color = Color(0xFFd9dbe2),
    val lightening_16: Color = Color(0xFFe6e7ec),
    val lightening_17: Color = Color(0xFFf2f3f5),
    val lightening_18: Color = Color(0xFFffffff),
)

data class Grayeing(
    val grayeing_1: Color = Color(0xFF27345b),
    val grayeing_2: Color = Color(0xFF2c385d),
    val grayeing_3: Color = Color(0xFF313d5f),
    val grayeing_4: Color = Color(0xFF374162),
    val grayeing_5: Color = Color(0xFF3c4664),
    val grayeing_6: Color = Color(0xFF414a66),
    val grayeing_7: Color = Color(0xFF464f68),
    val grayeing_8: Color = Color(0xFF4c536a),
    val grayeing_9: Color = Color(0xFF51586c),
    val grayeing_10: Color = Color(0xFF565c6f),
    val grayeing_11: Color = Color(0xFF5b6171),
    val grayeing_12: Color = Color(0xFF616573),
    val grayeing_13: Color = Color(0xFF666a75),
    val grayeing_14: Color = Color(0xFF6b6e77),
    val grayeing_15: Color = Color(0xFF707379),
    val grayeing_16: Color = Color(0xFF76777c),
    val grayeing_17: Color = Color(0xFF7b7c7e),
    val grayeing_18: Color = Color(0xFF808080),
)

data class Darkening(
    val darkening_1: Color = Color(0xFF27345b),
    val darkening_2: Color = Color(0xFF253156),
    val darkening_3: Color = Color(0xFF222e50),
    val darkening_4: Color = Color(0xFF202b4b),
    val darkening_5: Color = Color(0xFF1e2846),
    val darkening_6: Color = Color(0xFF1c2540),
    val darkening_7: Color = Color(0xFF19223b),
    val darkening_8: Color = Color(0xFF171f36),
    val darkening_9: Color = Color(0xFF151c30),
    val darkening_10: Color = Color(0xFF12182b),
    val darkening_11: Color = Color(0xFF101525),
    val darkening_12: Color = Color(0xFF0e1220),
    val darkening_13: Color = Color(0xFF0b0f1b),
    val darkening_14: Color = Color(0xFF090c15),
    val darkening_15: Color = Color(0xFF070910),
    val darkening_16: Color = Color(0xFF05060b),
    val darkening_17: Color = Color(0xFF020305),
    val darkening_18: Color = Color(0xFF000000),
)
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
    val werner2: Color
)

// Erstellt den Speicherort im Compose-Baum
val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        brandCustom = Color.Unspecified,
        onBrandCustom = Color.Unspecified,
        success = Color.Unspecified,
        successContainer = Color.Unspecified,
        werner1 = Darkening().darkening_17,
        werner2 = Darkening().darkening_10
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
    werner1 = Lightening().lightening_17,
    werner2 = Lightening().lightening_7
)


private fun darkCustomColors() = CustomColors(
    brandCustom = Color(0xFF757DE8),
    onBrandCustom = Color(0xFF000000),
    success = Color(0xFF81C784),
    successContainer = Color(0xFF0C2411),
    werner1 = Darkening().darkening_12,
    werner2 = Darkening().darkening_17
)

private fun lightColorScheme(): ColorScheme = androidx.compose.material3.lightColorScheme(
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

private fun darkColorScheme(): ColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
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
