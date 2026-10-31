@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

private val ButtonShape = RoundedCornerShape(14.dp)

/** Hauptaktion in Lila. Die Breite bestimmt der Aufrufer (z. B. `fillMaxWidth()`). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = NightColors.Primary,
            contentColor = NightColors.OnBackground,
            disabledContainerColor = NightColors.PrimaryDisabled,
            disabledContentColor = NightColors.OnBackgroundMuted,
        ),
    ) {
        Text(text, style = nightTextStyle(16.sp, FontWeight.Bold, Color.Unspecified))
    }
}

/**
 * Nebenaktion: mit Fläche und Rand ([outlined]) oder als Textaktion ohne Hintergrund.
 * [leading] ist für ein Icon vor dem Text gedacht.
 */
@Composable
@Suppress("LongParameterList")
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = true,
    contentColor: Color = if (outlined) NightColors.OnBackground else NightColors.Lavender,
    height: Dp = if (outlined) 52.dp else 44.dp,
    leading: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = ButtonShape,
        border = if (outlined) BorderStroke(1.dp, NightColors.BorderStrong) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (outlined) NightColors.Surface else Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = if (outlined) NightColors.Surface else Color.Transparent,
            disabledContentColor = NightColors.OnBackgroundMuted,
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(text, style = nightTextStyle(15.sp, FontWeight.Medium, Color.Unspecified))
        }
    }
}

@ThemePreviews
@Composable
private fun ButtonsPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton("Anmelden", {}, Modifier.fillMaxWidth())
        PrimaryButton("Link übernehmen", {}, Modifier.fillMaxWidth(), enabled = false, height = 48.dp)
        SecondaryButton("Mit Google fortfahren", {}, Modifier.fillMaxWidth())
        SecondaryButton("Abmelden", {}, Modifier.fillMaxWidth(), outlined = false)
    }
}
