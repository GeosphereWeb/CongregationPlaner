package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.notification.domain.usecase.NotifyConfirmedChangedUseCase
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.usecase.ConfirmSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.IsSlotDateTakenUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ReassignTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveEditedSlotUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Die UseCases, die einen Termin schreiben. */
@Inject
class SlotChangeUseCases(
    val saveEditedSlot: SaveEditedSlotUseCase,
    val deleteSlot: DeleteTalkSlotUseCase,
    val confirmSpeaker: ConfirmSpeakerUseCase,
    val reassignTalk: ReassignTalkUseCase,
    val isSlotDateTaken: IsSlotDateTakenUseCase,
    val notifyConfirmedChanged: NotifyConfirmedChangedUseCase,
)

/** Speichert und löscht Termine; [onYearChange] zeigt das Jahr des Termins. */
class SlotChangesHolder(
    private val session: TalkScheduleSession,
    private val useCases: SlotChangeUseCases,
    private val today: LocalDate,
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
        if (useCases.isSlotDateTaken(state.slots, editor.slot, editor.original?.date)) {
            store.update { it.copy(errorMessage = "Für diesen Tag gibt es schon einen Termin.") }
        } else {
            val warning = requestReplacementWarning(editor.original, editor.slot)
            if (warning != null) store.update { it.copy(requestWarning = warning) } else write(user, editor)
        }
    }

    /** Der Planer bestätigt, dass die offene Anfrage beim Speichern gelöscht wird. */
    fun confirmRequestWarning() {
        val editor = store.value.editor
        store.update { it.copy(requestWarning = null) }
        val user = session.allowedUser() ?: return
        if (editor != null && !store.value.isSaving) write(user, editor)
    }

    fun dismissRequestWarning() = store.update { it.copy(requestWarning = null) }

    private fun write(user: AppUser, editor: TalkSlotEditorState) {
        val slot = editor.slot
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            val from = editor.original?.date ?: slot.date
            useCases.saveEditedSlot(user, from, editor.original, slot, requested = slot.isRequested, today = today)
                .onSuccess { saved ->
                    store.update { it.copy(editor = TalkSlotEditorState(saved, saved), isSaving = false) }
                    // Ein Termin in einem anderen Jahr wäre sonst nicht in der Liste zu sehen.
                    onYearChange(saved.date.year)
                    notifyConfirmedChanged(user, editor.original, saved)
                }.onFailure { session.fail(it, "Termin konnte nicht gespeichert werden.") }
        }
    }

    fun delete(slot: TalkSlot) {
        if (store.value.isSaving) return
        val user = session.allowedUser() ?: return
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.deleteSlot(user, slot.date, slot.requestId)
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
                    notifyConfirmedChanged(user, original, saved)
                }.onFailure { session.fail(it, "Der Termin konnte nicht gespeichert werden.") }
        }
    }

    // Die Änderung ist gespeichert; scheitert nur die Meldung an Planer und Helfer, bleibt sie still (ADR-0035).
    private fun notifyConfirmedChanged(user: AppUser, previous: TalkSlot?, saved: TalkSlot) {
        session.scope.launch { useCases.notifyConfirmedChanged(user, previous, saved) }
    }
}
