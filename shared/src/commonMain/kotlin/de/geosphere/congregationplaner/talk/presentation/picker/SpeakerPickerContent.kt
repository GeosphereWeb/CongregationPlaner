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
import androidx.compose.material3.MaterialTheme
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
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Callbacks der Redner-Auswahl; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class SpeakerActions(
    val onSelectMember: (MemberRef?) -> Unit = {},
    val onSelectExternal: (ExternalSpeaker?) -> Unit = {},
    /** Trägt einen Redner, der in keiner Liste steht, mit dem Suchtext als Namen ein. */
    val onFreeSpeaker: (String) -> Unit = {},
)

/**
 * Auswahl des Redners: eigene Redner (nur mit der Berechtigung, die Mitglieder zu lesen) und externe Redner aus dem
 * Verzeichnis, das in der eigenen Verwaltung gepflegt wird.
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
    val matchingMembers = remember(members, query) { members.matching(query) }
    val matchingExternal = remember(externalSpeakers, query) { externalSpeakers.matchingExternal(query) }
    val freeName = freeEntryOf(query, TalkSlot.MAX_PERSON_NAME_LENGTH)
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
                        Text(
                            speaker.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onSelectExternal(speaker) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            freeName?.let { name ->
                TextButton(onClick = { actions.onFreeSpeaker(name) }) { Text("„$name“ frei eintragen") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
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
