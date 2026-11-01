@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.nav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.chip.StatusChip
import de.geosphere.congregationplaner.ui.chip.StatusTone

private val CARD_SHAPE = RoundedCornerShape(14.dp)
private val NESTED_CARD_SHAPE = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)
private val ITEM_SHAPE = RoundedCornerShape(10.dp)
private val EDGE_WIDTH = 2.dp

/** Gruppen-Überschrift einer Liste; [tone] färbt sie, ohne Angabe ist sie gedämpft. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, tone: StatusTone? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = tone?.content ?: NightColors.OnBackgroundMuted,
        modifier = modifier,
    )
}

/**
 * Karte der Übersicht auf dem Handy. [nested] rückt sie ein und setzt links eine Kante in [accent]; [badge] ist eine
 * kurze Marke wie „1 offen“; [supportingTone] färbt den Untertext; [indicator] setzt einen Statuspunkt vor den Titel.
 */
@Composable
@Suppress("LongParameterList")
fun NavCardRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    supportingTone: StatusTone? = null,
    selected: Boolean = false,
    nested: Boolean = false,
    accent: Color = NightColors.Border,
    badge: String? = null,
    badgeTone: StatusTone = StatusTone.Gold,
    indicator: StatusTone? = null,
) {
    val shape = if (nested) NESTED_CARD_SHAPE else CARD_SHAPE
    Row(
        modifier = modifier.padding(start = if (nested) 14.dp else 0.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) NightColors.SurfaceActive else NightColors.Surface)
            .border(1.dp, if (selected) NightColors.Lavender else NightColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind { if (nested) drawRect(accent, size = Size(EDGE_WIDTH.toPx(), size.height)) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indicator != null) Box(Modifier.size(8.dp).background(indicator.content, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NightColors.OnBackground)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingTone?.content ?: NightColors.OnBackgroundMuted,
                )
            }
        }
        if (badge != null) StatusChip(badge, badgeTone)
        Chevron()
    }
}

/** Eintrag der festen zweiten Spalte auf Tablet und Desktop; [nested] rückt ihn mit einer feinen Linie ein. */
@Composable
fun NavColumnItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    nested: Boolean = false,
) {
    Row(
        modifier = modifier.padding(start = if (nested) 12.dp else 0.dp)
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(ITEM_SHAPE)
            .background(if (selected) NightColors.HeaderBottom else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (nested) drawRect(NightColors.Border, size = Size(EDGE_WIDTH.toPx(), size.height))
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NightColors.OnBackground else NightColors.OnBackgroundBody,
            modifier = Modifier.padding(start = if (nested) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun Chevron() {
    Canvas(Modifier.size(16.dp)) {
        val color = NightColors.OnBackgroundMuted
        val width = 2.dp.toPx()
        val top = Offset(size.width * 0.38f, size.height * 0.25f)
        val tip = Offset(size.width * 0.68f, size.height / 2)
        val bottom = Offset(size.width * 0.38f, size.height * 0.75f)
        drawLine(color, top, tip, width, StrokeCap.Round)
        drawLine(color, tip, bottom, width, StrokeCap.Round)
    }
}

@ThemePreviews
@Composable
private fun NavCardRowPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Verwaltung", tone = StatusTone.Gold)
        NavCardRow("Allgemeine Versammlungsdaten", {}, supportingText = "Name, Zusammenkunftszeiten")
        NavCardRow(
            "Userverwaltung",
            {},
            supportingText = "Mitglieder, Rollen, Freigaben",
            nested = true,
            accent = NightColors.Gold,
            badge = "1 offen",
        )
        SectionLabel("Planung")
        NavCardRow("Planung unter der Woche", {}, supportingText = "Aufgabenplaner", supportingTone = StatusTone.Sky)
        NavCardRow(
            "Vortragsplanung",
            {},
            supportingText = "Vortragsplaner · Wochenende, Redner, Historie",
            supportingTone = StatusTone.Mint,
            selected = true,
        )
    }
}

@ThemePreviews
@Composable
private fun NavColumnItemPreview() = NightPreviewWrapper {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        NavColumnItem("Planung unter der Woche", {})
        NavColumnItem("Planung am Wochenende", {}, selected = true, nested = true)
        NavColumnItem("Externe Redner", {}, nested = true)
    }
}
