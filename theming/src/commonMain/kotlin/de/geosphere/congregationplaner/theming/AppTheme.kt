package de.geosphere.congregationplaner.theming

import androidx.compose.material3.MaterialTheme
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
    val btnContainerBorderColor: Color,
    val btnContainerColorDisabled: Color,
    val btnContentColorDisabled: Color,
)

// Erstellt den Speicherort im Compose-Baum
val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        brandCustom = BaseColors().indigo,
        onBrandCustom = Lightening().lightening_12,
        success = Color.Unspecified,
        successContainer = Color.Unspecified,
        werner1 = Darkening().darkening_14,
        werner2 = Darkening().darkening_10,
        btnContainerColor = ColorIndigo().shade700,
        btnContentColor = Color.Unspecified,
        btnContainerBorderColor = Color.Unspecified,
        btnContainerColorDisabled = Color.Unspecified,
        btnContentColorDisabled = Color.Unspecified,
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
// 3. Die Haupt-Theme-Composable für Ihre Anwendung (nur dunkel, siehe ADR-0014)
// ============================================================================
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCustomColors provides darkCustomColors()) {
        MaterialTheme(
            colorScheme = nightColorScheme(),
            typography = nightTypography(),
            shapes = nightShapes(),
            content = content,
        )
    }
}

private fun darkCustomColors() = CustomColors(
    brandCustom = Color.Magenta,
    onBrandCustom = Color(0xFF000000),
    success = Color(0xFF81C784),
    successContainer = Color(0xFF0C2411),
    werner1 = Darkening().darkening_12,
    werner2 = Darkening().darkening_14,
    btnContainerColor = Lightening().lightening_8.copy(0.3f),
    btnContentColor = Lightening().lightening_8,
    btnContainerBorderColor = Lightening().lightening_8.copy(0.6f),
    btnContainerColorDisabled = Lightening().lightening_8.copy(0.1f),
    btnContentColorDisabled = Darkening().darkening_4,
)
