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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Callbacks der Vortragsplanung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class TalkScheduleActions(
    val onYearChange: (Int) -> Unit = {},
    val onNewSlot: () -> Unit = {},
    val onEditSlot: (TalkSlot) -> Unit = {},
    val onEditorChange: (TalkSlot) -> Unit = {},
    val onSelectWeekEvent: (WeekEvent?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (TalkSlot) -> Unit = {},
    val onCancelEditing: () -> Unit = {},
    val onDiscardChanges: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

@Composable
fun TalkScheduleScreen(viewModel: TalkScheduleViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    TalkScheduleContent(
        state = state,
        modifier = modifier,
        actions = TalkScheduleActions(
            onYearChange = viewModel::changeYear,
            onNewSlot = viewModel::newSlot,
            onEditSlot = viewModel::editSlot,
            onEditorChange = viewModel::updateEditor,
            onSelectWeekEvent = viewModel::selectWeekEvent,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onCancelEditing = viewModel::cancelEditing,
            onDiscardChanges = viewModel::discardChanges,
            onDismissError = viewModel::dismissError,
        ),
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
internal fun TalkScheduleContent(
    state: TalkScheduleUiState,
    actions: TalkScheduleActions,
    modifier: Modifier = Modifier,
) {
    val editor = state.editor
    val navigator = rememberListDetailNavigation(state, onClose = actions.onCancelEditing)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) actions.onCancelEditing else actions.onDiscardChanges

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Planung Wochenende", style = MaterialTheme.typography.headlineSmall)
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

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { actions.onYearChange(state.year - 1) }) { Text("‹") }
            Text(state.year.toString(), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { actions.onYearChange(state.year + 1) }) { Text("›") }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            OutlinedButton(onClick = actions.onNewSlot, enabled = !state.isSaving) {
                                Text("Termin anlegen")
                            }
                        }
                        if (state.slots.isEmpty()) {
                            item { Text("Für ${state.year} sind noch keine Termine angelegt.") }
                        }
                        items(state.slots, key = { it.id }) { slot ->
                            SelectableListCard(
                                selected = editor?.original?.date == slot.date,
                                onSelect = { actions.onEditSlot(slot) },
                                onDelete = { actions.onDelete(slot) },
                                actionsEnabled = !state.isSaving,
                                canDelete = true,
                            ) {
                                SlotSummary(slot)
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden) {
                            TextButton(onClick = actions.onCancelEditing, enabled = !state.isSaving) {
                                Text("Zurück zur Liste")
                            }
                        }
                        if (editor != null) {
                            TalkSlotEditor(
                                editor = editor,
                                weekEvents = state.weekEvents,
                                isSaving = state.isSaving,
                                onChange = actions.onEditorChange,
                                onSelectWeekEvent = actions.onSelectWeekEvent,
                                onSave = actions.onSave,
                                onCancel = onCancelEditing,
                            )
                        } else {
                            Text("Wähle einen Termin zum Bearbeiten aus.")
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SlotSummary(slot: TalkSlot) {
    Text("${slot.date.toDisplayText()} · ${slot.time}", style = MaterialTheme.typography.titleMedium)
    slot.weekEventName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    if (slot.talkCancelled) Text("Vortrag entfällt", color = MaterialTheme.colorScheme.error)
}

@ThemePreviews
@Composable
private fun TalkScheduleContentPreview() = PreviewThemeWrapper {
    TalkScheduleContent(
        state = TalkScheduleUiState(
            year = 2026,
            slots = listOf(
                TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30)),
                TalkSlot(
                    LocalDate(2026, 11, 1),
                    LocalTime(17, 30),
                    weekEventId = "kongress",
                    weekEventName = "Kongress",
                    talkCancelled = true,
                ),
            ),
            isLoading = false,
            canPlan = true,
        ),
        actions = TalkScheduleActions(),
    )
}
