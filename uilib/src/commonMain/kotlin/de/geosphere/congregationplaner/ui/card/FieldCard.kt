@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton

private val CARD_RADIUS = 14.dp

/** Gestrichelter Rahmen mit abgerundeten Ecken, z. B. für eine Angabe, die noch fehlt. */
fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    drawRoundRect(color, cornerRadius = CornerRadius(cornerRadius.toPx()), style = stroke)
}

/**
 * Feldkarte im Detailbereich: links Beschriftung, Wert und Zusatz, rechts eine Aktion. Mit [missing] zeigt sie
 * einen gestrichelten Koralle-Rand und den Wert gedämpft, weil die Angabe noch fehlt.
 */
@Composable
fun FieldCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    missing: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(CARD_RADIUS)
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(NightColors.Surface)
            .then(
                if (missing) {
                    Modifier.dashedBorder(NightColors.Coral, CARD_RADIUS)
                } else {
                    Modifier.border(1.dp, NightColors.Border, shape)
                },
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = NightColors.OnBackgroundMuted)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (missing) NightColors.OnBackgroundMuted else NightColors.OnBackground,
            )
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = NightColors.OnBackgroundMuted)
            }
        }
        action?.invoke()
    }
}

/** Kompakter Datumsblock: Wochentag kurz über dem Tag, 48 dp breit. */
@Composable
fun DateBadge(weekday: String, day: String, modifier: Modifier = Modifier) {
    Column(modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(weekday, style = MaterialTheme.typography.bodySmall, color = NightColors.OnBackgroundMuted)
        Text(day, style = MaterialTheme.typography.headlineSmall, color = NightColors.OnBackground)
    }
}

@ThemePreviews
@Composable
private fun FieldCardPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldCard(
            label = "Vortrag",
            value = "Nr. 12 – Beispielvortrag",
            action = { SecondaryButton("Ändern", {}, outlined = false) },
        )
        FieldCard(
            label = "Redner",
            value = "Noch kein Redner",
            supporting = "Aus der eigenen Versammlung oder aus „Externe Redner“",
            missing = true,
            action = { PrimaryButton("Redner wählen", {}, height = 44.dp) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateBadge("So", "1")
            DateBadge("So", "8")
        }
    }
}
