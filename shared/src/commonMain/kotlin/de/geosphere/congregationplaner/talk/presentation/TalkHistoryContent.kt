package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.CollapsingHeader
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.DetailPlaceholder
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.presentation.components.collapsingHeaderScroll
import de.geosphere.congregationplaner.presentation.components.rememberCollapsingHeaderState
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.field.AppSwitch
import kotlinx.datetime.LocalDate

/** Callbacks der Historie; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class TalkHistoryActions(
    val onSelect: (Talk) -> Unit = {},
    val onSelectNumber: (Int) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onGroupByFrequency: (Boolean) -> Unit = {},
    val onClose: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

@Composable
fun TalkHistoryScreen(viewModel: TalkHistoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    TalkHistoryContent(
        state = state,
        actions = TalkHistoryActions(
            onSelect = viewModel::selectTalk,
            onSelectNumber = viewModel::selectTalkNumber,
            onSearch = viewModel::search,
            onGroupByFrequency = viewModel::setGroupByFrequency,
            onClose = viewModel::closeDetail,
            onDismissError = viewModel::dismissError,
        ),
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun TalkHistoryContent(state: TalkHistoryUiState, actions: TalkHistoryActions, modifier: Modifier = Modifier) {
    val navigator = rememberListDetailNavigation(state, onClose = actions.onClose)
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState(listState)
    Column(
        modifier = modifier.fillMaxSize().collapsingHeaderScroll(header).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListPaneHeader("Vortragshistorie")
        if (!state.canPlan) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        state.errorMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onDismissError) { Text("OK") }
            }
        }
        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        CollapsingHeader(visible = header.visible || state.isSearching) { TalkHistoryHeader(state, actions) }
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = { AnimatedPane { TalkHistoryList(state, actions, listState) } },
            detailPane = {
                AnimatedPane {
                    DetailPane(
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = actions.onClose,
                    ) {
                        TalkHistoryDetail(state.selected)
                    }
                }
            },
        )
    }
}

@Composable
private fun TalkHistoryHeader(state: TalkHistoryUiState, actions: TalkHistoryActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val neverHeld = state.rows.count { it.entry == null }
        Text("$neverHeld von ${state.rows.size} Vorträgen wurden noch nie gehalten.")
        OutlinedTextField(
            value = state.query,
            onValueChange = actions.onSearch,
            label = { Text("Redner suchen") },
            supportingText = { Text("Ab dem zweiten Buchstaben: zeigt seine Vorträge und wann er sie gehalten hat") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSwitch(checked = state.groupByFrequency, onCheckedChange = actions.onGroupByFrequency)
            Text("Nach Häufigkeit gruppieren")
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TalkHistoryList(state: TalkHistoryUiState, actions: TalkHistoryActions, listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isSearching) {
            speakerResults(state, actions)
            return@LazyColumn
        }
        if (state.rows.isEmpty()) item { Text("Noch keine Vorträge in der Liste.") }
        if (!state.groupByFrequency) {
            items(state.rows, key = { it.talk.number }) { row -> TalkRowCard(row, state, actions) }
            return@LazyColumn
        }
        state.groups.forEach { group ->
            stickyHeader(key = "group-${group.count}") {
                Text(
                    "${group.title} (${group.rows.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                )
            }
            items(group.rows, key = { it.talk.number }) { row -> TalkRowCard(row, state, actions) }
        }
    }
}

@Composable
private fun TalkRowCard(row: TalkHistoryRow, state: TalkHistoryUiState, actions: TalkHistoryActions) {
    SelectableListCard(
        selected = state.selectedNumber == row.talk.number,
        onSelect = { actions.onSelect(row.talk) },
        onDelete = {},
        actionsEnabled = true,
        canDelete = false,
    ) {
        Text("${row.talk.number} – ${row.talk.title}", style = MaterialTheme.typography.titleMedium)
        val last = row.entry?.lastHeldOn
        Text(if (last == null) "noch nie gehalten" else "zuletzt ${last.toDisplayText()}")
    }
}

/** Je gefundenem Redner seine Vorträge mit den Tagen; ein Tipp öffnet den Vortrag. */
private fun LazyListScope.speakerResults(state: TalkHistoryUiState, actions: TalkHistoryActions) {
    val results = state.speakerResults
    if (results.isEmpty()) item { Text("Kein Redner gefunden.") }
    results.forEach { speaker ->
        item(key = "speaker-${speaker.speaker}") {
            Text(speaker.speaker, style = MaterialTheme.typography.titleSmall)
        }
        items(speaker.talks, key = { "${speaker.speaker}-${it.talkNumber}" }) { held ->
            SelectableListCard(
                selected = state.selectedNumber == held.talkNumber,
                onSelect = { actions.onSelectNumber(held.talkNumber) },
                onDelete = {},
                actionsEnabled = true,
                canDelete = false,
            ) {
                Text(
                    "${held.talkNumber} – ${held.talk?.title.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(held.days.joinToString(", ") { it.toDisplayText() })
            }
        }
    }
}

/** Zeigt die Tage eines Vortrags; geändert werden sie nur in der Planung am Wochenende. */
@Composable
private fun TalkHistoryDetail(row: TalkHistoryRow?) {
    if (row == null) {
        DetailPlaceholder("Wähle einen Vortrag aus, um zu sehen, wann er gehalten wurde.")
        return
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${row.talk.number} – ${row.talk.title}", style = MaterialTheme.typography.titleMedium)
            val days = row.entry?.heldOn.orEmpty()
            if (days.isEmpty()) Text("Noch nie gehalten.")
            days.reversed().forEach { day ->
                val speaker = row.entry?.speakers?.get(day)
                Text(if (speaker == null) day.toDisplayText() else "${day.toDisplayText()} – $speaker")
            }
            Text("Die Tage ändern sich nur in der Planung am Wochenende.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@ThemePreviews
@Composable
private fun TalkHistoryContentPreview() = PreviewThemeWrapper {
    val talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Noch nie gehalten"))
    TalkHistoryContent(
        state = TalkHistoryUiState(
            talks = talks,
            history = listOf(
                TalkHistoryEntry(
                    1,
                    listOf(LocalDate(2026, 3, 1), LocalDate(2026, 10, 25)),
                    speakers = mapOf(LocalDate(2026, 10, 25) to "Muster, Max"),
                ),
            ),
            isLoading = false,
            canPlan = true,
            selectedNumber = 1,
        ),
        actions = TalkHistoryActions(),
    )
}

@ThemePreviews
@Composable
private fun TalkHistorySearchPreview() = PreviewThemeWrapper {
    val talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Zweiter Vortrag"), Talk(3, "Noch nie gehalten"))
    TalkHistoryContent(
        state = TalkHistoryUiState(
            talks = talks,
            history = listOf(
                TalkHistoryEntry(
                    1,
                    listOf(LocalDate(2026, 3, 1), LocalDate(2026, 10, 25)),
                    speakers = mapOf(
                        LocalDate(2026, 3, 1) to "Beispiel, Tim (Nord)",
                        LocalDate(2026, 10, 25) to "Muster, Max",
                    ),
                ),
                TalkHistoryEntry(
                    2,
                    listOf(LocalDate(2026, 5, 3)),
                    speakers = mapOf(
                        LocalDate(2026, 5, 3) to "Muster, Max",
                    ),
                ),
            ),
            isLoading = false,
            canPlan = true,
            query = "mus",
        ),
        actions = TalkHistoryActions(),
    )
}

@ThemePreviews
@Composable
private fun TalkHistoryGroupedPreview() = PreviewThemeWrapper {
    val talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Zweiter Vortrag"), Talk(3, "Noch nie gehalten"))
    TalkHistoryContent(
        state = TalkHistoryUiState(
            talks = talks,
            history = listOf(TalkHistoryEntry(2, listOf(LocalDate(2026, 5, 3)))),
            isLoading = false,
            canPlan = true,
            groupByFrequency = true,
        ),
        actions = TalkHistoryActions(),
    )
}
