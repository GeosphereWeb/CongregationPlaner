package de.geosphere.congregationplaner.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkHistoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ein Vortrag der Vortragsliste mit seiner Historie; [entry] ist `null`, wenn er noch nie gehalten wurde. */
data class TalkHistoryRow(val talk: Talk, val entry: TalkHistoryEntry?)

data class TalkHistoryUiState(
    val talks: List<Talk> = emptyList(),
    val history: List<TalkHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val canPlan: Boolean = false,
    val selectedNumber: Int? = null,
    override val errorMessage: String? = null,
) : ListDetailState {
    override val hasDetail: Boolean get() = selected != null

    /** Die Historie wird nur angezeigt; sie ändert sich ausschließlich durch die Planung. */
    override val isSaving: Boolean get() = false

    val rows: List<TalkHistoryRow>
        get() {
            val byNumber = history.associateBy { it.talkNumber }
            return talks.map { TalkHistoryRow(it, byNumber[it.number]) }
        }

    val selected: TalkHistoryRow? get() = rows.firstOrNull { it.talk.number == selectedNumber }
}

/** Übersicht, wann welcher Vortrag in der Versammlung gehalten wurde (nur lesend). */
@Inject
class TalkHistoryViewModel(
    private val observeCatalog: ObserveTalkCatalogUseCase,
    private val observeHistory: ObserveTalkHistoryUseCase,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TalkHistoryUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canPlan = policy.canPlanTalks(user)
        mutableUiState.update { it.copy(canPlan = canPlan, selectedNumber = it.selectedNumber.takeIf { canPlan }) }
        if (canPlan) {
            observe(user)
        } else {
            observeJob?.cancel()
            observeJob = null
            mutableUiState.update { it.copy(talks = emptyList(), history = emptyList(), isLoading = false) }
        }
    }

    fun selectTalk(talk: Talk) {
        mutableUiState.update { it.copy(selectedNumber = talk.number) }
    }

    fun closeDetail() {
        mutableUiState.update { it.copy(selectedNumber = null) }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        currentUser?.takeIf(policy::canPlanTalks)?.let(::observe)
    }

    private fun observe(user: AppUser) {
        if (observeJob?.isActive == true) return
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            combine(observeCatalog(), observeHistory(user)) { talks, history -> talks to history }
                .catch { exception ->
                    val message = exception.message ?: "Vortragshistorie konnte nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { (talks, history) ->
                    mutableUiState.update { it.copy(talks = talks, history = history, isLoading = false) }
                }
        }
    }
}
