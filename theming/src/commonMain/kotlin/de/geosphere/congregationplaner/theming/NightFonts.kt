package de.geosphere.congregationplaner.theming

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
