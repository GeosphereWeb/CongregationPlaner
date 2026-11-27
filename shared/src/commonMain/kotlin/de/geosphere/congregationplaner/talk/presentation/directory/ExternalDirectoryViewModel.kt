package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakerVisitsUseCase
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
    private val observeVisits: ObserveExternalSpeakerVisitsUseCase,
    changeUseCases: DirectoryChangeUseCases,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val store = UiStateStore(ExternalDirectoryUiState())
    val uiState: StateFlow<ExternalDirectoryUiState> = store.state

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null
    private var visitsJob: Job? = null

    /** Der Redner, der geöffnet wird, sobald das Verzeichnis geladen ist (Sprung aus der Planung). */
    private var pendingSpeakerId: String? = null

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
            visitsJob?.cancel()
            visitsJob = null
            store.update { it.copy(directory = ExternalDirectory(), visits = null, isLoading = false) }
        }
    }

    /**
     * Öffnet den Redner [speakerId], sobald das Verzeichnis geladen ist. Filter und Suche werden zurückgesetzt, damit
     * er in der Liste steht; einen Redner, den es nicht mehr gibt, ignoriert die Liste.
     */
    fun openSpeaker(speakerId: String) {
        pendingSpeakerId = speakerId
        openPendingSpeaker()
    }

    private fun openPendingSpeaker() {
        val id = pendingSpeakerId ?: return
        if (store.value.isLoading) return
        pendingSpeakerId = null
        val speaker = store.value.directory.speakers.firstOrNull { it.id == id } ?: return
        store.update { it.copy(filter = DirectoryFilter(), congregationQuery = "", speakerQuery = "") }
        editor.select(speaker)
    }

    /** Schaltet zwischen Baum und flacher Liste nach letztem Besuch um. */
    fun setSortByLastVisit(enabled: Boolean) {
        store.update { it.copy(sortByLastVisit = enabled) }
    }

    fun dismissError() {
        store.update { it.copy(errorMessage = null) }
        // Nach einem Ladefehler erneut laden.
        currentUser?.takeIf(policy::canViewTalkPlanning)?.let(::observe)
    }

    private fun observe(user: AppUser) {
        observeVisits(user)
        if (observeJob?.isActive == true) return
        store.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeDirectory(user)
                .catch { exception ->
                    val message = exception.message ?: "Externe Redner konnten nicht geladen werden."
                    store.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { directory ->
                    store.update { it.copy(directory = directory, isLoading = false) }
                    openPendingSpeaker()
                }
        }
    }

    /** Ohne Besuche (z. B. ohne Leserecht auf die Planung) bleibt die Liste ohne Farben; das ist kein Fehler. */
    private fun observeVisits(user: AppUser) {
        if (visitsJob?.isActive == true) return
        visitsJob = viewModelScope.launch {
            observeVisits.invoke(user)
                .catch { store.update { it.copy(visits = null) } }
                .collect { visits -> store.update { it.copy(visits = visits) } }
        }
    }
}
