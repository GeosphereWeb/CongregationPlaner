package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Der geöffnete Termin: für den Helfer nur „Informiert“, für den Planer der Editor samt „Umplanen“. */
@Composable
internal fun SlotDetailContent(
    state: TalkScheduleUiState,
    editor: TalkSlotEditorState,
    nextDate: LocalDate?,
    actions: TalkScheduleActions,
    onCancelEditing: () -> Unit,
) {
    if (!state.canPlan) {
        TalkSlotConfirmEditor(
            editor = editor,
            chip = slotChip(slotStatus(editor.slot, nextDate)),
            isSaving = state.isSaving,
            onInformedChange = actions.onInformedChange,
            onSave = actions.onSave,
            onCancel = onCancelEditing,
        )
    } else {
        if (state.canStartReassign) {
            SecondaryButton(
                text = "Auf anderen Tag umplanen",
                onClick = actions.onStartReassign,
                outlined = false,
            )
        }
        TalkSlotEditor(
            editor = editor,
            chip = slotChip(slotStatus(editor.slot, nextDate)),
            weekEvents = state.weekEvents,
            talks = state.talks,
            speakers = state.speakers,
            chairmen = state.chairmen,
            externalSpeakers = state.externalSpeakers,
            canViewMembers = state.canViewMembers,
            isSaving = state.isSaving,
            onChange = actions.onEditorChange,
            onSelectWeekEvent = actions.onSelectWeekEvent,
            onSelectTalk = actions.onSelectTalk,
            speakerActions = actions.speaker,
            onSelectChairman = actions.onSelectChairman,
            onOpenHistory = actions.onOpenHistory,
            onSave = actions.onSave,
            onCancel = onCancelEditing,
            onDelete = { editor.original?.let(actions.onDelete) },
        )
    }
}

@ThemePreviews
@Composable
private fun SlotDetailContentPreview() = PreviewThemeWrapper {
    val slot = TalkSlot(
        LocalDate(2026, 11, 8),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    )
    val state =
        TalkScheduleUiState(year = 2026, slots = listOf(slot), isLoading = false, canView = true, canPlan = true)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlotDetailContent(state, TalkSlotEditorState(slot, slot), null, TalkScheduleActions(), onCancelEditing = {})
    }
}
