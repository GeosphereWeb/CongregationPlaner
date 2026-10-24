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
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
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
    chairmen: List<Chairman>,
    externalSpeakers: List<ExternalSpeaker>,
    canViewMembers: Boolean,
    isSaving: Boolean,
    onChange: (TalkSlot) -> Unit,
    onSelectWeekEvent: (WeekEvent?) -> Unit,
    onSelectTalk: (Talk?) -> Unit,
    speakerActions: SpeakerActions,
    onSelectChairman: (Chairman?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val slot = editor.slot
    var showTalkPicker by remember { mutableStateOf(false) }
    var showSpeakerPicker by remember { mutableStateOf(false) }
    var showChairmanPicker by remember { mutableStateOf(false) }
    // Wer schon Vorsitzender ist, kann nicht Redner sein, und umgekehrt.
    val selectableSpeakers = speakers.filterNot { it.memberId == slot.chairmanMemberId }
    val selectableChairmen = chairmen.filterNot { it.memberId == slot.speakerMemberId }
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

            MemberSection(
                label = "Redner",
                actionLabel = "Redner",
                selectedText = slot.externalSpeaker?.displayName ?: slot.speaker?.displayName,
                pickEnabled = !slot.talkCancelled,
                hint = when {
                    !canViewMembers ->
                        "Ohne die Berechtigung „Mitglieder einsehen“ stehen nur externe Redner zur Auswahl."
                    speakers.isEmpty() -> "Noch kein Mitglied ist als Redner markiert (Userverwaltung)."
                    else -> null
                },
                onPick = { showSpeakerPicker = true },
                onRemove = { speakerActions.onSelectMember(null) },
            )

            MemberSection(
                label = "Vorsitzender",
                actionLabel = "Vorsitzenden",
                selectedText = slot.chairman?.displayName,
                pickEnabled = canViewMembers && selectableChairmen.isNotEmpty(),
                hint = when {
                    !canViewMembers ->
                        "Dir fehlt die Berechtigung „Mitglieder einsehen“, " +
                            "deshalb können keine Vorsitzenden geladen werden."
                    chairmen.isEmpty() ->
                        "Kein Bruder mit Dienstamt (Ältester oder Dienstamtgehilfe) mit Namen gefunden."
                    selectableChairmen.isEmpty() -> "Alle Brüder mit Dienstamt sind schon als Redner eingeteilt."
                    else -> null
                },
                onPick = { showChairmanPicker = true },
                onRemove = { onSelectChairman(null) },
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
            members = selectableSpeakers,
            externalSpeakers = externalSpeakers,
            canViewMembers = canViewMembers,
            actions = speakerActions.closingPicker { showSpeakerPicker = false },
            onDismiss = { showSpeakerPicker = false },
        )
    }

    if (showChairmanPicker) {
        MemberPickerDialog(
            title = "Vorsitzenden wählen",
            members = selectableChairmen,
            onSelect = {
                onSelectChairman(it)
                showChairmanPicker = false
            },
            onDismiss = { showChairmanPicker = false },
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

/** Redner oder Vorsitzender: Anzeige der Auswahl, Auswählen, Entfernen und ein Hinweis bei fehlenden Daten. */
@Composable
@Suppress("LongParameterList")
private fun MemberSection(
    label: String,
    actionLabel: String,
    selectedText: String?,
    pickEnabled: Boolean,
    hint: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(selectedText ?: "Noch kein $label gewählt", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick, enabled = pickEnabled) {
                Text(if (selectedText == null) "$actionLabel wählen" else "$actionLabel ändern")
            }
            if (selectedText != null) TextButton(onClick = onRemove) { Text("Entfernen") }
        }
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

// Nach der Auswahl eines Redners schließt sich der Dialog.
private fun SpeakerActions.closingPicker(close: () -> Unit): SpeakerActions = copy(
    onSelectMember = {
        onSelectMember(it)
        close()
    },
    onSelectExternal = {
        onSelectExternal(it)
        close()
    },
)

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
        chairmen = listOf(Chairman("2", "Beispiel", "Tim")),
        externalSpeakers = listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord")),
        canViewMembers = true,
        isSaving = false,
        onChange = {},
        onSelectWeekEvent = {},
        onSelectTalk = {},
        speakerActions = SpeakerActions(),
        onSelectChairman = {},
        onSave = {},
        onCancel = {},
    )
}
