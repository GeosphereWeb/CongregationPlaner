package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import de.geosphere.congregationplaner.congregation.presentation.MeetingTimeInput
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
@Suppress("LongParameterList")
internal fun TalkSlotEditor(
    editor: TalkSlotEditorState,
    weekEvents: List<WeekEvent>,
    talks: List<Talk>,
    speakers: List<Speaker>,
    canViewMembers: Boolean,
    isSaving: Boolean,
    onChange: (TalkSlot) -> Unit,
    onSelectWeekEvent: (WeekEvent?) -> Unit,
    onSelectTalk: (Talk?) -> Unit,
    onSelectSpeaker: (Speaker?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val slot = editor.slot
    var showTalkPicker by remember { mutableStateOf(false) }
    var showSpeakerPicker by remember { mutableStateOf(false) }
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (editor.isNew) "Neuer Termin" else "Termin ändern", style = MaterialTheme.typography.titleMedium)
            TalkDateInput(slot.date, onDateChange = { onChange(slot.copy(date = it)) })
            MeetingTimeInput("Uhrzeit", slot.time, onTimeChange = { onChange(slot.copy(time = it)) })

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Wochenereignis", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = slot.weekEventId == null,
                        onClick = { onSelectWeekEvent(null) },
                        label = { Text("Kein Ereignis") },
                    )
                    weekEvents.forEach { event ->
                        FilterChip(
                            selected = slot.weekEventId == event.id,
                            onClick = { onSelectWeekEvent(event) },
                            label = { Text(event.name) },
                        )
                    }
                    // Ein inzwischen entferntes Ereignis bleibt am Termin sichtbar.
                    if (slot.weekEventId != null && weekEvents.none { it.id == slot.weekEventId }) {
                        FilterChip(selected = true, onClick = {}, label = { Text(slot.weekEventName.orEmpty()) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vortrag entfällt", modifier = Modifier.weight(1f))
                Switch(checked = slot.talkCancelled, onCheckedChange = { onChange(slot.copy(talkCancelled = it)) })
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Vortrag", style = MaterialTheme.typography.labelLarge)
                Text(
                    slot.talkNumber?.let { "$it – ${slot.talkTitle.orEmpty()}" } ?: "Noch kein Vortrag gewählt",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTalkPicker = true }, enabled = !slot.talkCancelled) {
                        Text(if (slot.talkNumber == null) "Vortrag wählen" else "Vortrag ändern")
                    }
                    if (slot.talkNumber != null) {
                        TextButton(onClick = { onSelectTalk(null) }) { Text("Entfernen") }
                    }
                }
                if (slot.talkCancelled) {
                    Text(
                        "Der Vortrag entfällt, eine Auswahl ist nicht nötig.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SpeakerSection(
                slot = slot,
                hasSpeakers = speakers.isNotEmpty(),
                canViewMembers = canViewMembers,
                onPick = { showSpeakerPicker = true },
                onRemove = { onSelectSpeaker(null) },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isSaving && editor.isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isSaving) { Text("Abbrechen") }
            }
        }
    }

    if (showSpeakerPicker) {
        SpeakerPickerDialog(
            speakers = speakers,
            onSelect = {
                onSelectSpeaker(it)
                showSpeakerPicker = false
            },
            onDismiss = { showSpeakerPicker = false },
        )
    }

    if (showTalkPicker) {
        TalkPickerDialog(
            talks = talks,
            onSelect = {
                onSelectTalk(it)
                showTalkPicker = false
            },
            onDismiss = { showTalkPicker = false },
        )
    }
}

@Composable
private fun SpeakerSection(
    slot: TalkSlot,
    hasSpeakers: Boolean,
    canViewMembers: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Redner", style = MaterialTheme.typography.labelLarge)
        Text(slot.speaker?.displayName ?: "Noch kein Redner gewählt", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick, enabled = canViewMembers && hasSpeakers && !slot.talkCancelled) {
                Text(if (slot.speaker == null) "Redner wählen" else "Redner ändern")
            }
            if (slot.speaker != null) TextButton(onClick = onRemove) { Text("Entfernen") }
        }
        val hint = when {
            !canViewMembers ->
                "Dir fehlt die Berechtigung „Mitglieder einsehen“, deshalb können keine Redner geladen werden."
            !hasSpeakers -> "Noch kein Mitglied ist als Redner markiert (Userverwaltung)."
            else -> null
        }
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@ThemePreviews
@Composable
private fun TalkSlotEditorPreview() = PreviewThemeWrapper {
    TalkSlotEditor(
        editor = TalkSlotEditorState(TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30))),
        weekEvents = listOf(
            WeekEvent("kongress", "Kongress", cancelsTalk = true),
            WeekEvent("besuch", "Kreisaufseher"),
        ),
        talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Weiterer Vortrag")),
        speakers = listOf(Speaker("1", "Muster", "Max")),
        canViewMembers = true,
        isSaving = false,
        onChange = {},
        onSelectWeekEvent = {},
        onSelectTalk = {},
        onSelectSpeaker = {},
        onSave = {},
        onCancel = {},
    )
}
