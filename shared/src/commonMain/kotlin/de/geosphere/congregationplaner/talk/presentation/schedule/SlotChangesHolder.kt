package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.usecase.ConfirmSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ReassignTalkUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

/** Die UseCases, die einen Termin schreiben. */
@Inject
class SlotChangeUseCases(
    val moveSlot: MoveTalkSlotUseCase,
    val deleteSlot: DeleteTalkSlotUseCase,
    val confirmSpeaker: ConfirmSpeakerUseCase,
    val reassignTalk: ReassignTalkUseCase,
)

/** Speichert und löscht Termine; [onYearChange] zeigt das Jahr des Termins. */
class SlotChangesHolder(
    private val session: TalkScheduleSession,
    private val useCases: SlotChangeUseCases,
    private val onYearChange: (Int) -> Unit,
) {
    private val store get() = session.store

    fun save() {
        val state = store.value
        val editor = state.editor ?: return
        when {
            state.isSaving -> Unit
            state.canPlan -> savePlanned(state, editor)
            else -> saveInformed(editor)
        }
    }

    private fun savePlanned(state: TalkScheduleUiState, editor: TalkSlotEditorState) {
        val user = session.allowedUser() ?: return
        val slot = editor.slot
        // shortcut: geprüft wird nur gegen das geladene Jahr; beim Verschieben über den Jahreswechsel kann ein
        // Termin im anderen Jahr überschrieben werden. Bei Bedarf vor dem Speichern das Zieldokument lesen.
        if (state.slots.any { it.date == slot.date && it.date != editor.original?.date }) {
            store.update { it.copy(errorMessage = "Für diesen Tag gibt es schon einen Termin.") }
        } else {
            write(user, editor)
        }
    }

    private fun write(user: AppUser, editor: TalkSlotEditorState) {
        val slot = editor.slot
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.moveSlot(user, editor.original?.date ?: slot.date, slot)
                .onSuccess { saved ->
                    store.update { it.copy(editor = TalkSlotEditorState(saved, saved), isSaving = false) }
                    // Ein Termin in einem anderen Jahr wäre sonst nicht in der Liste zu sehen.
                    onYearChange(saved.date.year)
                }.onFailure { session.fail(it, "Termin konnte nicht gespeichert werden.") }
        }
    }

    fun delete(slot: TalkSlot) {
        if (store.value.isSaving) return
        val user = session.allowedUser() ?: return
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.deleteSlot(user, slot.date)
                .onSuccess {
                    store.update { state ->
                        state.copy(
                            editor = state.editor?.takeUnless {
                                it.original?.date == slot.date
                            },
                            isSaving = false,
                        )
                    }
                }.onFailure { session.fail(it, "Termin konnte nicht gelöscht werden.") }
        }
    }

    // Der Helfer schreibt nur das Feld `confirmed`; der Rest des Termins bleibt, wie er in der Datenbank steht.
    private fun saveInformed(editor: TalkSlotEditorState) {
        val original = editor.original ?: return
        val user = session.allowedUser(confirmOnly = true) ?: return
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.confirmSpeaker(user, original.date, editor.slot.confirmed)
                .onSuccess {
                    val saved = original.copy(confirmed = editor.slot.confirmed)
                    store.update { it.copy(editor = TalkSlotEditorState(saved, saved), isSaving = false) }
                }.onFailure { session.fail(it, "Der Termin konnte nicht gespeichert werden.") }
        }
    }
}
