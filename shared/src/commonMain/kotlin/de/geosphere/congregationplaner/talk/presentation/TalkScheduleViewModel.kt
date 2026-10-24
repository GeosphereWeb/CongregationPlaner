package de.geosphere.congregationplaner.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Der gerade bearbeitete Termin; [original] ist `null`, solange ein neuer Termin entsteht. */
data class TalkSlotEditorState(val slot: TalkSlot, val original: TalkSlot? = null) {
    val isNew: Boolean get() = original == null
    val isDirty: Boolean get() = slot != original
}

data class TalkScheduleUiState(
    val year: Int,
    val slots: List<TalkSlot> = emptyList(),
    val weekEvents: List<WeekEvent> = emptyList(),
    val talks: List<Talk> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = true,
    val canPlan: Boolean = false,
    /** Ohne diese Berechtigung können die Redner nicht geladen werden. */
    val canViewMembers: Boolean = false,
    val editor: TalkSlotEditorState? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** Vortragsplanung der Versammlung des angemeldeten Nutzers, sofern er planen darf. */
@Inject
class TalkScheduleViewModel(
    private val observeYear: ObserveTalkYearUseCase,
    private val observeWeekEvents: ObserveWeekEventsUseCase,
    private val observeTalkCatalog: ObserveTalkCatalogUseCase,
    private val observeSpeakers: ObserveSpeakersUseCase,
    private val moveSlot: MoveTalkSlotUseCase,
    private val deleteSlot: DeleteTalkSlotUseCase,
    private val permissionPolicy: TalkSchedulePermissionPolicy,
    private val userPolicy: UserPermissionPolicy,
) : ViewModel() {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val mutableUiState = MutableStateFlow(TalkScheduleUiState(year = today.year))
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var slotsJob: Job? = null
    private var weekEventsJob: Job? = null
    private var talksJob: Job? = null
    private var speakersJob: Job? = null
    private var observing = false
    private var observedCongregation: CongregationRef? = null
    private var observedYear: Int? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canPlan = permissionPolicy.canPlanTalks(user)
        val canViewMembers = canPlan && userPolicy.canViewMembers(user)
        mutableUiState.update {
            it.copy(canPlan = canPlan, canViewMembers = canViewMembers, editor = it.editor?.takeIf { canPlan })
        }
        observeReferenceData(user)
        observeSlots()
    }

    fun changeYear(year: Int) {
        if (year == mutableUiState.value.year) return
        mutableUiState.update { it.copy(year = year) }
        observeSlots()
    }

    fun newSlot() {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val year = mutableUiState.value.year
        val date = today.takeIf { it.year == year } ?: LocalDate(year, 1, 1)
        val slot = TalkSlot(date = date, time = CongregationMetadata().weekendMeetingTime)
        mutableUiState.update { it.copy(editor = TalkSlotEditorState(slot), errorMessage = null) }
    }

    fun editSlot(slot: TalkSlot) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        mutableUiState.update { it.copy(editor = TalkSlotEditorState(slot, slot), errorMessage = null) }
    }

    /** Während des Speicherns bleibt der Editor unverändert, sonst würde das Ergebnis die Eingaben überschreiben. */
    fun updateEditor(slot: TalkSlot) {
        mutableUiState.update { if (it.isSaving) it else it.copy(editor = it.editor?.copy(slot = slot)) }
    }

    /** Übernimmt das Ereignis samt der Vorbelegung „Vortrag entfällt“; `null` entfernt das Ereignis. */
    fun selectWeekEvent(event: WeekEvent?) {
        val slot = mutableUiState.value.editor?.slot ?: return
        updateEditor(
            if (event == null) {
                slot.copy(weekEventId = null, weekEventName = null)
            } else {
                slot.copy(weekEventId = event.id, weekEventName = event.name, talkCancelled = event.cancelsTalk)
            },
        )
    }

    /** Übernimmt Nummer und Titel des Vortrags als Momentaufnahme; `null` entfernt den Vortrag. */
    fun selectTalk(talk: Talk?) {
        if (talk != null && !talk.active) return
        val slot = mutableUiState.value.editor?.slot ?: return
        updateEditor(slot.copy(talkNumber = talk?.number, talkTitle = talk?.title))
    }

    /** Übernimmt Name und Vorname des Redners als Momentaufnahme; `null` entfernt den Redner. */
    fun selectSpeaker(speaker: Speaker?) {
        val slot = mutableUiState.value.editor?.slot ?: return
        updateEditor(
            slot.copy(
                speakerMemberId = speaker?.memberId,
                speakerLastName = speaker?.lastName,
                speakerFirstName = speaker?.firstName,
            ),
        )
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    /** Verwirft Änderungen, lässt aber den ausgewählten Termin offen; ein neuer, ungespeicherter wird geschlossen. */
    fun discardChanges() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { state ->
            val editor = state.editor?.takeUnless { it.isNew }?.let { it.copy(slot = it.original ?: it.slot) }
            state.copy(editor = editor, errorMessage = null)
        }
    }

    fun save() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        if (state.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val slot = editor.slot
        // shortcut: geprüft wird nur gegen das geladene Jahr; beim Verschieben über den Jahreswechsel kann ein
        // Termin im anderen Jahr überschrieben werden. Bei Bedarf vor dem Speichern das Zieldokument lesen.
        if (state.slots.any { it.date == slot.date && it.date != editor.original?.date }) {
            mutableUiState.update { it.copy(errorMessage = "Für diesen Tag gibt es schon einen Termin.") }
            return
        }

        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            moveSlot(user, editor.original?.date ?: slot.date, slot)
                .onSuccess { saved ->
                    mutableUiState.update {
                        it.copy(editor = TalkSlotEditorState(saved, saved), isSaving = false)
                    }
                    // Ein Termin in einem anderen Jahr wäre sonst nicht in der Liste zu sehen.
                    changeYear(saved.date.year)
                }.onFailure { exception ->
                    fail(exception, "Termin konnte nicht gespeichert werden.")
                }
        }
    }

    fun delete(slot: TalkSlot) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteSlot(user, slot.date)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            editor = state.editor?.takeUnless { it.original?.date == slot.date },
                            isSaving = false,
                        )
                    }
                }.onFailure { exception -> fail(exception, "Termin konnte nicht gelöscht werden.") }
        }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        currentUser?.let { user ->
            observeReferenceData(user)
            observeSlots()
        }
    }

    private fun observeSlots() {
        val user = currentUser ?: return
        val congregation = user.congregation?.takeIf { permissionPolicy.canPlanTalks(user) }
        val year = mutableUiState.value.year
        if (observing && congregation == observedCongregation && year == observedYear) return

        slotsJob?.cancel()
        observedCongregation = congregation
        observedYear = year
        observing = true
        if (congregation == null) {
            mutableUiState.update { it.copy(slots = emptyList(), isLoading = false) }
            return
        }
        mutableUiState.update { it.copy(isLoading = true) }
        slotsJob = viewModelScope.launch {
            observeYear(user, year)
                .catch { exception ->
                    val message = exception.message ?: "Termine konnten nicht geladen werden."
                    // Der Flow ist beendet; ohne Zurücksetzen würde observeSlots() nie wieder laden.
                    observing = false
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { slots ->
                    mutableUiState.update { it.copy(slots = slots, isLoading = false) }
                }
        }
    }

    // Wochenereignisse und Vortragsliste sind global, die Redner stammen aus den Mitgliedern der Versammlung.
    // Sie werden nur beobachtet, solange der Nutzer planen (bzw. Mitglieder einsehen) darf.
    private fun observeReferenceData(user: AppUser) {
        val canPlan = permissionPolicy.canPlanTalks(user)
        if (!canPlan) {
            weekEventsJob?.cancel()
            weekEventsJob = null
            talksJob?.cancel()
            talksJob = null
        } else {
            if (weekEventsJob == null) {
                weekEventsJob = launchReference("Wochenereignisse", observeWeekEvents()) { events ->
                    mutableUiState.update { it.copy(weekEvents = events) }
                }
            }
            if (talksJob == null) {
                talksJob = launchReference("Vortragsliste", observeTalkCatalog()) { talks ->
                    mutableUiState.update { it.copy(talks = talks) }
                }
            }
        }
        if (canPlan && userPolicy.canViewMembers(user)) {
            if (speakersJob == null) {
                speakersJob = launchReference("Redner", observeSpeakers(user)) { speakers ->
                    mutableUiState.update { it.copy(speakers = speakers) }
                }
            }
        } else {
            speakersJob?.cancel()
            speakersJob = null
            mutableUiState.update { it.copy(speakers = emptyList()) }
        }
    }

    private fun <T> launchReference(name: String, source: Flow<T>, onValue: (T) -> Unit): Job = viewModelScope.launch {
        source
            .catch { exception ->
                val message = exception.message ?: "$name konnten nicht geladen werden."
                // Der Flow ist beendet; ohne Zurücksetzen würde er nie wieder gestartet.
                resetReferenceJob(name)
                mutableUiState.update { it.copy(errorMessage = message) }
            }.collect(onValue)
    }

    private fun resetReferenceJob(name: String) {
        when (name) {
            "Wochenereignisse" -> weekEventsJob = null
            "Vortragsliste" -> talksJob = null
            else -> speakersJob = null
        }
    }

    private fun checkAllowed(): Boolean {
        val allowed = currentUser?.let(permissionPolicy::canPlanTalks) == true && currentUser?.congregation != null
        if (!allowed) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return allowed
    }

    private fun fail(exception: Throwable, fallback: String) {
        mutableUiState.update { it.copy(isSaving = false, errorMessage = exception.message ?: fallback) }
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
