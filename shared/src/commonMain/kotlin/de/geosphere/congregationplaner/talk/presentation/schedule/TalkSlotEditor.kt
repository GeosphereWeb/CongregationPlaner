package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import de.geosphere.congregationplaner.congregation.presentation.MeetingTimeInput
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.presentation.navigation.germanWeekdayName
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.usecase.SlotTalkCheck
import de.geosphere.congregationplaner.talk.presentation.common.rememberToday
import de.geosphere.congregationplaner.talk.presentation.picker.MemberPickerDialog
import de.geosphere.congregationplaner.talk.presentation.picker.SpeakerActions
import de.geosphere.congregationplaner.talk.presentation.picker.SpeakerPickerDialog
import de.geosphere.congregationplaner.talk.presentation.picker.TalkPickerDialog
import de.geosphere.congregationplaner.talk.presentation.picker.offeredBy
import de.geosphere.congregationplaner.talk.presentation.picker.offering
import de.geosphere.congregationplaner.talk.presentation.picker.offeringMembers
import de.geosphere.congregationplaner.talk.presentation.picker.selectedSpeakerTalks
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.chip.StatusChip
import de.geosphere.congregationplaner.ui.chip.StatusTone
import de.geosphere.congregationplaner.ui.form.EditorActions
import de.geosphere.congregationplaner.ui.form.SwitchRow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun TalkSlotEditor(
    editor: TalkSlotEditorState,
    chip: SlotChip,
    eventTypes: List<EventType>,
    speakers: List<Speaker>,
    chairmen: List<Chairman>,
    externalSpeakers: List<ExternalSpeaker>,
    canViewMembers: Boolean,
    isSaving: Boolean,
    onChange: (TalkSlot) -> Unit,
    onRequestedChange: (Boolean) -> Unit,
    onSelectWeekEvent: (EventType?) -> Unit,
    onSelectTalk: (Talk?) -> Unit,
    speakerActions: SpeakerActions,
    onSelectChairman: (Chairman?) -> Unit,
    onOpenHistory: (Int) -> Unit,
    onOpenSpeaker: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val slot = editor.slot
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTalkPicker by remember { mutableStateOf(false) }
    var showSpeakerPicker by remember { mutableStateOf(false) }
    var showChairmanPicker by remember { mutableStateOf(false) }
    // Wer schon Vorsitzender ist, kann nicht Redner sein, und umgekehrt.
    val selectableSpeakers = speakers.filterNot { it.memberId == slot.chairmanMemberId }
    val selectableChairmen = chairmen.filterNot { it.memberId == slot.speakerMemberId }
    val speakerText = slotSpeakerText(slot)
    val talkText = slotTalkText(slot)
    val talkCheck = editor.talkCheck
    val historyTalk = editor.historyTalkNumber(isSaving)
    val directorySpeaker = editor.directorySpeakerId(isSaving)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlotHeader(slot, chip)
        TalkDateInput(slot.date, onDateChange = { onChange(slot.copy(date = it)) })
        MeetingTimeInput("Uhrzeit", slot.time, onTimeChange = { onChange(slot.copy(time = it)) })

        WeekEventDropdown(slot, eventTypes, onSelectWeekEvent)
        SwitchRow("Vortrag entfällt", slot.talkCancelled) { onChange(slot.copy(talkCancelled = it)) }

        TalkCard(
            slot = slot,
            talkText = talkText,
            inactiveNumber = talkCheck.inactiveNumber,
            historyTalk = historyTalk,
            actions = CardActions(
                onPick = { showTalkPicker = true },
                onRemove = { onSelectTalk(null) },
                onOpen = { historyTalk?.let(onOpenHistory) },
            ),
        )
        talkCheck.outdatedTitle?.let { title ->
            SecondaryButton(
                text = "Titel aktualisieren: „$title“",
                onClick = { onChange(slot.copy(talkTitle = title)) },
                enabled = !isSaving,
            )
        }

        SpeakerCard(
            slot = slot,
            speakerText = speakerText,
            supporting = speakerSupporting(
                canViewMembers = canViewMembers,
                noSpeakers = speakers.isEmpty() && externalSpeakers.isEmpty(),
                speakerMissing = speakerText == null && !slot.talkCancelled,
            ),
            directorySpeaker = directorySpeaker,
            actions = CardActions(
                onPick = { showSpeakerPicker = true },
                onRemove = { speakerActions.onSelectMember(null) },
                onOpen = { directorySpeaker?.let(onOpenSpeaker) },
            ),
        )

        ChairmanCard(
            slot = slot,
            supporting = chairmanSupporting(canViewMembers, chairmen.isEmpty(), selectableChairmen.isEmpty()),
            canPick = canViewMembers && selectableChairmen.isNotEmpty(),
            onPick = { showChairmanPicker = true },
            onRemove = { onSelectChairman(null) },
        )

        RequestSwitch(slot, onRequestedChange)

        SwitchRow(
            label = "Informiert",
            checked = slot.confirmed,
            enabled = slot.canBeInformed,
        ) { onChange(slot.copy(confirmed = it)) }

        OutlinedTextField(
            value = slot.note.orEmpty(),
            onValueChange = { onChange(slot.copy(note = it)) },
            label = { Text("Notiz") },
            supportingText = { Text("${slot.note.orEmpty().length} / ${TalkSlot.MAX_NOTE_LENGTH}") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        EditorActions(
            saveText = if (isSaving) "Speichert …" else "Speichern",
            saveEnabled = !isSaving && editor.isDirty,
            discardEnabled = !isSaving,
            onSave = onSave,
            onDiscard = onCancel,
            // Nur ein gespeicherter Termin lässt sich löschen; ein neuer ist noch nicht angelegt.
            onDelete = if (editor.original != null) ({ showDeleteConfirm = true }) else null,
            deleteText = "Termin löschen",
            deleteEnabled = !isSaving,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Termin löschen?") },
            text = { Text("Der Termin ${slotDateHeadline(slot.date)} wird endgültig gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NightColors.Coral),
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") } },
        )
    }

    if (showSpeakerPicker) {
        SpeakerPickerDialog(
            members = selectableSpeakers.offeringMembers(slot.talkNumber),
            externalSpeakers = externalSpeakers.offering(slot.talkNumber),
            canViewMembers = canViewMembers,
            actions = speakerActions.closingPicker { showSpeakerPicker = false },
            onDismiss = { showSpeakerPicker = false },
            usage = editor.usage,
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
            // Die Auswahl zeigt und übernimmt den Titel, der am Termintag gilt.
            talks = talkCheck.talksOnDate.offeredBy(slot.selectedSpeakerTalks(speakers, externalSpeakers)),
            onSelect = {
                onSelectTalk(it)
                showTalkPicker = false
            },
            onDismiss = { showTalkPicker = false },
            onFreeEntry = {
                onChange(slot.withFreeTalk(it))
                showTalkPicker = false
            },
            usage = editor.usage,
            today = rememberToday(),
        )
    }
}

/**
 * Editor für den Vortragsplanungs-Helfer: Der Termin steht nur zum Lesen da, geändert wird allein „Informiert“.
 */
@Composable
internal fun TalkSlotConfirmEditor(
    editor: TalkSlotEditorState,
    chip: SlotChip,
    isSaving: Boolean,
    onInformedChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val slot = editor.slot
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlotHeader(slot, chip)
        slotReadOnlyLines(slot).forEach { Text(it) }
        SwitchRow("Informiert", slot.confirmed, enabled = slot.canBeInformed && !isSaving, onInformedChange)
        EditorActions(
            saveText = if (isSaving) "Speichert …" else "Speichern",
            saveEnabled = !isSaving && editor.isDirty,
            discardEnabled = !isSaving,
            onSave = onSave,
            onDiscard = onCancel,
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
    onFreeSpeaker = {
        onFreeSpeaker(it)
        close()
    },
)

private val previewEventTypes = listOf(
    EventType("kongress", "Kongress", cancelsTalk = true, icon = "ic_cp_arena_duo"),
    EventType("besuch", "Kreisaufseher"),
)

@Composable
private fun EditorPreview(slot: TalkSlot, original: TalkSlot?, chip: SlotChip) = PreviewThemeWrapper {
    TalkSlotEditor(
        editor = TalkSlotEditorState(
            slot,
            original,
            talkCheck = SlotTalkCheck(talksOnDate = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Weiterer Vortrag"))),
        ),
        chip = chip,
        eventTypes = previewEventTypes,
        speakers = listOf(Speaker("1", "Muster", "Max")),
        chairmen = listOf(Chairman("2", "Beispiel", "Tim")),
        externalSpeakers = listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord")),
        canViewMembers = true,
        isSaving = false,
        onChange = {},
        onRequestedChange = {},
        onSelectWeekEvent = {},
        onSelectTalk = {},
        speakerActions = SpeakerActions(),
        onSelectChairman = {},
        onOpenHistory = {},
        onOpenSpeaker = {},
        onSave = {},
        onCancel = {},
        onDelete = {},
    )
}

@ThemePreviews
@Composable
private fun TalkSlotEditorMissingSpeakerPreview() {
    val slot = TalkSlot(LocalDate(2026, 11, 1), LocalTime(17, 30), talkNumber = 12, talkTitle = "Der Weg zum Glück")
    EditorPreview(slot, slot, SlotChip("Nicht geplant", StatusTone.Coral))
}

@ThemePreviews
@Composable
private fun TalkSlotConfirmEditorPreview() = PreviewThemeWrapper {
    val slot = TalkSlot(
        LocalDate(2026, 11, 8),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
        note = "Bringt Unterlagen mit",
    )
    val chip = SlotChip("Geplant", StatusTone.Gold)
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        TalkSlotConfirmEditor(TalkSlotEditorState(slot, slot), chip, false, {}, {}, {})
        TalkSlotConfirmEditor(
            TalkSlotEditorState(slot.copy(confirmed = true), slot),
            SlotChip("Informiert", StatusTone.Mint),
            false,
            {},
            {},
            {},
        )
    }
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
    EditorPreview(slot, slot, SlotChip("Informiert", StatusTone.Mint))
}

@ThemePreviews
@Composable
private fun TalkSlotEditorExternalSpeakerPreview() {
    val slot = TalkSlot(
        LocalDate(2026, 11, 22),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
    )
    EditorPreview(slot, slot, SlotChip("Geplant", StatusTone.Gold))
}

@ThemePreviews
@Composable
private fun TalkSlotEditorFreeEntriesPreview() {
    val slot = TalkSlot(LocalDate(2026, 11, 15), LocalTime(17, 30))
        .withFreeTalk("Sondervortrag zum Bezirkskongress")
        .withFreeSpeaker("Gast, Paul (Hamburg)")
    EditorPreview(slot, slot, SlotChip("Geplant", StatusTone.Gold))
}
