package de.geosphere.congregationplaner.theming

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import congregationplaner.theming.generated.resources.Res
import congregationplaner.theming.generated.resources.jetbrains_mono_medium
import congregationplaner.theming.generated.resources.space_grotesk_bold
import congregationplaner.theming.generated.resources.space_grotesk_medium
import congregationplaner.theming.generated.resources.space_grotesk_regular
import org.jetbrains.compose.resources.Font

/** Fließtext und Überschriften (400/500/700). */
@Composable
fun spaceGroteskFamily(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk_regular, FontWeight.Normal),
    Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    Font(Res.font.space_grotesk_bold, FontWeight.Bold),
)

/** Labels, Schrittanzeige und Eingaben mit technischem Inhalt (500). */
@Composable
fun jetBrainsMonoFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Textstil im Nachtblau-Design; [Color.Unspecified] übernimmt die Inhaltsfarbe des Elternteils (z. B. in Buttons). */
@Composable
fun nightTextStyle(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = NightColors.OnBackground,
    monospace: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = if (monospace) jetBrainsMonoFamily() else spaceGroteskFamily(),
    fontSize = size,
    // JetBrains Mono ist nur in Medium eingebunden
    fontWeight = if (monospace) FontWeight.Medium else weight,
    color = color,
    letterSpacing = letterSpacing,
    lineHeight = lineHeight,
)
