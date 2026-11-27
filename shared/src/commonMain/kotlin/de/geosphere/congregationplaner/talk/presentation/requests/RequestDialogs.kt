package de.geosphere.congregationplaner.talk.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import de.geosphere.congregationplaner.talk.presentation.picker.SpeakerActions
import de.geosphere.congregationplaner.talk.presentation.picker.SpeakerPickerDialog
import de.geosphere.congregationplaner.talk.presentation.picker.TalkPickerDialog
import de.geosphere.congregationplaner.talk.presentation.schedule.slotSpeakerText
import de.geosphere.congregationplaner.talk.presentation.schedule.slotTalkText
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.dialog.ConfirmDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Der Dialog, der im Zustand offen ist. */
@Composable
internal fun RequestDialogs(state: SpeakerRequestsUiState, actions: SpeakerRequestsActions) {
    val error = state.errorMessage
    when (val dialog = state.dialog) {
        is RequestDialog.Contact ->
            ContactAttemptDialog(state.isSaving, error, actions.onSaveContact, actions.onDismissDialog)
        is RequestDialog.Assign ->
            AssignDayDialog(state.assignableSlots, error, actions.onPickDay, actions.onDismissDialog)
        is RequestDialog.Decline -> DeclineDialog(dialog, actions.onConfirmDecline, actions.onDismissDialog)
        is RequestDialog.New -> NewRequestDialog(dialog, state, actions)
        null -> Unit
    }
}

@Composable
private fun ContactAttemptDialog(
    isSaving: Boolean,
    error: String?,
    onSave: (ContactKind, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(ContactKind.CALL) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kontaktversuch heute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ContactKind.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = option == kind, onClick = { kind = option }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == kind, onClick = { kind = option })
                        Text(option.label)
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = limitAttemptNote(it) },
                    label = { Text("Notiz") },
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = NightColors.Coral) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(kind, note) }, enabled = !isSaving) { Text("Eintragen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun AssignDayDialog(slots: List<TalkSlot>, error: String?, onPick: (TalkSlot) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag zuweisen") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (slots.isEmpty()) Text("Es gibt keinen freien Tag. Lege zuerst Termine an.")
                slots.forEach { slot -> TextButton(onClick = { onPick(slot) }) { Text(slot.date.toDisplayText()) } }
                error?.let { Text(it, color = NightColors.Coral) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun DeclineDialog(dialog: RequestDialog.Decline, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Anfrage abgesagt?",
        message = declineQuestion(dialog.open),
        confirmText = "Löschen",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDangerous = true,
    )
}

@Composable
private fun NewRequestDialog(
    dialog: RequestDialog.New,
    state: SpeakerRequestsUiState,
    actions: SpeakerRequestsActions,
) {
    var showSpeakerPicker by remember { mutableStateOf(false) }
    var showTalkPicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        title = { Text("Anfrage ohne Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(slotSpeakerText(dialog.draft) ?: "Kein Redner gewählt")
                TextButton(onClick = { showSpeakerPicker = true }) { Text("Redner wählen") }
                Text(slotTalkText(dialog.draft) ?: "Vortrag offen")
                TextButton(onClick = { showTalkPicker = true }) { Text("Vortrag wählen") }
                OutlinedTextField(
                    value = dialog.note,
                    onValueChange = actions.onNoteChange,
                    label = { Text("Notiz, z. B. Wunschzeitraum") },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorMessage?.let { Text(it, color = NightColors.Coral) }
            }
        },
        confirmButton = { TextButton(onClick = actions.onSaveNew, enabled = !state.isSaving) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = actions.onDismissDialog) { Text("Abbrechen") } },
    )
    if (showSpeakerPicker) {
        SpeakerPickerDialog(
            members = state.speakers,
            externalSpeakers = state.externalSpeakers,
            canViewMembers = state.canEdit,
            actions = SpeakerActions(
                onSelectMember = { actions.onSelectSpeaker(it).also { showSpeakerPicker = false } },
                onSelectExternal = { actions.onSelectExternal(it).also { showSpeakerPicker = false } },
                onFreeSpeaker = { actions.onFreeSpeaker(it).also { showSpeakerPicker = false } },
            ),
            onDismiss = { showSpeakerPicker = false },
        )
    }
    if (showTalkPicker) {
        TalkPickerDialog(
            talks = state.talks,
            onSelect = { actions.onSelectTalk(it).also { showTalkPicker = false } },
            onDismiss = { showTalkPicker = false },
            onFreeEntry = { actions.onFreeTalk(it).also { showTalkPicker = false } },
        )
    }
}

private val previewState = SpeakerRequestsUiState(
    today = LocalDate(2026, 11, 24),
    canEdit = true,
    isLoading = false,
    allSlots = listOf(TalkSlot(LocalDate(2026, 12, 13), LocalTime(10, 0))),
    speakers = listOf(Speaker("m1", "Muster", "Max")),
    externalSpeakers = listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord")),
    talks = listOf(Talk(12, "Der Weg zum Glück")),
)

@ThemePreviews
@Composable
private fun ContactAttemptDialogPreview() = PreviewThemeWrapper {
    val dialog = RequestDialog.Contact(SpeakerRequest("r1", previewState.today))
    RequestDialogs(previewState.copy(dialog = dialog), SpeakerRequestsActions())
}

@ThemePreviews
@Composable
private fun AssignDayDialogPreview() = PreviewThemeWrapper {
    val dialog = RequestDialog.Assign(SpeakerRequest("u1", previewState.today))
    RequestDialogs(previewState.copy(dialog = dialog), SpeakerRequestsActions())
}

@ThemePreviews
@Composable
private fun DeclineDialogPreview() = PreviewThemeWrapper {
    val slot = TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), speakerLastName = "Muster", requestId = "r1")
    val open = OpenRequest(SpeakerRequest("r1", previewState.today), slot, needsFollowUp = false, datePassed = false)
    RequestDialogs(previewState.copy(dialog = RequestDialog.Decline(open)), SpeakerRequestsActions())
}

@ThemePreviews
@Composable
private fun NewRequestDialogPreview() = PreviewThemeWrapper {
    val draft = TalkSlot(previewState.today, LocalTime(0, 0), speakerLastName = "Huber (Nord)", speakerFirstName = "")
    RequestDialogs(
        previewState.copy(dialog = RequestDialog.New(draft, "Frühjahr"), errorMessage = "Redner fehlt."),
        SpeakerRequestsActions(),
    )
}
