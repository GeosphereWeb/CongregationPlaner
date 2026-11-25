package de.geosphere.congregationplaner.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Colors of the "how long ago" scale (CP-191): red for recent talks through blue for talks not held for over six
 * years; [never] is the neutral color of talks never held, [onTile] the text on a colored tile.
 */
data class RecencyPalette(
    val recent: Color,
    val oneToTwo: Color,
    val twoToFour: Color,
    val fourToSix: Color,
    val overSix: Color,
    val never: Color,
    val onTile: Color,
)

val recencyDarkPalette = RecencyPalette(
    recent = Color(0xFFF2786A),
    oneToTwo = Color(0xFFE6A45A),
    twoToFour = Color(0xFFD9CF6A),
    fourToSix = Color(0xFF6FCDB6),
    overSix = Color(0xFF4F8FF0),
    never = Color(0xFF8F9AB5),
    onTile = Color(0xFF0B1020),
)

// Dark enough for white tile text and for colored text on a light background (WCAG AA, 4.5:1).
val recencyLightPalette = RecencyPalette(
    recent = Color(0xFFB71C1C),
    oneToTwo = Color(0xFFBF360C),
    twoToFour = Color(0xFF6D5E00),
    fourToSix = Color(0xFF00695C),
    overSix = Color(0xFF0D47A1),
    never = Color(0xFF5F6B85),
    onTile = Color(0xFFFFFFFF),
)

private const val DARK_LUMINANCE = 0.5f

/** The palette matching the current background: the dark one on dark designs, otherwise the light one. */
@Composable
fun recencyPalette(): RecencyPalette =
    if (MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE) recencyDarkPalette else recencyLightPalette
