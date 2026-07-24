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


data class BaseColors(
    val baseColor1: Color = Color(0xFF2b3d97),
    val baseColor2: Color = Color(0xFF4f2b97),
    val baseColor3: Color = Color(0xFF2b7397),
    val baseColor4: Color = Color(0xFF97852b)
)
data class ColorTone(
    val brandTwilightIndigo50: Color = Color(0xFFE3E7F7),
    val brandTwilightIndigo100: Color = Color(0xFFD0D5F1),
    val brandTwilightIndigo200: Color = Color(0xFFA8B2E6),
    val brandTwilightIndigo300: Color = Color(0xFF808FDB),
    val brandTwilightIndigo400: Color = Color(0xFF586CD0),
    val brandTwilightIndigo500: Color = Color(0xFF364DBF),
    val brandTwilightIndigo600: Color = Color(0xFF2B3D97),
    val brandTwilightIndigo700: Color = Color(0xFF202D6F),
    val brandTwilightIndigo800: Color = Color(0xFF141D48),
    val brandTwilightIndigo900: Color = Color(0xFF090D20),
)
data class ColorTone2(
    val brandTwilightIndigo50: Color =  Color(0xFFEAE3F7),
    val brandTwilightIndigo100: Color = Color(0xFFDBD0F1),
    val brandTwilightIndigo200: Color = Color(0xFFBDA8E6),
    val brandTwilightIndigo300: Color = Color(0xFF9E80DB),
    val brandTwilightIndigo400: Color = Color(0xFF8058D0),
    val brandTwilightIndigo500: Color = Color(0xFF6436BF),
    val brandTwilightIndigo600: Color = Color(0xFF4F2B97),
    val brandTwilightIndigo700: Color = Color(0xFF3A206F),
    val brandTwilightIndigo800: Color = Color(0xFF251448),
    val brandTwilightIndigo900: Color = Color(0xFF110920),
)
data class ColorTone3(
    val brandTwilightIndigo50: Color =  Color(0xFFE3F1F7),
    val brandTwilightIndigo100: Color = Color(0xFFD0E6F1),
    val brandTwilightIndigo200: Color = Color(0xFFA8D1E6),
    val brandTwilightIndigo300: Color = Color(0xFF80BDDB),
    val brandTwilightIndigo400: Color = Color(0xFF58A8D0),
    val brandTwilightIndigo500: Color = Color(0xFF3691BF),
    val brandTwilightIndigo600: Color = Color(0xFF2B7397),
    val brandTwilightIndigo700: Color = Color(0xFF20556F),
    val brandTwilightIndigo800: Color = Color(0xFF143748),
    val brandTwilightIndigo900: Color = Color(0xFF091820),
)
data class ColorTone4(
val brandTwilightIndigo50: Color =  Color(0xFFF7F4E3),
val brandTwilightIndigo100: Color = Color(0xFFF1ECD0),
val brandTwilightIndigo200: Color = Color(0xFFE6DCA8),
val brandTwilightIndigo300: Color = Color(0xFFDBCC80),
val brandTwilightIndigo400: Color = Color(0xFFD0BC58),
val brandTwilightIndigo500: Color = Color(0xFFBFA836),
val brandTwilightIndigo600: Color = Color(0xFF97852B),
val brandTwilightIndigo700: Color = Color(0xFF6F6220),
val brandTwilightIndigo800: Color = Color(0xFF483F14),
val brandTwilightIndigo900: Color = Color(0xFF201C09),
)

data class Lightening(
    val lightening_1: Color = Color(0xFF2b3d97),
    val lightening_2: Color = Color(0xFF3b4c9f),
    val lightening_3: Color = Color(0xFF4c5ba7),
    val lightening_4: Color = Color(0xFF5c6aaf),
    val lightening_5: Color = Color(0xFF6c79b7),
    val lightening_6: Color = Color(0xFF7d88bf),
    val lightening_7: Color = Color(0xFF8d97c7),
    val lightening_8: Color = Color(0xFF9da5cf),
    val lightening_9: Color = Color(0xFFadb4d7),
    val lightening_10: Color = Color(0xFFbec3df),
    val lightening_11: Color = Color(0xFFced2e7),
    val lightening_12: Color = Color(0xFFdee1ef),
    val lightening_13: Color = Color(0xFFeff0f7),
    val lightening_14: Color = Color(0xFFffffff),

)

data class Grayeing(
    val grayeing_1: Color = Color(0xFF2b3d97),
    val grayeing_2: Color = Color(0xFF324295),
    val grayeing_3: Color = Color(0xFF384793),
    val grayeing_4: Color = Color(0xFF3f4c92),
    val grayeing_5: Color = Color(0xFF455290),
    val grayeing_6: Color = Color(0xFF4c578e),
    val grayeing_7: Color = Color(0xFF525c8c),
    val grayeing_8: Color = Color(0xFF59618b),
    val grayeing_9: Color = Color(0xFF5f6689),
    val grayeing_10: Color = Color(0xFF666b87),
    val grayeing_11: Color = Color(0xFF6c7185),
    val grayeing_12: Color = Color(0xFF737684),
    val grayeing_13: Color = Color(0xFF797b82),
    val grayeing_14: Color = Color(0xFF808080),

)

data class Darkening(
    val darkening_1: Color = Color(0xFF2b3d97),
    val darkening_2: Color = Color(0xFF28388b),
    val darkening_3: Color = Color(0xFF243480),
    val darkening_4: Color = Color(0xFF212f74),
    val darkening_5: Color = Color(0xFF1e2a69),
    val darkening_6: Color = Color(0xFF1a265d),
    val darkening_7: Color = Color(0xFF172151),
    val darkening_8: Color = Color(0xFF141c46),
    val darkening_9: Color = Color(0xFF11173a),
    val darkening_10: Color = Color(0xFF0d132e),
    val darkening_11: Color = Color(0xFF0a0e23),
    val darkening_12: Color = Color(0xFF070917),
    val darkening_13: Color = Color(0xFF03050c),
    val darkening_14: Color = Color(0xFF000000),

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
        brandCustom = BaseColors().baseColor1,
        onBrandCustom = BaseColors().baseColor2,
        success = Color.Unspecified,
        successContainer = Color.Unspecified,
        werner1 = Darkening().darkening_14,
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
    werner1 = Lightening().lightening_14,
    werner2 = Lightening().lightening_7
)


private fun darkCustomColors() = CustomColors(
    brandCustom = Color(0xFF757DE8),
    onBrandCustom = Color(0xFF000000),
    success = Color(0xFF81C784),
    successContainer = Color(0xFF0C2411),
    werner1 = Darkening().darkening_12,
    werner2 = Darkening().darkening_14
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
