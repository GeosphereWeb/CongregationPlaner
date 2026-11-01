package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import de.geosphere.congregationplaner.presentation.navigation.germanWeekdayName
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.card.FieldCard
import de.geosphere.congregationplaner.ui.chip.StatusChip
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val COMPACT_WIDTH = 600.dp
private val ACTION_HEIGHT = 44.dp
private val BUTTON_HEIGHT = 48.dp

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun TalkSlotEditor(
    editor: TalkSlotEditorState,
    chip: SlotChip,
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
    onOpenHistory: (Int) -> Unit,
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
    val speakerText = slot.externalSpeaker?.displayName ?: slot.speaker?.displayName
    val complete = slot.talkNumber != null && speakerText != null

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlotHeader(slot, chip)
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
        SwitchRow("Vortrag entfällt", slot.talkCancelled) { onChange(slot.copy(talkCancelled = it)) }

        FieldCard(
            label = "Vortrag",
            value = slot.talkNumber?.let { "Nr. $it – ${slot.talkTitle.orEmpty()}" } ?: "Noch kein Vortrag gewählt",
            supporting = if (slot.talkCancelled) "Der Vortrag entfällt, eine Auswahl ist nicht nötig." else null,
            missing = slot.talkNumber == null && !slot.talkCancelled,
            action = {
                FieldActions(
                    changeLabel = if (slot.talkNumber == null) "Wählen" else "Ändern",
                    enabled = !slot.talkCancelled,
                    onChange = { showTalkPicker = true },
                    onRemove = if (slot.talkNumber != null) ({ onSelectTalk(null) }) else null,
                )
            },
        )
        slot.talkNumber?.let { number ->
            // Ungespeicherte Änderungen gingen beim Wechsel verloren.
            SecondaryButton(
                text = "Historie zu Nr. $number ansehen",
                onClick = { onOpenHistory(number) },
                enabled = !editor.isDirty && !isSaving,
                outlined = false,
            )
        }

        val speakerMissing = speakerText == null && !slot.talkCancelled
        FieldCard(
            label = "Redner",
            value = speakerText ?: "Noch kein Redner",
            supporting = when {
                !canViewMembers ->
                    "Ohne die Berechtigung „Mitglieder einsehen“ stehen nur externe Redner zur Auswahl."
                speakers.isEmpty() && externalSpeakers.isEmpty() ->
                    "Noch kein Mitglied ist als Redner markiert (Userverwaltung)."
                speakerMissing -> "Aus der eigenen Versammlung oder aus „Externe Redner“"
                else -> null
            },
            missing = speakerMissing,
            action = {
                if (speakerMissing) {
                    PrimaryButton("Redner wählen", { showSpeakerPicker = true }, height = ACTION_HEIGHT)
                } else {
                    FieldActions(
                        changeLabel = if (speakerText == null) "Wählen" else "Ändern",
                        enabled = !slot.talkCancelled,
                        onChange = { showSpeakerPicker = true },
                        onRemove = if (speakerText != null) ({ speakerActions.onSelectMember(null) }) else null,
                    )
                }
            },
        )

        FieldCard(
            label = "Vorsitz",
            value = slot.chairman?.displayName ?: "Noch kein Vorsitzender",
            supporting = when {
                !canViewMembers ->
                    "Dir fehlt die Berechtigung „Mitglieder einsehen“, " +
                        "deshalb können keine Vorsitzenden geladen werden."
                chairmen.isEmpty() ->
                    "Kein Bruder mit Dienstamt (Ältester oder Dienstamtgehilfe) mit Namen gefunden."
                selectableChairmen.isEmpty() -> "Alle Brüder mit Dienstamt sind schon als Redner eingeteilt."
                else -> null
            },
            action = {
                FieldActions(
                    changeLabel = if (slot.chairman == null) "Wählen" else "Ändern",
                    enabled = canViewMembers && selectableChairmen.isNotEmpty(),
                    onChange = { showChairmanPicker = true },
                    onRemove = if (slot.chairman != null) ({ onSelectChairman(null) }) else null,
                )
            },
        )

        SwitchRow(
            label = "Bestätigt",
            checked = slot.confirmed,
            enabled = complete && !slot.talkCancelled,
        ) { onChange(slot.copy(confirmed = it)) }

        EditorActions(
            saveText = if (isSaving) "Speichert …" else "Speichern",
            saveEnabled = !isSaving && editor.isDirty,
            discardEnabled = !isSaving,
            onSave = onSave,
            onDiscard = onCancel,
        )
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

/** Wochentag, Datum und Statusmarke; ein Wochenereignis steht daneben als eigene Marke. */
@Composable
private fun SlotHeader(slot: TalkSlot, chip: SlotChip) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                germanWeekdayName(slot.date).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = NightColors.OnBackgroundMuted,
            )
            Text(
                slotDateHeadline(slot.date),
                style = MaterialTheme.typography.headlineMedium,
                color = NightColors.OnBackground,
            )
        }
        slot.weekEventName?.takeIf { it != chip.text }?.let { StatusChip(it, StatusTone.Sky) }
        StatusChip(chip.text, chip.tone)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** „Ändern“/„Wählen“ und, falls etwas gewählt ist, „Entfernen“ rechts in einer [FieldCard]. */
@Composable
private fun FieldActions(changeLabel: String, enabled: Boolean, onChange: () -> Unit, onRemove: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.End) {
        SecondaryButton(changeLabel, onChange, enabled = enabled, outlined = false)
        if (onRemove != null) TextButton(onClick = onRemove) { Text("Entfernen") }
    }
}

/** Auf dem Handy volle Breite untereinander, sonst nebeneinander rechts. */
@Composable
private fun EditorActions(
    saveText: String,
    saveEnabled: Boolean,
    discardEnabled: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < COMPACT_WIDTH) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(saveText, onSave, Modifier.fillMaxWidth(), saveEnabled, BUTTON_HEIGHT)
                SecondaryButton(
                    "Verwerfen",
                    onDiscard,
                    Modifier.fillMaxWidth(),
                    discardEnabled,
                    height = BUTTON_HEIGHT,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                SecondaryButton("Verwerfen", onDiscard, enabled = discardEnabled, height = BUTTON_HEIGHT)
                PrimaryButton(saveText, onSave, enabled = saveEnabled, height = BUTTON_HEIGHT)
            }
        }
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

private val previewWeekEvents = listOf(
    WeekEvent("kongress", "Kongress", cancelsTalk = true),
    WeekEvent("besuch", "Kreisaufseher"),
)

@Composable
private fun EditorPreview(slot: TalkSlot, original: TalkSlot?, chip: SlotChip) = PreviewThemeWrapper {
    TalkSlotEditor(
        editor = TalkSlotEditorState(slot, original),
        chip = chip,
        weekEvents = previewWeekEvents,
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
        onOpenHistory = {},
        onSave = {},
        onCancel = {},
    )
}

@ThemePreviews
@Composable
private fun TalkSlotEditorMissingSpeakerPreview() {
    val slot = TalkSlot(LocalDate(2026, 11, 1), LocalTime(17, 30), talkNumber = 12, talkTitle = "Der Weg zum Glück")
    EditorPreview(slot, slot, SlotChip("Redner fehlt", StatusTone.Coral))
}

@ThemePreviews
@Composable
private fun TalkSlotEditorCompletePreview() {
    val slot = TalkSlot(
        LocalDate(2026, 11, 8),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
        confirmed = true,
    )
    EditorPreview(slot, slot, SlotChip("Bestätigt", StatusTone.Mint))
}
