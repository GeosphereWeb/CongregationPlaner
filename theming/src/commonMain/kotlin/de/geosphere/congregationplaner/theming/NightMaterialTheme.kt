package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Nachtblau-Design als Material-Farbschema (ADR-0014). */
fun nightColorScheme(): ColorScheme = darkColorScheme(
    primary = NightColors.Primary,
    onPrimary = NightColors.OnBackground,
    primaryContainer = NightColors.HeaderBottom,
    onPrimaryContainer = NightColors.OnBackground,
    secondary = NightColors.Lavender,
    onSecondary = NightColors.Background,
    secondaryContainer = NightColors.HeaderBottom,
    onSecondaryContainer = NightColors.OnBackground,
    tertiary = NightColors.Mint,
    tertiaryContainer = NightColors.MintContainer,
    background = NightColors.Background,
    onBackground = NightColors.OnBackground,
    surface = NightColors.Background,
    onSurface = NightColors.OnBackground,
    surfaceVariant = NightColors.Surface,
    onSurfaceVariant = NightColors.OnBackgroundMuted,
    surfaceContainerLowest = NightColors.HeaderTop,
    surfaceContainerLow = NightColors.NavBar,
    surfaceContainer = NightColors.Surface,
    surfaceContainerHigh = NightColors.SurfaceRaised,
    surfaceContainerHighest = NightColors.BorderStrong,
    outline = NightColors.BorderStrong,
    outlineVariant = NightColors.Border,
    error = NightColors.Coral,
    onError = NightColors.Background,
    errorContainer = NightColors.CoralContainer,
    onErrorContainer = NightColors.Coral,
    inverseSurface = NightColors.OnBackground,
    inverseOnSurface = NightColors.Background,
    inversePrimary = NightColors.Primary,
)

/** Space Grotesk für alle Stile, JetBrains Mono nur für `labelSmall`. */
@Composable
fun nightTypography(): Typography {
    val grotesk = spaceGroteskFamily()
    val base = Typography()
    fun TextStyle.night() = copy(fontFamily = grotesk)
    return Typography(
        displayLarge = base.displayLarge.night(),
        displayMedium = base.displayMedium.night(),
        displaySmall = base.displaySmall.night(),
        headlineLarge = base.headlineLarge.night(),
        headlineMedium = base.headlineMedium.night().copy(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
        ),
        headlineSmall = base.headlineSmall.night().copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.night().copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.night().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.night().copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.night().copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.night().copy(fontSize = 15.sp),
        bodySmall = base.bodySmall.night().copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.night().copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.night().copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(
            fontFamily = jetBrainsMonoFamily(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.em,
        ),
    )
}

fun nightShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
