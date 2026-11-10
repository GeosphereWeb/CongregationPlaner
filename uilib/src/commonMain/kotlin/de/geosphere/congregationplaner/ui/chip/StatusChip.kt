@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

/** Bedeutung eines Chips: Gold = Highlight/Wartet, Mint = Erfolg, Himmelblau = Info, Koralle = Fehler. */
enum class StatusTone(val content: Color, val container: Color) {
    Gold(NightColors.Gold, NightColors.GoldContainer),
    Mint(NightColors.Mint, NightColors.MintContainer),
    Sky(NightColors.Sky, NightColors.SkyContainer),
    Coral(NightColors.Coral, NightColors.CoralContainer),
}

/** Kleine Statusmarke mit Punkt. */
@Composable
fun StatusChip(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(tone.container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(tone.content, CircleShape))
        Text(text, style = nightTextStyle(13.sp, FontWeight.Medium, tone.content))
    }
}

/**
 * Schmale Statusmarke mit senkrechtem Text (von unten nach oben gelesen), gedacht für den rechten Rand eines
 * Listeneintrags. Die Höhe gibt der Aufrufer vor, z. B. mit `fillMaxHeight()`.
 */
@Composable
fun VerticalStatusTag(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(tone.container, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        RotatedUp {
            Text(
                text,
                style = nightTextStyle(11.sp, FontWeight.Medium, tone.content),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

/**
 * Dreht den Inhalt um 90° gegen den Uhrzeigersinn; Breite und Höhe des Layouts werden getauscht, auch in den
 * Intrinsic-Abfragen, damit ein Elternteil mit `IntrinsicSize.Min` die richtige Höhe bekommt.
 */
@Composable
private fun RotatedUp(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier, measurePolicy = RotatedUpMeasurePolicy)
}

private object RotatedUpMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeable = measurables.single().measure(Constraints())
        return layout(placeable.height, placeable.width) {
            placeable.placeWithLayer(
                x = -(placeable.width - placeable.height) / 2,
                y = (placeable.width - placeable.height) / 2,
            ) { rotationZ = -90f }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.single().minIntrinsicHeight(Constraints.Infinity)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.single().maxIntrinsicHeight(Constraints.Infinity)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.single().maxIntrinsicWidth(Constraints.Infinity)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.single().maxIntrinsicWidth(Constraints.Infinity)
}

@ThemePreviews
@Composable
private fun StatusChipPreview() = NightPreviewWrapper {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip("Freigabe ausstehend", StatusTone.Gold)
        StatusChip("Geprobt", StatusTone.Mint)
        StatusChip("Entwurf", StatusTone.Sky)
        StatusChip("Fehler", StatusTone.Coral)
    }
}

@ThemePreviews
@Composable
private fun VerticalStatusTagPreview() = NightPreviewWrapper {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(72.dp)) {
        VerticalStatusTag("Informiert", StatusTone.Mint, Modifier.fillMaxHeight())
        VerticalStatusTag("Nächster", StatusTone.Gold, Modifier.fillMaxHeight())
        VerticalStatusTag("Entfällt", StatusTone.Sky, Modifier.fillMaxHeight())
    }
}
