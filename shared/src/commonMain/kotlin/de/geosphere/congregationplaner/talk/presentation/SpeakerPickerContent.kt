package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Callbacks der Redner-Auswahl; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class SpeakerActions(
    val onSelectMember: (MemberRef?) -> Unit = {},
    val onSelectExternal: (ExternalSpeaker?) -> Unit = {},
    val onAddExternal: (lastName: String, firstName: String, congregationName: String) -> Unit = { _, _, _ -> },
    val onDeleteExternal: (ExternalSpeaker) -> Unit = {},
)

/**
 * Auswahl des Redners: eigene Redner (nur mit der Berechtigung, die Mitglieder zu lesen) und externe Redner,
 * die hier auch angelegt und gelöscht werden können.
 */
@Composable
internal fun SpeakerPickerDialog(
    members: List<MemberRef>,
    externalSpeakers: List<ExternalSpeaker>,
    canViewMembers: Boolean,
    actions: SpeakerActions,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val matchingMembers = remember(members, query) { members.matching(query) }
    val matchingExternal = remember(externalSpeakers, query) { externalSpeakers.matchingExternal(query) }
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
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (canViewMembers) {
                        item { SectionTitle("Eigene Redner") }
                        if (matchingMembers.isEmpty()) item { Text("Kein Mitglied gefunden.") }
                        items(matchingMembers, key = { "member-${it.memberId}" }) { member ->
                            Text(
                                member.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actions.onSelectMember(member) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                    item { SectionTitle("Externe Redner") }
                    if (matchingExternal.isEmpty()) item { Text("Kein externer Redner gefunden.") }
                    items(matchingExternal, key = { "external-${it.id}" }) { speaker ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                speaker.displayName,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { actions.onSelectExternal(speaker) }
                                    .padding(vertical = 12.dp),
                            )
                            TextButton(onClick = { actions.onDeleteExternal(speaker) }) {
                                Text("Löschen", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showAddDialog = true }) { Text("Externen Redner anlegen") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )

    if (showAddDialog) {
        AddExternalSpeakerDialog(
            onAdd = { lastName, firstName, congregationName ->
                actions.onAddExternal(lastName, firstName, congregationName)
                showAddDialog = false
                onDismiss()
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
}

/** Legt einen Redner aus einer anderen Versammlung an; Familienname und Versammlung sind Pflicht. */
@Composable
private fun AddExternalSpeakerDialog(onAdd: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var congregationName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Externen Redner anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Familienname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Vorname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = congregationName,
                    onValueChange = { congregationName = it },
                    label = { Text("Versammlung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(lastName, firstName, congregationName) },
                enabled = lastName.isNotBlank() && congregationName.isNotBlank(),
            ) {
                Text("Anlegen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Die Eingabe passt auf Familien-, Vornamen oder die Versammlung, ohne Rücksicht auf Groß- und Kleinschreibung. */
internal fun List<ExternalSpeaker>.matchingExternal(query: String): List<ExternalSpeaker> {
    val text = query.trim().lowercase()
    return filter { speaker ->
        text.isEmpty() ||
            speaker.lastName.lowercase().contains(text) ||
            speaker.firstName.lowercase().contains(text) ||
            speaker.congregationName.lowercase().contains(text)
    }
}

@ThemePreviews
@Composable
private fun SpeakerPickerDialogPreview() = PreviewThemeWrapper {
    SpeakerPickerDialog(
        members = listOf(MemberRef("1", "Muster", "Max")),
        externalSpeakers = listOf(ExternalSpeaker("e1", "Beispiel", "Tim", "Nord")),
        canViewMembers = true,
        actions = SpeakerActions(),
        onDismiss = {},
    )
}
