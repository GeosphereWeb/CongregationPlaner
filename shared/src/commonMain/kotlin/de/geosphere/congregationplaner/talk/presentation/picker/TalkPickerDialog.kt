package de.geosphere.congregationplaner.talk.presentation.picker

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
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Auswahl aus der Vortragsliste mit Suche nach Nummer oder Titel; nicht aktive Vorträge sind nicht wählbar. Steht der
 * Vortrag nicht in der Liste, trägt [onFreeEntry] den Suchtext als Titel ohne Nummer ein.
 */
@Composable
internal fun TalkPickerDialog(
    talks: List<Talk>,
    onSelect: (Talk) -> Unit,
    onDismiss: () -> Unit,
    onFreeEntry: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(talks, query) { talks.matching(query) }
    val freeTitle = freeEntryOf(query, TalkSlot.MAX_TALK_TITLE_LENGTH)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vortrag wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Nummer oder Titel suchen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (matches.isEmpty()) item { Text("Kein aktiver Vortrag gefunden.") }
                    items(matches, key = { it.number }) { talk ->
                        Text(
                            talk.label(),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(talk) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            freeTitle?.let { title -> TextButton(onClick = { onFreeEntry(title) }) { Text("„$title“ frei eintragen") } }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@ThemePreviews
@Composable
private fun TalkPickerDialogPreview() = PreviewThemeWrapper {
    TalkPickerDialog(
        talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Weiterer Vortrag"), Talk(3, "Pausiert", active = false)),
        onSelect = {},
        onDismiss = {},
    )
}
