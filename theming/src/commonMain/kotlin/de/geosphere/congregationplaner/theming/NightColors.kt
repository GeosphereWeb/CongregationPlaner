package de.geosphere.congregationplaner.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot

/** All color tokens of one design; [NightColors] reads them from the active palette. */
data class NightPalette(
    val background: Color,
    val surface: Color,
    val surfaceActive: Color,
    val border: Color,
    val borderStrong: Color,
    val navBar: Color,
    val surfaceDetail: Color,
    val surfaceRaised: Color,
    val headerTop: Color,
    val headerMiddle: Color,
    val headerBottom: Color,
    val primary: Color,
    val primaryDisabled: Color,
    val lavender: Color,
    val gold: Color,
    val goldContainer: Color,
    val mint: Color,
    val mintContainer: Color,
    val sky: Color,
    val skyContainer: Color,
    val skyBorder: Color,
    val coral: Color,
    val coralContainer: Color,
    val coralBorder: Color,
    val onBackground: Color,
    val onBackgroundBody: Color,
    val onBackgroundMuted: Color,
    val onBackgroundDisabled: Color,
    val placeholder: Color,
    /** Space Grotesk/JetBrains Mono instead of the system font. */
    val brandFonts: Boolean,
    /** Lavender glow at the bottom left of the header. */
    val headerGlow: Boolean,
)

/** The original night-blue design (board "Farbpalette"). */
val nightPalette = NightPalette(
    background = Color(0xFF0B1020),
    surface = Color(0xFF161D33),
    surfaceActive = Color(0xFF1A1430),
    border = Color(0xFF26304D),
    borderStrong = Color(0xFF2E3A5C),
    navBar = Color(0xFF10162A),
    surfaceDetail = Color(0xFF0E1428),
    surfaceRaised = Color(0xFF1C2440),
    headerTop = Color(0xFF0C0819),
    headerMiddle = Color(0xFF160F26),
    headerBottom = Color(0xFF2B1C48),
    primary = Color(0xFF7A55B5),
    primaryDisabled = Color(0xFF2A2240),
    lavender = Color(0xFFB9A2E0),
    gold = Color(0xFFE6B866),
    goldContainer = Color(0xFF3A2E16),
    mint = Color(0xFF6FCDB6),
    mintContainer = Color(0xFF12352F),
    sky = Color(0xFF86A6F2),
    skyContainer = Color(0xFF1B2850),
    skyBorder = Color(0xFF374F99),
    coral = Color(0xFFF2786A),
    coralContainer = Color(0xFF45201E),
    coralBorder = Color(0xFF793B38),
    onBackground = Color(0xFFE8ECF5),
    onBackgroundBody = Color(0xFFC3CADB),
    onBackgroundMuted = Color(0xFF8F9AB5),
    onBackgroundDisabled = Color(0xFF4A5578),
    placeholder = Color(0xFF7C87A3),
    brandFonts = true,
    headerGlow = true,
)

/** Maps a sober Material scheme onto the night tokens: flat header, no glow, system font. */
fun paletteOf(scheme: ColorScheme, dark: Boolean): NightPalette = NightPalette(
    background = scheme.background,
    surface = scheme.surfaceContainer,
    surfaceActive = scheme.secondaryContainer,
    border = scheme.outlineVariant,
    borderStrong = scheme.outline,
    navBar = scheme.surfaceContainer,
    surfaceDetail = scheme.surfaceContainerLow,
    surfaceRaised = scheme.surfaceContainerHigh,
    headerTop = scheme.surfaceContainer,
    headerMiddle = scheme.surfaceContainer,
    headerBottom = scheme.surfaceContainer,
    primary = scheme.primary,
    primaryDisabled = scheme.onSurface.copy(alpha = 0.12f),
    lavender = scheme.primary,
    gold = if (dark) Color(0xFFF0BF6C) else Color(0xFF7A5900),
    goldContainer = if (dark) Color(0xFF5C4300) else Color(0xFFFFDEA6),
    mint = scheme.tertiary,
    mintContainer = scheme.tertiaryContainer,
    sky = scheme.primary,
    skyContainer = scheme.primaryContainer,
    skyBorder = scheme.outline,
    coral = scheme.error,
    coralContainer = scheme.errorContainer,
    coralBorder = scheme.error,
    onBackground = scheme.onSurface,
    onBackgroundBody = scheme.onSurface,
    onBackgroundMuted = scheme.onSurfaceVariant,
    onBackgroundDisabled = scheme.onSurface.copy(alpha = 0.38f),
    placeholder = scheme.onSurfaceVariant,
    brandFonts = false,
    headerGlow = false,
)

/**
 * Color tokens of the app. The values come from the palette [AppTheme] activates, so code that reads the tokens
 * directly follows a theme switch.
 */
object NightColors {
    // shortcut: global snapshot state instead of a CompositionLocal, so the existing non-composable reads keep working;
    // replace once the tokens are migrated to MaterialTheme.colorScheme.
    /** The active palette; set by [AppTheme]. */
    var palette by mutableStateOf(nightPalette)

    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceActive get() = palette.surfaceActive
    val Border get() = palette.border
    val BorderStrong get() = palette.borderStrong

    /** Navigationsleiste unten. */
    val NavBar get() = palette.navBar

    /** Detailbereich und zweite Spalte. */
    val SurfaceDetail get() = palette.surfaceDetail

    /** Menüs und Dialoge. */
    val SurfaceRaised get() = palette.surfaceRaised

    val HeaderTop get() = palette.headerTop
    val HeaderMiddle get() = palette.headerMiddle
    val HeaderBottom get() = palette.headerBottom

    val Primary get() = palette.primary
    val PrimaryDisabled get() = palette.primaryDisabled
    val Lavender get() = palette.lavender

    val Gold get() = palette.gold
    val GoldContainer get() = palette.goldContainer
    val Mint get() = palette.mint
    val MintContainer get() = palette.mintContainer
    val Sky get() = palette.sky
    val SkyContainer get() = palette.skyContainer
    val SkyBorder get() = palette.skyBorder
    val Coral get() = palette.coral
    val CoralContainer get() = palette.coralContainer
    val CoralBorder get() = palette.coralBorder

    val OnBackground get() = palette.onBackground
    val OnBackgroundBody get() = palette.onBackgroundBody
    val OnBackgroundMuted get() = palette.onBackgroundMuted
    val OnBackgroundDisabled get() = palette.onBackgroundDisabled
    val Placeholder get() = palette.placeholder

    /** Verlauf des Kopfbereichs, von oben nach unten. */
    val headerBrush: Brush
        get() = Brush.verticalGradient(
            0f to HeaderTop,
            0.45f to HeaderMiddle,
            1f to HeaderBottom,
        )

    /** Schwacher Lavendel-Glanz links unten im Kopfbereich (Kreis bei 30 % / 88 %, läuft bei 60 % des Radius aus). */
    fun headerGlowBrush(size: Size): Brush {
        if (!palette.headerGlow) return Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        val center = Offset(size.width * 0.3f, size.height * 0.88f)
        // Radius bis zur am weitesten entfernten Ecke, wie bei CSS-Standard `circle`
        val radius = hypot(size.width * 0.7f, size.height * 0.88f)
        return Brush.radialGradient(
            0f to Lavender.copy(alpha = 0.09f),
            0.6f to Lavender.copy(alpha = 0f),
            center = center,
            radius = radius,
        )
    }
}
