package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Auswahl aus Mitgliedern der Versammlung (Redner oder Vorsitzende) mit Suche nach Name. */
@Composable
internal fun MemberPickerDialog(
    title: String,
    members: List<MemberRef>,
    onSelect: (MemberRef) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(members, query) { members.matching(query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Name suchen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (matches.isEmpty()) item { Text("Kein Mitglied gefunden.") }
                    items(matches, key = { it.memberId }) { member ->
                        Text(
                            member.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(member) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Die Eingabe passt auf Familien- oder Vornamen, ohne Rücksicht auf Groß- und Kleinschreibung. */
internal fun List<MemberRef>.matching(query: String): List<MemberRef> {
    val text = query.trim().lowercase()
    return filter { member ->
        text.isEmpty() || member.lastName.lowercase().contains(text) || member.firstName.lowercase().contains(text)
    }
}

@ThemePreviews
@Composable
private fun MemberPickerDialogPreview() = PreviewThemeWrapper {
    MemberPickerDialog(
        title = "Redner wählen",
        members = listOf(MemberRef("1", "Muster", "Max"), MemberRef("2", "Beispiel", "Tim")),
        onSelect = {},
        onDismiss = {},
    )
}
