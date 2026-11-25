package de.geosphere.congregationplaner.talk.presentation.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.RecencyLevel
import de.geosphere.congregationplaner.talk.presentation.common.RecencyBadge
import de.geosphere.congregationplaner.talk.presentation.common.RecencyChip
import de.geosphere.congregationplaner.talk.presentation.common.RecencyStripe
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

private const val BLOCKED_ALPHA = 0.5f

/**
 * Eine Zeile im Auswahldialog: Name bzw. Vortrag und darunter, wann er zuletzt eingeplant ist; gesperrte blass. Mit
 * [badge] zeigt die Zeile links den Farbstreifen und rechts den Chip „wie lange her“ (CP-191).
 */
@Composable
internal fun SuggestionRow(
    label: String,
    note: String,
    blocked: Boolean,
    badge: RecencyBadge? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .alpha(if (blocked) BLOCKED_ALPHA else 1f)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badge?.let { RecencyStripe(it.level) }
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
        badge?.let { RecencyChip(it) }
    }
}

/** Schalter, mit dem sich die vom Stichtag gesperrten Einträge einblenden lassen; [count] nennt, wie viele es sind. */
@Composable
internal fun ShowBlockedToggle(count: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Gesperrte anzeigen ($count)", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@ThemePreviews
@Composable
private fun SuggestionRowsPreview() = PreviewThemeWrapper {
    Column {
        ShowBlockedToggle(count = 2, checked = true, onCheckedChange = {})
        SuggestionRow("12 – Der Weg zum Glück", "noch nie eingeplant", blocked = false, onClick = {})
        SuggestionRow("31 – Glück im Alltag", "zuletzt eingeplant So, 02.11.2025", blocked = false, onClick = {})
        SuggestionRow(
            "44 – Hoffnung",
            "zuletzt eingeplant So, 10.03.2019",
            blocked = false,
            badge = RecencyBadge(RecencyLevel.OVER_SIX_YEARS, "vor 7 J. · 3×"),
            onClick = {},
        )
        SuggestionRow("45 – Hoffnung", "gesperrt – eingeplant So, 07.02.2027", blocked = true, onClick = {})
    }
}
