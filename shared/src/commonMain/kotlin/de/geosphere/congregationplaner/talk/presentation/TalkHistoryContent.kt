package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/** Callbacks der Historie; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class TalkHistoryActions(
    val onSelect: (Talk) -> Unit = {},
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
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Vortragshistorie", style = MaterialTheme.typography.headlineSmall)
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
        val neverHeld = state.rows.count { it.entry == null }
        Text("$neverHeld von ${state.rows.size} Vorträgen wurden noch nie gehalten.")
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = { AnimatedPane { TalkHistoryList(state, actions) } },
            detailPane = {
                AnimatedPane {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden) {
                            TextButton(onClick = actions.onClose) { Text("Zurück zur Liste") }
                        }
                        TalkHistoryDetail(state.selected)
                    }
                }
            },
        )
    }
}

@Composable
private fun TalkHistoryList(state: TalkHistoryUiState, actions: TalkHistoryActions) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.rows.isEmpty()) item { Text("Noch keine Vorträge in der Liste.") }
        items(state.rows, key = { it.talk.number }) { row ->
            SelectableListCard(
                selected = state.selectedNumber == row.talk.number,
                onSelect = { actions.onSelect(row.talk) },
                onDelete = {},
                actionsEnabled = true,
                canDelete = false,
            ) {
                Text("${row.talk.number} – ${row.talk.title}", style = MaterialTheme.typography.titleMedium)
                val last = row.entry?.lastHeldOn
                val count = row.entry?.heldOn?.size
                Text(if (last == null) "noch nie gehalten" else "zuletzt ${last.toDisplayText()} ($count×)")
            }
        }
    }
}

/** Zeigt die Tage eines Vortrags; geändert werden sie nur in der Planung am Wochenende. */
@Composable
private fun TalkHistoryDetail(row: TalkHistoryRow?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (row == null) {
                Text(
                    "Wähle einen Vortrag aus, um zu sehen, wann er gehalten wurde.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }
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
