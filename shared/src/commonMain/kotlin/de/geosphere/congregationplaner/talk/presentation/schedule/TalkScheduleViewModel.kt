package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Vortragsplanung der Versammlung des angemeldeten Nutzers. Das ViewModel hält Terminliste, Filter und Jahr; Editor,
 * Auswahl im Editor, Speichern und Jahresverwaltung übernehmen die Zustandshalter (ADR-0025).
 */
@Inject
class TalkScheduleViewModel(
    private val observeYear: ObserveTalkYearUseCase,
    slotChangeUseCases: SlotChangeUseCases,
    yearSetupUseCases: YearSetupUseCases,
    referenceUseCases: ScheduleReferenceUseCases,
    private val permissionPolicy: TalkSchedulePermissionPolicy,
    private val userPolicy: UserPermissionPolicy,
) : ViewModel() {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val session = TalkScheduleSession(
        UiStateStore(TalkScheduleUiState(year = today.year)),
        viewModelScope,
        permissionPolicy,
    )
    private val store get() = session.store
    val uiState: StateFlow<TalkScheduleUiState> = session.store.state

    val editor = SlotEditorHolder(session, today)
    val picker = SlotPickerHolder(session, editor)
    val changes = SlotChangesHolder(session, slotChangeUseCases, ::changeYear)
    val yearSetup = YearSetupHolder(session, yearSetupUseCases)
    val planningSettings = PlanningSettingsHolder(session, referenceUseCases)
    private val referenceData = ScheduleReferenceDataLoader(session, referenceUseCases)

    private var slotsJob: Job? = null
    private var observedCongregation: CongregationRef? = null
    private var observedYear: Int? = null
    private var observing = false

    fun onCurrentUserChanged(user: AppUser) {
        session.user = user
        val canView = permissionPolicy.canViewTalkPlanning(user)
        val canPlan = permissionPolicy.canPlanTalks(user)
        store.update {
            it.copy(
                canView = canView,
                canPlan = canPlan,
                canViewMembers = canView && userPolicy.canViewMembers(user),
                editor = it.editor?.takeIf { editor -> canView && (canPlan || !editor.isNew) },
            )
        }
        referenceData.observe(user)
        observeSlots()
    }

    /** Nimmt einen Stand in den Filter auf oder heraus; mehrere Stände ergänzen sich. */
    fun toggleStatusFilter(status: SlotStatus) = store.update { state ->
        val filter = if (status in state.statusFilter) state.statusFilter - status else state.statusFilter + status
        state.copy(statusFilter = filter)
    }

    fun changeYear(year: Int) {
        if (year == store.value.year) return
        store.update { it.copy(year = year) }
        observeSlots()
    }

    fun dismissInfo() = store.update { it.copy(infoMessage = null) }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        store.update { it.copy(errorMessage = null) }
        session.user?.let { user ->
            referenceData.observe(user)
            observeSlots()
        }
    }

    private fun observeSlots() {
        val user = session.user ?: return
        val congregation = user.congregation?.takeIf { permissionPolicy.canViewTalkPlanning(user) }
        val year = store.value.year
        if (observing && congregation == observedCongregation && year == observedYear) return

        slotsJob?.cancel()
        observedCongregation = congregation
        observedYear = year
        observing = true
        if (congregation == null) {
            store.update { it.copy(slots = emptyList(), isLoading = false) }
            return
        }
        store.update { it.copy(isLoading = true) }
        slotsJob = viewModelScope.launch {
            observeYear(user, year)
                .catch { exception ->
                    // Der Flow ist beendet; ohne Zurücksetzen würde observeSlots() nie wieder laden.
                    observing = false
                    val message = exception.message ?: "Termine konnten nicht geladen werden."
                    store.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { slots ->
                    store.update { it.copy(slots = slots, isLoading = false) }
                }
        }
    }
}
