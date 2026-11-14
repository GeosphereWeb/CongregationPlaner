package de.geosphere.congregationplaner.event.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.event.domain.usecase.DeleteSpecialEventUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveManagedEventsUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveSpecialEventUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Pflege der besonderen Ereignisse durch den Versammlungsadmin. */
@Inject
class SpecialEventManagementViewModel(
    private val observeEvents: ObserveManagedEventsUseCase,
    private val saveEvent: SaveSpecialEventUseCase,
    private val deleteEvent: DeleteSpecialEventUseCase,
    private val policy: SpecialEventPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SpecialEventUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        val changed = user.uid != currentUser?.uid || user.congregation != currentUser?.congregation
        currentUser = user
        val canManage = policy.canManage(user)
        mutableUiState.update { it.copy(canManage = canManage, editor = it.editor?.takeIf { canManage }) }
        if (changed || observeJob == null) observe(user)
    }

    fun newEvent(today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val editor = SpecialEventEditorState(SpecialEvent(startDate = today))
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    fun editEvent(event: SpecialEvent) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        mutableUiState.update { it.copy(editor = SpecialEventEditorState(event, event), errorMessage = null) }
    }

    fun updateEditor(editor: SpecialEventEditorState) {
        mutableUiState.update { if (it.isSaving) it else it.copy(editor = editor) }
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    fun save() {
        val state = mutableUiState.value
        if (state.isSaving || !checkAllowed()) return
        val editor = state.editor
        val user = currentUser
        if (editor == null || user == null) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveEvent(user, editor.event)
                .onSuccess { saved ->
                    // Show the result at once; the snapshot flow can lag behind, especially on desktop.
                    mutableUiState.update {
                        val events = (
                            it.events.filterNot { e ->
                                e.id == saved.id
                            } + saved
                            ).sortedBy { e -> e.startDate }
                        it.copy(events = events, editor = SpecialEventEditorState(saved, saved), isSaving = false)
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Ereignis konnte nicht gespeichert werden.") }
                }
        }
    }

    fun delete(event: SpecialEvent) {
        val user = currentUser ?: return
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteEvent(user, event.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            events = state.events.filterNot { it.id == event.id },
                            editor = state.editor?.takeUnless { it.original?.id == event.id },
                            isSaving = false,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Ereignis konnte nicht gelöscht werden.") }
                }
        }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        if (observeJob?.isActive != true) currentUser?.let(::observe)
    }

    private fun observe(user: AppUser) {
        observeJob?.cancel()
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeEvents(user)
                .catch { exception ->
                    val message = exception.message ?: "Ereignisse konnten nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { events ->
                    mutableUiState.update { it.copy(events = events, isLoading = false) }
                }
        }
    }

    private fun checkAllowed(): Boolean {
        val allowed = currentUser?.let(policy::canManage) == true
        if (!allowed) mutableUiState.update { it.copy(errorMessage = "Dafür fehlt dir die Berechtigung.") }
        return allowed
    }
}

private fun SpecialEventUiState.failed(exception: Throwable, fallback: String) =
    copy(isSaving = false, errorMessage = exception.message ?: fallback)
