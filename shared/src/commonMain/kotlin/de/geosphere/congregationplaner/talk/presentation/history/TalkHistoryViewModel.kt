package de.geosphere.congregationplaner.talk.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.BuildTalkHistoryOverviewUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.FrequencyGroup
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkHistoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SearchSpeakerHistoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SpeakerHistory
import de.geosphere.congregationplaner.talk.domain.usecase.TalkHistoryOverview
import de.geosphere.congregationplaner.talk.domain.usecase.TalkHistoryRow
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die Historie der Vorträge; [overview], [speakerResults] und [isSearching] leitet das ViewModel über die UseCases
 * aus Vortragsliste, Historie und Suchtext ab.
 */
data class TalkHistoryUiState(
    val talks: List<Talk> = emptyList(),
    val history: List<TalkHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val canView: Boolean = false,
    val selectedNumber: Int? = null,
    val query: String = "",
    val groupByFrequency: Boolean = false,
    val overview: TalkHistoryOverview = TalkHistoryOverview(),
    /** Ergebnis der Rednersuche; leer, solange die Suche nicht aktiv ist. */
    val speakerResults: List<SpeakerHistory> = emptyList(),
    val isSearching: Boolean = false,
    override val errorMessage: String? = null,
) : ListDetailState {
    override val hasDetail: Boolean get() = selected != null

    /** Die Historie wird nur angezeigt; sie ändert sich ausschließlich durch die Planung. */
    override val isSaving: Boolean get() = false

    val rows: List<TalkHistoryRow> get() = overview.rows
    val groups: List<FrequencyGroup> get() = overview.groups
    val neverHeldCount: Int get() = overview.neverHeldCount

    val selected: TalkHistoryRow? get() = rows.firstOrNull { it.talk.number == selectedNumber }
}

/** Übersicht, wann welcher Vortrag in der Versammlung gehalten wurde (nur lesend). */
@Inject
class TalkHistoryViewModel(
    private val observeCatalog: ObserveTalkCatalogUseCase,
    private val observeHistory: ObserveTalkHistoryUseCase,
    private val policy: TalkSchedulePermissionPolicy,
    private val buildOverview: BuildTalkHistoryOverviewUseCase,
    private val searchSpeakers: SearchSpeakerHistoryUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TalkHistoryUiState())
    val uiState = mutableUiState.asStateFlow()

    /** Ändert den Zustand und leitet Übersicht und Suchergebnis über die UseCases neu ab. */
    private fun update(transform: (TalkHistoryUiState) -> TalkHistoryUiState) = mutableUiState.update { current ->
        val next = transform(current)
        next.copy(
            overview = buildOverview(next.talks, next.history),
            speakerResults = searchSpeakers(next.talks, next.history, next.query),
            isSearching = searchSpeakers.isActive(next.query),
        )
    }

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canView = policy.canViewTalkPlanning(user)
        update { it.copy(canView = canView, selectedNumber = it.selectedNumber.takeIf { canView }) }
        if (canView) {
            observe(user)
        } else {
            observeJob?.cancel()
            observeJob = null
            update { it.copy(talks = emptyList(), history = emptyList(), isLoading = false) }
        }
    }

    fun selectTalk(talk: Talk) {
        update { it.copy(selectedNumber = talk.number) }
    }

    /** Wählt einen Vortrag über seine Nummer, z. B. beim Wechsel aus der Planung am Wochenende. */
    fun selectTalkNumber(number: Int) {
        update { it.copy(selectedNumber = number) }
    }

    /** Sucht Redner nach Namen (ab dem zweiten Buchstaben); die Liste ändert sich sofort. */
    fun search(query: String) {
        update { it.copy(query = query) }
    }

    /** Schaltet zwischen der Liste nach Nummer und der nach Häufigkeit gruppierten Darstellung um. */
    fun setGroupByFrequency(enabled: Boolean) {
        update { it.copy(groupByFrequency = enabled) }
    }

    fun closeDetail() {
        update { it.copy(selectedNumber = null) }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        update { it.copy(errorMessage = null) }
        currentUser?.takeIf(policy::canViewTalkPlanning)?.let(::observe)
    }

    private fun observe(user: AppUser) {
        if (observeJob?.isActive == true) return
        update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            combine(observeCatalog(), observeHistory(user)) { talks, history -> talks to history }
                .catch { exception ->
                    val message = exception.message ?: "Vortragshistorie konnte nicht geladen werden."
                    update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { (talks, history) ->
                    update { it.copy(talks = talks, history = history, isLoading = false) }
                }
        }
    }
}
