package de.geosphere.congregationplaner.talk.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.RecencyLevel
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.RecencyPalette
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.recencyPalette
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Die Farbe einer Stufe der Skala „wie lange her“ (CP-191). */
fun RecencyPalette.colorOf(level: RecencyLevel): Color = when (level) {
    RecencyLevel.RECENT -> recent
    RecencyLevel.ONE_TO_TWO_YEARS -> oneToTwo
    RecencyLevel.TWO_TO_FOUR_YEARS -> twoToFour
    RecencyLevel.FOUR_TO_SIX_YEARS -> fourToSix
    RecencyLevel.OVER_SIX_YEARS -> overSix
    RecencyLevel.NEVER -> never
}

/** Der heutige Tag, einmal je Anzeige gelesen. */
@Composable
internal fun rememberToday(): LocalDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

private const val CHIP_FILL_ALPHA = 0.18f
private const val DIMMED_ALPHA = 0.5f

/** Schmaler Farbstreifen links an einer Zeile; „nie gehalten“ nur als Umriss. */
@Composable
internal fun RecencyStripe(level: RecencyLevel, modifier: Modifier = Modifier) {
    val color = recencyPalette().colorOf(level)
    val shape = RoundedCornerShape(3.dp)
    val base = modifier.width(6.dp).fillMaxHeight()
    Box(
        if (level == RecencyLevel.NEVER) base.border(1.dp, color, shape) else base.background(color, shape),
    )
}

/** Chip mit Abstand und Anzahl, z. B. „vor 5 J. · 2×“, in der Farbe der Stufe. */
@Composable
internal fun RecencyChip(badge: RecencyBadge) {
    val color = recencyPalette().colorOf(badge.level)
    val shape = RoundedCornerShape(6.dp)
    Text(
        badge.label,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(color.copy(alpha = CHIP_FILL_ALPHA), shape)
            .border(BorderStroke(1.dp, color), shape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * Kachel der Heatmap mit der Vortragsnummer; inaktive Vorträge blass, die gewählte Kachel umrandet. [description]
 * nennt Vortrag und Abstand in Worten, damit die Stufe nicht nur in der Farbe steckt.
 */
@Composable
internal fun RecencyTile(
    number: Int,
    level: RecencyLevel,
    description: String,
    dimmed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = recencyPalette()
    val color = palette.colorOf(level)
    val shape = RoundedCornerShape(6.dp)
    val never = level == RecencyLevel.NEVER
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .alpha(if (dimmed) DIMMED_ALPHA else 1f)
            .semantics { contentDescription = description }
            .clip(shape)
            .background(if (never) Color.Transparent else color, shape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onBackground else color,
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            "$number",
            color = if (never) color else palette.onTile,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Legende der Skala: je Stufe ein Chip mit Text, damit die Kacheln ohne Farbsehen lesbar sind. */
@Composable
internal fun RecencyLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        recencyLegend().forEach { RecencyChip(it) }
    }
}

private val previewToday = LocalDate(2026, 11, 25)
private val previewDays = listOf(
    LocalDate(2026, 12, 6),
    LocalDate(2025, 3, 2),
    LocalDate(2023, 6, 4),
    LocalDate(2021, 11, 2),
    LocalDate(2018, 5, 6),
    null,
)

@ThemePreviews
@Composable
private fun RecencyDecorationsPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecencyLegend()
        previewDays.forEach { day ->
            val badge = recencyBadgeOf(day, if (day == null) 0 else 2, previewToday)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(32.dp),
            ) {
                RecencyStripe(badge.level)
                RecencyChip(badge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            previewDays.forEachIndexed { index, day ->
                Box(Modifier.width(44.dp)) {
                    val badge = recencyBadgeOf(day, 1, previewToday)
                    RecencyTile(
                        number = index + 1,
                        level = badge.level,
                        description = badge.tileDescription(index + 1),
                        dimmed = index == 2,
                        selected = index == 3,
                        onClick = {},
                    )
                }
            }
        }
    }
}
