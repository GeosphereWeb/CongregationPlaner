package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ChangeYearSlotTimesUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ConfirmSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.CreateYearSlotsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkMembersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SyncTalkHistoryWithSlotUseCase
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
import kotlinx.datetime.LocalTime
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
    /** Gewählte Stände; leer zeigt das ganze Jahr. Bleibt beim Jahreswechsel erhalten. */
    val statusFilter: Set<SlotStatus> = emptySet(),
    val weekEvents: List<WeekEvent> = emptyList(),
    val talks: List<Talk> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val chairmen: List<Chairman> = emptyList(),
    val externalSpeakers: List<ExternalSpeaker> = emptyList(),
    val isLoading: Boolean = true,
    val canView: Boolean = false,
    val canPlan: Boolean = false,
    /** Ohne diese Berechtigung können die Redner nicht geladen werden. */
    val canViewMembers: Boolean = false,
    val editor: TalkSlotEditorState? = null,
    /** `true`, solange die Rückfrage zu „Jahr anlegen“ offen ist. */
    val confirmCreateYear: Boolean = false,
    /** Die im Dialog „Uhrzeit ändern“ gewählte Uhrzeit; `null`, solange der Dialog zu ist. */
    val yearTimeDialog: LocalTime? = null,
    val infoMessage: String? = null,
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
    private val observeTalkMembers: ObserveTalkMembersUseCase,
    private val loadExternalSpeakers: ObserveExternalSpeakersUseCase,
    private val moveSlot: MoveTalkSlotUseCase,
    private val createYearSlots: CreateYearSlotsUseCase,
    private val changeYearTimes: ChangeYearSlotTimesUseCase,
    private val deleteSlot: DeleteTalkSlotUseCase,
    private val syncHistory: SyncTalkHistoryWithSlotUseCase,
    private val confirmSpeaker: ConfirmSpeakerUseCase,
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
    private var membersJob: Job? = null
    private var externalSpeakersJob: Job? = null
    private var observing = false
    private var observedCongregation: CongregationRef? = null
    private var observedYear: Int? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canView = permissionPolicy.canViewTalkPlanning(user)
        val canPlan = permissionPolicy.canPlanTalks(user)
        val canViewMembers = canView && userPolicy.canViewMembers(user)
        mutableUiState.update {
            it.copy(
                canView = canView,
                canPlan = canPlan,
                canViewMembers = canViewMembers,
                editor = it.editor?.takeIf { canView && (canPlan || !it.isNew) },
            )
        }
        observeReferenceData(user)
        observeSlots()
    }

    /** Nimmt einen Stand in den Filter auf oder heraus; mehrere Stände ergänzen sich. */
    fun toggleStatusFilter(status: SlotStatus) {
        mutableUiState.update { state ->
            val filter = if (status in state.statusFilter) state.statusFilter - status else state.statusFilter + status
            state.copy(statusFilter = filter)
        }
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
        if (mutableUiState.value.isSaving || !checkAllowed(confirmOnly = true)) return
        mutableUiState.update { it.copy(editor = TalkSlotEditorState(slot, slot), errorMessage = null) }
    }

    /** Während des Speicherns bleibt der Editor unverändert, sonst würde das Ergebnis die Eingaben überschreiben. */
    fun updateEditor(slot: TalkSlot) {
        mutableUiState.update {
            if (it.isSaving) it else it.copy(editor = it.editor?.copy(slot = slot.withLimitedNote()))
        }
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
        if (speaker != null && speaker.memberId == slot.chairmanMemberId) return rejectSamePerson()
        updateEditor(
            slot.copy(
                speakerMemberId = speaker?.memberId,
                speakerLastName = speaker?.lastName,
                speakerFirstName = speaker?.firstName,
                externalSpeakerId = null,
                speakerCongregation = null,
            ),
        )
    }

    /** Übernimmt einen externen Redner samt seiner Versammlung als Momentaufnahme; ersetzt ein Mitglied als Redner. */
    fun selectExternalSpeaker(speaker: ExternalSpeaker?) {
        val slot = mutableUiState.value.editor?.slot ?: return
        updateEditor(
            slot.copy(
                speakerMemberId = null,
                speakerLastName = speaker?.lastName,
                speakerFirstName = speaker?.firstName,
                externalSpeakerId = speaker?.id,
                speakerCongregation = speaker?.congregationName,
            ),
        )
    }

    /** Übernimmt Name und Vorname des Vorsitzenden als Momentaufnahme; `null` entfernt den Vorsitzenden. */
    fun selectChairman(chairman: Chairman?) {
        val slot = mutableUiState.value.editor?.slot ?: return
        if (chairman != null && chairman.memberId == slot.speakerMemberId) return rejectSamePerson()
        updateEditor(
            slot.copy(
                chairmanMemberId = chairman?.memberId,
                chairmanLastName = chairman?.lastName,
                chairmanFirstName = chairman?.firstName,
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

    /** Der Helfer ändert im Editor nur „Informiert“; beim Planer geht das über [updateEditor]. */
    fun setSpeakerInformed(confirmed: Boolean) {
        mutableUiState.update { state ->
            val editor = state.editor
            if (state.isSaving || editor == null || !editor.slot.canBeInformed) {
                state
            } else {
                state.copy(editor = editor.copy(slot = editor.slot.copy(confirmed = confirmed)))
            }
        }
    }

    fun save() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        if (state.isSaving) return
        if (!state.canPlan) {
            saveInformed(editor)
            return
        }
        if (!checkAllowed()) return
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
                    val history = syncHistory(user, editor.original, saved)
                    mutableUiState.update {
                        it.copy(
                            editor = TalkSlotEditorState(saved, saved),
                            isSaving = false,
                            errorMessage = history.exceptionOrNull()?.let(::historyError),
                        )
                    }
                    // Ein Termin in einem anderen Jahr wäre sonst nicht in der Liste zu sehen.
                    changeYear(saved.date.year)
                }.onFailure { exception ->
                    fail(exception, "Termin konnte nicht gespeichert werden.")
                }
        }
    }

    // Der Helfer schreibt nur das Feld `confirmed`; der Rest des Termins bleibt, wie er in der Datenbank steht.
    private fun saveInformed(editor: TalkSlotEditorState) {
        val original = editor.original
        val user = currentUser
        if (original == null || user == null || !checkAllowed(confirmOnly = true)) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            confirmSpeaker(user, original.date, editor.slot.confirmed)
                .onSuccess {
                    val saved = original.copy(confirmed = editor.slot.confirmed)
                    mutableUiState.update {
                        it.copy(editor = TalkSlotEditorState(saved, saved), isSaving = false)
                    }
                }.onFailure { exception -> fail(exception, "Der Termin konnte nicht gespeichert werden.") }
        }
    }

    /** Öffnet die Rückfrage; angelegt wird erst nach [createYear]. */
    fun askCreateYear() {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        mutableUiState.update { it.copy(confirmCreateYear = true, errorMessage = null, infoMessage = null) }
    }

    fun dismissCreateYear() {
        mutableUiState.update { it.copy(confirmCreateYear = false) }
    }

    /** Legt alle fehlenden Termine am Wochenende des gewählten Jahres an; vorhandene bleiben unverändert. */
    fun createYear() {
        val state = mutableUiState.value
        if (state.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val year = state.year
        mutableUiState.update { it.copy(confirmCreateYear = false, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            createYearSlots(user, year)
                .onSuccess { result ->
                    mutableUiState.update { it.copy(isSaving = false, infoMessage = yearSlotsMessage(year, result)) }
                }.onFailure { exception -> fail(exception, "Termine konnten nicht angelegt werden.") }
        }
    }

    /** Öffnet den Dialog mit der häufigsten Uhrzeit des Jahres; geändert wird erst nach [changeYearTime]. */
    fun askChangeYearTime() {
        val state = mutableUiState.value
        if (state.isSaving || !checkAllowed()) return
        val initial = commonSlotTime(state.slots) ?: DEFAULT_YEAR_TIME
        mutableUiState.update { it.copy(yearTimeDialog = initial, errorMessage = null, infoMessage = null) }
    }

    fun selectYearTime(time: LocalTime) {
        mutableUiState.update { if (it.yearTimeDialog == null) it else it.copy(yearTimeDialog = time) }
    }

    fun dismissYearTime() {
        mutableUiState.update { it.copy(yearTimeDialog = null) }
    }

    /** Setzt die gewählte Uhrzeit bei allen Terminen des gewählten Jahres. */
    fun changeYearTime() {
        val state = mutableUiState.value
        val time = state.yearTimeDialog ?: return
        if (state.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val year = state.year
        mutableUiState.update { it.copy(yearTimeDialog = null, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            changeYearTimes(user, year, time)
                .onSuccess { changed ->
                    mutableUiState.update {
                        it.copy(isSaving = false, infoMessage = yearTimeMessage(year, changed, time))
                    }
                }.onFailure { exception -> fail(exception, "Uhrzeit konnte nicht geändert werden.") }
        }
    }

    fun dismissInfo() {
        mutableUiState.update { it.copy(infoMessage = null) }
    }

    fun delete(slot: TalkSlot) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteSlot(user, slot.date)
                .onSuccess {
                    val history = syncHistory(user, slot, null)
                    mutableUiState.update { state ->
                        state.copy(
                            editor = state.editor?.takeUnless { it.original?.date == slot.date },
                            isSaving = false,
                            errorMessage = history.exceptionOrNull()?.let(::historyError),
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
        val congregation = user.congregation?.takeIf { permissionPolicy.canViewTalkPlanning(user) }
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
        val canView = permissionPolicy.canViewTalkPlanning(user)
        // Wochenereignisse und Vortragsliste braucht nur der Editor des Planers.
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
        if (!canView) {
            externalSpeakersJob?.cancel()
            externalSpeakersJob = null
            mutableUiState.update { it.copy(externalSpeakers = emptyList()) }
        } else {
            if (externalSpeakersJob == null) {
                externalSpeakersJob = launchReference("Externe Redner", loadExternalSpeakers(user)) { speakers ->
                    mutableUiState.update { it.copy(externalSpeakers = speakers) }
                }
            }
        }
        if (canView && userPolicy.canViewMembers(user)) {
            if (membersJob == null) {
                membersJob = launchReference(MEMBERS_NAME, observeTalkMembers(user)) { members ->
                    mutableUiState.update { it.copy(speakers = members.speakers, chairmen = members.chairmen) }
                }
            }
        } else {
            membersJob?.cancel()
            membersJob = null
            mutableUiState.update { it.copy(speakers = emptyList(), chairmen = emptyList()) }
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
            "Externe Redner" -> externalSpeakersJob = null
            else -> membersJob = null
        }
    }

    // `confirmOnly` gilt für Aktionen, die auch der Helfer ausführen darf (Termin öffnen, „Informiert“ speichern).
    private fun checkAllowed(confirmOnly: Boolean = false): Boolean {
        val permitted = if (confirmOnly) permissionPolicy::canConfirmSpeaker else permissionPolicy::canPlanTalks
        val allowed = currentUser?.let(permitted) == true && currentUser?.congregation != null
        if (!allowed) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return allowed
    }

    // Ein Mitglied kann an einem Tag nicht Redner und Vorsitzender sein; die Dialoge bieten es gar nicht erst an.
    private fun rejectSamePerson() {
        mutableUiState.update { it.copy(errorMessage = TalkScheduleException.SpeakerIsChairman().message) }
    }

    private fun fail(exception: Throwable, fallback: String) {
        mutableUiState.update { it.copy(isSaving = false, errorMessage = exception.message ?: fallback) }
    }

    // Der Termin ist dann schon gespeichert; nur die Vortragshistorie konnte nicht nachgezogen werden.
    private fun historyError(exception: Throwable): String =
        "Termin gespeichert, aber die Vortragshistorie konnte nicht aktualisiert werden: " +
            (exception.message ?: "unbekannter Fehler")

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
        const val MEMBERS_NAME = "Redner und Vorsitzende"
        val DEFAULT_YEAR_TIME = LocalTime(10, 0)
    }
}
