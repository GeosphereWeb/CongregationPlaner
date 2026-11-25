package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.usecase.BuildTalkHistoryOverviewUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SearchSpeakerHistoryUseCase
import kotlinx.datetime.LocalDate

/** Vorschau-Zustand mit den Werten, die sonst das ViewModel über die UseCases ableitet. */
@Suppress("LongParameterList")
internal fun previewState(
    talks: List<Talk>,
    history: List<TalkHistoryEntry>,
    isLoading: Boolean,
    canView: Boolean,
    selectedNumber: Int? = null,
    query: String = "",
    groupByFrequency: Boolean = false,
    showTiles: Boolean = false,
): TalkHistoryUiState {
    val search = SearchSpeakerHistoryUseCase()
    return TalkHistoryUiState(
        talks = talks,
        history = history,
        isLoading = isLoading,
        canView = canView,
        selectedNumber = selectedNumber,
        query = query,
        groupByFrequency = groupByFrequency,
        showTiles = showTiles,
        overview = BuildTalkHistoryOverviewUseCase()(talks, history),
        speakerResults = search(talks, history, query),
        isSearching = search.isActive(query),
    )
}

/** Fester „heute“-Tag der Vorschauen, damit die Farben stabil bleiben. */
internal val previewToday = LocalDate(2026, 11, 25)
