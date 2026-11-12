package de.geosphere.congregationplaner.talk.presentation.picker

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
import de.geosphere.congregationplaner.talk.domain.model.PlanningUsage
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/** Callbacks der Redner-Auswahl; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class SpeakerActions(
    val onSelectMember: (MemberRef?) -> Unit = {},
    val onSelectExternal: (ExternalSpeaker?) -> Unit = {},
    /** Trägt einen Redner, der in keiner Liste steht, mit dem Suchtext als Namen ein. */
    val onFreeSpeaker: (String) -> Unit = {},
)

/**
 * Auswahl des Redners: eigene Redner (nur mit der Berechtigung, die Mitglieder zu lesen) und externe Redner aus dem
 * Verzeichnis, das in der eigenen Verwaltung gepflegt wird. Vorgeschlagen werden zuerst die Redner, die am längsten
 * nicht eingeplant sind; Redner, die der Stichtag aus [usage] sperrt, lassen sich einblenden. Die externen Redner sind
 * schon auf den gewählten Vortrag eingeschränkt (siehe [offering]).
 */
@Composable
internal fun SpeakerPickerDialog(
    members: List<MemberRef>,
    externalSpeakers: List<ExternalSpeaker>,
    canViewMembers: Boolean,
    actions: SpeakerActions,
    onDismiss: () -> Unit,
    usage: PlanningUsage = PlanningUsage(),
) {
    var query by remember { mutableStateOf("") }
    var showBlocked by remember { mutableStateOf(false) }
    val matchingMembers = remember(members, query) { members.matching(query) }
    val matchingExternal = remember(externalSpeakers, query) { externalSpeakers.matchingExternal(query) }
    val memberSuggestions = remember(matchingMembers, usage, showBlocked) {
        matchingMembers.suggestedMembers(usage, showBlocked)
    }
    val externalSuggestions = remember(matchingExternal, usage, showBlocked) {
        matchingExternal.suggestedExternal(usage, showBlocked)
    }
    val blockedCount = remember(matchingMembers, matchingExternal, usage) {
        matchingMembers.count { usage.isMemberBlocked(it.memberId) } +
            matchingExternal.count { usage.isExternalBlocked(it.id) }
    }
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
                if (blockedCount > 0) ShowBlockedToggle(blockedCount, showBlocked) { showBlocked = it }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (canViewMembers) {
                        item { SectionTitle("Eigene Redner") }
                        if (memberSuggestions.isEmpty()) item { Text("Kein freies Mitglied gefunden.") }
                        items(memberSuggestions, key = { "member-${it.item.memberId}" }) { suggestion ->
                            SuggestionRow(suggestion.item.displayName, suggestion.note, suggestion.blocked) {
                                actions.onSelectMember(suggestion.item)
                            }
                        }
                    }
                    item { SectionTitle("Externe Redner") }
                    if (externalSuggestions.isEmpty()) item { Text("Kein freier externer Redner gefunden.") }
                    items(externalSuggestions, key = { "external-${it.item.id}" }) { suggestion ->
                        SuggestionRow(suggestion.item.displayName, suggestion.note, suggestion.blocked) {
                            actions.onSelectExternal(suggestion.item)
                        }
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
        members = listOf(MemberRef("1", "Muster", "Max"), MemberRef("2", "Beispiel", "Ben")),
        externalSpeakers = listOf(ExternalSpeaker("e1", "Beispiel", "Tim", "Nord")),
        canViewMembers = true,
        actions = SpeakerActions(),
        onDismiss = {},
        usage = PlanningUsage(
            settings = TalkPlanningSettings(speakerFreeSince = LocalDate(2025, 6, 1)),
            memberLastPlanned = mapOf("1" to LocalDate(2026, 2, 1), "2" to LocalDate(2024, 10, 6)),
        ),
    )
}
