package de.geosphere.congregationplaner.talk.presentation.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

private const val BLOCKED_ALPHA = 0.5f

/** Eine Zeile im Auswahldialog: Name bzw. Vortrag und darunter, wann er zuletzt eingeplant ist; gesperrte blass. */
@Composable
internal fun SuggestionRow(label: String, note: String, blocked: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (blocked) BLOCKED_ALPHA else 1f)
            .padding(vertical = 8.dp),
    ) {
        Text(label)
        Text(note, style = MaterialTheme.typography.bodySmall)
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
        SuggestionRow("45 – Hoffnung", "gesperrt – eingeplant So, 07.02.2027", blocked = true, onClick = {})
    }
}
