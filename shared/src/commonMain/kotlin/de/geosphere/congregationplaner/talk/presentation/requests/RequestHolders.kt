package de.geosphere.congregationplaner.talk.presentation.requests

import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.usecase.SaveSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.presentation.schedule.withExternalSpeaker
import de.geosphere.congregationplaner.talk.presentation.schedule.withFreeSpeaker
import de.geosphere.congregationplaner.talk.presentation.schedule.withFreeTalk
import de.geosphere.congregationplaner.talk.presentation.schedule.withSpeaker
import de.geosphere.congregationplaner.talk.presentation.schedule.withTalk
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Zustand der Übersicht, den ViewModel und Holder teilen. */
class SpeakerRequestsSession(private val scope: CoroutineScope, val today: LocalDate) {
    internal val state = MutableStateFlow(SpeakerRequestsUiState(today))
    val uiState: StateFlow<SpeakerRequestsUiState> = state.asStateFlow()
    internal var user: AppUser? = null

    /** Öffnet einen Dialog; nur für Planer und nicht während des Speicherns. */
    internal fun openDialog(dialog: RequestDialog) = state.update {
        if (it.canEdit && !it.isSaving) it.copy(dialog = dialog, errorMessage = null) else it
    }

    /**
     * Eine Änderung: der Dialog schließt erst nach dem Erfolg, ein Fehler bleibt mit offenem Dialog sichtbar. [apply]
     * übernimmt das Ergebnis sofort in die Übersicht, denn auf dem Desktop kommt der neue Stand erst mit der nächsten
     * Abfrage; sonst gingen schnelle Folgeänderungen auf dem alten Stand verloren.
     */
    internal fun <T> run(
        action: suspend (AppUser) -> Result<T>,
        apply: (SpeakerRequestsUiState, T) -> SpeakerRequestsUiState,
    ) {
        val user = user ?: return
        if (state.value.isSaving || !state.value.canEdit) return
        state.update { it.copy(isSaving = true, errorMessage = null) }
        scope.launch {
            action(user)
                .onSuccess { result -> state.update { apply(it, result).copy(isSaving = false, dialog = null) } }
                .onFailure { error -> state.update { it.copy(isSaving = false, errorMessage = messageOf(error)) } }
        }
    }
}

/** Kontaktversuch, Zusage, Absage und Tag zuweisen für bestehende Anfragen. */
class RequestChangesHolder(
    private val session: SpeakerRequestsSession,
    private val useCases: SpeakerRequestChangeUseCases,
) {
    fun openContact(open: OpenRequest) = session.openDialog(RequestDialog.Contact(open.request))

    fun saveContact(kind: ContactKind, note: String) {
        val dialog = session.state.value.dialog as? RequestDialog.Contact ?: return
        val attempt = ContactAttempt(session.today, kind, note)
        session.run({ user -> useCases.addContact(user, dialog.request, attempt) }, SpeakerRequestsUiState::withRequest)
    }

    fun accept(open: OpenRequest) =
        session.run({ user -> useCases.accept(user, open) }) { state, _ -> state.withoutRequest(open.request.id) }

    fun askDecline(open: OpenRequest) = session.openDialog(RequestDialog.Decline(open))

    fun confirmDecline() {
        val dialog = session.state.value.dialog as? RequestDialog.Decline ?: return
        session.run({ user -> useCases.decline(user, dialog.open) }) { state, _ ->
            state.withoutRequest(dialog.open.request.id)
        }
    }

    fun openAssign(open: OpenRequest) = session.openDialog(RequestDialog.Assign(open.request))

    fun assign(slot: TalkSlot) {
        val dialog = session.state.value.dialog as? RequestDialog.Assign ?: return
        session.run({ user -> useCases.assign(user, dialog.request, slot) }, SpeakerRequestsUiState::withAssigned)
    }
}

/** Neue Anfrage ohne Tag: Redner und Vortrag werden wie im Termin gewählt. */
class RequestDraftHolder(private val session: SpeakerRequestsSession, private val save: SaveSpeakerRequestUseCase) {
    fun newRequest() = session.openDialog(RequestDialog.New(TalkSlot(session.today, LocalTime(0, 0))))

    fun selectSpeaker(speaker: MemberRef?) = update { it.withSpeaker(speaker) ?: it }

    fun selectExternalSpeaker(speaker: ExternalSpeaker?) = update { it.withExternalSpeaker(speaker) }

    fun selectFreeSpeaker(name: String) = update { it.withFreeSpeaker(name) }

    fun selectTalk(talk: Talk?) = update { it.withTalk(talk) }

    fun selectFreeTalk(title: String) = update { it.withFreeTalk(title) }

    fun updateNote(note: String) = session.state.update { state ->
        val dialog = state.dialog as? RequestDialog.New ?: return@update state
        state.copy(dialog = dialog.copy(note = note.take(SpeakerRequest.MAX_NOTE_LENGTH)))
    }

    fun saveNew() {
        val dialog = session.state.value.dialog as? RequestDialog.New ?: return
        val request = requestFromDraft(dialog.draft, dialog.note, session.today)
        session.run({ user -> save(user, request, session.today) }, SpeakerRequestsUiState::withNewRequest)
    }

    private fun update(change: (TalkSlot) -> TalkSlot) = session.state.update { state ->
        val dialog = state.dialog as? RequestDialog.New ?: return@update state
        state.copy(dialog = dialog.copy(draft = change(dialog.draft)))
    }
}

internal fun SpeakerRequestsUiState.withRequest(request: SpeakerRequest): SpeakerRequestsUiState =
    copy(items = items.map { if (it.request.id == request.id) it.copy(request = request) else it })

internal fun SpeakerRequestsUiState.withoutRequest(id: String): SpeakerRequestsUiState =
    copy(items = items.filterNot { it.request.id == id })

internal fun SpeakerRequestsUiState.withNewRequest(request: SpeakerRequest): SpeakerRequestsUiState =
    copy(items = items + OpenRequest(request, slot = null, needsFollowUp = false, datePassed = false))

/** Die Anfrage liegt jetzt auf [slot]: Redner und Vortrag stehen am Termin, der Tag ist belegt. */
internal fun SpeakerRequestsUiState.withAssigned(slot: TalkSlot): SpeakerRequestsUiState = copy(
    items = items.map { open ->
        if (open.request.id != slot.requestId) {
            open
        } else {
            val request = open.request
            val dated = SpeakerRequest(request.id, request.createdOn, request.note, request.contactAttempts)
            open.copy(request = dated, slot = slot)
        }
    },
    allSlots = allSlots.filterNot { it.date == slot.date } + slot,
)

internal fun messageOf(error: Throwable): String =
    (error as? TalkScheduleException)?.message ?: "Die Anfrage konnte nicht gespeichert werden."
