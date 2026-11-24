package de.geosphere.congregationplaner.talk.presentation.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.assignableSlots
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.AcceptSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.AddContactAttemptUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.AssignRequestToDateUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeclineSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveAllTalkSlotsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveOpenRequestsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkMembersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveSpeakerRequestUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Der offene Dialog der Übersicht; `null` im Zustand heißt: keiner. */
sealed interface RequestDialog {
    data class Contact(val request: SpeakerRequest) : RequestDialog

    data class Assign(val request: SpeakerRequest) : RequestDialog

    data class Decline(val open: OpenRequest) : RequestDialog

    /** Neue Anfrage ohne Tag; Redner und Vortrag stehen im [draft], damit die Auswahl wie im Termin funktioniert. */
    data class New(val draft: TalkSlot, val note: String = "") : RequestDialog
}

data class SpeakerRequestsUiState(
    val today: LocalDate,
    val items: List<OpenRequest> = emptyList(),
    val canEdit: Boolean = false,
    val isLoading: Boolean = true,
    val allSlots: List<TalkSlot> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val externalSpeakers: List<ExternalSpeaker> = emptyList(),
    val talks: List<Talk> = emptyList(),
    val dialog: RequestDialog? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val assignableSlots: List<TalkSlot> get() = assignableSlots(allSlots, today)
}

/** Die UseCases, die eine Anfrage ändern. */
@Inject
class SpeakerRequestChangeUseCases(
    val save: SaveSpeakerRequestUseCase,
    val addContact: AddContactAttemptUseCase,
    val accept: AcceptSpeakerRequestUseCase,
    val decline: DeclineSpeakerRequestUseCase,
    val assign: AssignRequestToDateUseCase,
)

/** Redner und Vorträge für den Dialog „Anfrage ohne Tag“. */
@Inject
class RequestReferenceUseCases(
    val members: ObserveTalkMembersUseCase,
    val externals: ObserveExternalSpeakersUseCase,
    val catalog: ObserveTalkCatalogUseCase,
)

/**
 * Übersicht der offenen Redneranfragen (ADR-0039): Planer ändern sie, Helfer sehen sie nur. Die Änderungen liegen in
 * [changes] (bestehende Anfragen) und [draft] (neue Anfrage ohne Tag).
 */
@Inject
class SpeakerRequestsViewModel(
    private val observeOpen: ObserveOpenRequestsUseCase,
    private val observeSlots: ObserveAllTalkSlotsUseCase,
    changeUseCases: SpeakerRequestChangeUseCases,
    private val reference: RequestReferenceUseCases,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val session = SpeakerRequestsSession(viewModelScope, Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val uiState = session.uiState

    val changes = RequestChangesHolder(session, changeUseCases)
    val draft = RequestDraftHolder(session, changeUseCases.save)

    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        if (user == session.user) return
        session.user = user
        val canEdit = policy.canPlanTalks(user)
        val today = session.today
        session.state.value = SpeakerRequestsUiState(today, canEdit = canEdit)
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            // Redner, Vorträge und freie Tage braucht nur, wer Anfragen ändern darf.
            val members = if (canEdit) reference.members(user) else flowOf(null)
            val externals = if (canEdit) reference.externals(user) else flowOf(emptyList())
            val talks = if (canEdit) reference.catalog(user) else flowOf(emptyList())
            val slots = if (canEdit) observeSlots(user) else flowOf(emptyList())
            combine(observeOpen(user, today), slots, members, externals, talks) { items, all, people, ext, catalog ->
                session.state.value.copy(
                    items = items,
                    allSlots = all,
                    speakers = people?.speakers.orEmpty(),
                    externalSpeakers = ext,
                    talks = catalog.filter { it.active },
                    isLoading = false,
                )
            }.catch { error ->
                emit(session.state.value.copy(isLoading = false, errorMessage = messageOf(error)))
            }.collect { next -> session.state.value = next }
        }
    }

    fun dismissDialog() {
        if (!session.state.value.isSaving) session.state.update { it.copy(dialog = null) }
    }

    fun dismissError() = session.state.update { it.copy(errorMessage = null) }
}
