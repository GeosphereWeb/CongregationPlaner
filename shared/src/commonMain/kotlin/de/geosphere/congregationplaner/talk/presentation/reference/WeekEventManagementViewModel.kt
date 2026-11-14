package de.geosphere.congregationplaner.talk.presentation.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.usecase.ListCountriesUseCase
import de.geosphere.congregationplaner.organization.presentation.country.CountryChoiceHolder
import de.geosphere.congregationplaner.organization.presentation.country.CountryChoiceState
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteWeekEventUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Das gerade bearbeitete Wochenereignis; [original] ist `null`, solange ein neues entsteht. */
data class WeekEventEditorState(val name: String, val cancelsTalk: Boolean, val original: WeekEvent? = null) {
    val isNew: Boolean get() = original == null
    val event: WeekEvent get() = WeekEvent(original?.id.orEmpty(), name, cancelsTalk)
    val isDirty: Boolean get() = isNew || event != original
}

data class WeekEventUiState(
    val events: List<WeekEvent> = emptyList(),
    val isLoading: Boolean = true,
    val canManage: Boolean = false,
    /** Das Land, dessen Ereignisse gepflegt werden. */
    val country: CountryChoiceState = CountryChoiceState(),
    val editor: WeekEventEditorState? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** Pflege der Wochenereignisse eines Landes durch globale Administratoren; das Land wird zuerst gewählt. */
@Inject
class WeekEventManagementViewModel(
    private val observeEvents: ObserveWeekEventsUseCase,
    private val saveEvent: SaveWeekEventUseCase,
    private val deleteEvent: DeleteWeekEventUseCase,
    private val policy: TalkReferenceDataPolicy,
    listCountries: ListCountriesUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WeekEventUiState())
    val uiState = mutableUiState.asStateFlow()

    val countries = CountryChoiceHolder(
        viewModelScope,
        listCountries,
        read = { mutableUiState.value.country },
        write = { country -> mutableUiState.update { it.copy(country = country) } },
        // Another country: load its list; an open editor belonged to the old country.
        onSelected = { country ->
            observeJob?.cancel()
            observeJob = null
            mutableUiState.update { it.copy(events = emptyList(), editor = null, isLoading = country != null) }
            observe()
        },
    )

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canManage = policy.canManage(user)
        mutableUiState.update { it.copy(canManage = canManage, editor = it.editor?.takeIf { canManage }) }
        if (canManage) {
            countries.load(user.congregation)
            observe()
        } else {
            observeJob?.cancel()
            observeJob = null
            mutableUiState.update { it.copy(events = emptyList(), isLoading = false) }
        }
    }

    fun newEvent() {
        val state = mutableUiState.value
        if (state.isSaving || !checkAllowed() || state.country.selected == null) return
        mutableUiState.update { it.copy(editor = WeekEventEditorState("", cancelsTalk = false), errorMessage = null) }
    }

    fun editEvent(event: WeekEvent) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val editor = WeekEventEditorState(event.name, event.cancelsTalk, event)
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    fun updateEditor(editor: WeekEventEditorState) {
        mutableUiState.update { if (it.isSaving) it else it.copy(editor = editor) }
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    fun save() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        if (state.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val country = state.country.selected ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveEvent(user, country, editor.event)
                .onSuccess { saved ->
                    val next = WeekEventEditorState(saved.name, saved.cancelsTalk, saved)
                    // Show the result at once; the snapshot flow can lag behind, especially on desktop.
                    mutableUiState.update {
                        val events = (it.events.filterNot { e -> e.id == saved.id } + saved)
                            .sortedBy { e -> e.name.lowercase() }
                        it.copy(events = events, editor = next, isSaving = false)
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Wochenereignis konnte nicht gespeichert werden.") }
                }
        }
    }

    fun delete(event: WeekEvent) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val country = mutableUiState.value.country.selected ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteEvent(user, country, event.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            events = state.events.filterNot { it.id == event.id },
                            editor = state.editor?.takeUnless { it.original?.id == event.id },
                            isSaving = false,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Wochenereignis konnte nicht gelöscht werden.") }
                }
        }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        if (currentUser?.let(policy::canManage) == true) observe()
    }

    private fun observe() {
        if (observeJob?.isActive == true) return
        val country = mutableUiState.value.country.selected ?: return
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeEvents(country)
                .catch { exception ->
                    val message = exception.message ?: "Wochenereignisse konnten nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { events ->
                    mutableUiState.update { it.copy(events = events, isLoading = false) }
                }
        }
    }

    private fun checkAllowed(): Boolean {
        val allowed = currentUser?.let(policy::canManage) == true
        if (!allowed) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return allowed
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}

private fun WeekEventUiState.failed(exception: Throwable, fallback: String) =
    copy(isSaving = false, errorMessage = exception.message ?: fallback)
