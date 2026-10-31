package de.geosphere.congregationplaner.theming

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Farb-Tokens des Nachtblau-Designs (Board „Farbpalette“). Container sind die abgedunkelten Chip-Hintergründe. */
object NightColors {
    val Background = Color(0xFF0B1020)
    val Surface = Color(0xFF161D33)
    val SurfaceActive = Color(0xFF1A1430)
    val Border = Color(0xFF26304D)
    val BorderStrong = Color(0xFF2E3A5C)

    val HeaderTop = Color(0xFF0C0819)
    val HeaderMiddle = Color(0xFF160F26)
    val HeaderBottom = Color(0xFF2B1C48)

    val Primary = Color(0xFF7A55B5)
    val PrimaryDisabled = Color(0xFF2A2240)
    val Lavender = Color(0xFFB9A2E0)

    val Gold = Color(0xFFE6B866)
    val GoldContainer = Color(0xFF3A2E16)
    val Mint = Color(0xFF6FCDB6)
    val MintContainer = Color(0xFF12352F)
    val Sky = Color(0xFF86A6F2)
    val SkyContainer = Color(0xFF1B2850)
    val Coral = Color(0xFFF2786A)
    val CoralContainer = Color(0xFF45201E)

    val OnBackground = Color(0xFFE8ECF5)
    val OnBackgroundBody = Color(0xFFC3CADB)
    val OnBackgroundMuted = Color(0xFF8F9AB5)
    val Placeholder = Color(0xFF7C87A3)

    /** Verlauf des Kopfbereichs, von oben nach unten. */
    val headerBrush: Brush
        get() = Brush.verticalGradient(
            0f to HeaderTop,
            0.45f to HeaderMiddle,
            1f to HeaderBottom,
        )
}
