package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Pflege des Verzeichnisses externer Redner (Kreis → Versammlung → Redner) durch die Planer der Versammlung. Das
 * ViewModel beobachtet das Verzeichnis; Filter, Editor und Änderungen übernehmen die Zustandshalter (ADR-0025).
 */
@Inject
class ExternalDirectoryViewModel(
    private val observeDirectory: ObserveExternalDirectoryUseCase,
    changeUseCases: DirectoryChangeUseCases,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val store = UiStateStore(ExternalDirectoryUiState())
    val uiState: StateFlow<ExternalDirectoryUiState> = store.state

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    val filter = DirectoryFilterHolder(store)
    val editor = DirectoryEditorHolder(store)
    val changes = DirectoryChangesHolder(store, viewModelScope, changeUseCases) { currentUser }

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canView = policy.canViewTalkPlanning(user)
        val canPlan = policy.canPlanTalks(user)
        store.update {
            it.copy(
                canView = canView,
                canPlan = canPlan,
                editor = it.editor.takeIf { canView && (canPlan || it?.id != null) },
            )
        }
        if (canView) {
            observe(user)
        } else {
            observeJob?.cancel()
            observeJob = null
            store.update { it.copy(directory = ExternalDirectory(), isLoading = false) }
        }
    }

    fun dismissError() {
        store.update { it.copy(errorMessage = null) }
        // Nach einem Ladefehler erneut laden.
        currentUser?.takeIf(policy::canViewTalkPlanning)?.let(::observe)
    }

    private fun observe(user: AppUser) {
        if (observeJob?.isActive == true) return
        store.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeDirectory(user)
                .catch { exception ->
                    val message = exception.message ?: "Externe Redner konnten nicht geladen werden."
                    store.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { directory ->
                    store.update { it.copy(directory = directory, isLoading = false) }
                }
        }
    }
}
