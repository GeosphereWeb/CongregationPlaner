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
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Auswahl aus den Rednern der Versammlung mit Suche nach Name. */
@Composable
internal fun SpeakerPickerDialog(speakers: List<Speaker>, onSelect: (Speaker) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = remember(speakers, query) { speakers.matching(query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redner wählen") },
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
                    if (matches.isEmpty()) item { Text("Kein Redner gefunden.") }
                    items(matches, key = { it.memberId }) { speaker ->
                        Text(
                            speaker.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(speaker) }
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
internal fun List<Speaker>.matching(query: String): List<Speaker> {
    val text = query.trim().lowercase()
    return filter { speaker ->
        text.isEmpty() || speaker.lastName.lowercase().contains(text) || speaker.firstName.lowercase().contains(text)
    }
}

@ThemePreviews
@Composable
private fun SpeakerPickerDialogPreview() = PreviewThemeWrapper {
    SpeakerPickerDialog(
        speakers = listOf(Speaker("1", "Muster", "Max"), Speaker("2", "Beispiel", "Tim")),
        onSelect = {},
        onDismiss = {},
    )
}
