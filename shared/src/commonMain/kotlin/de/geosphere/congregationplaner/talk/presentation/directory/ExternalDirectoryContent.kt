package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.presentation.picker.label
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.list.CollapsingHeaderColumn

/** Callbacks der Verwaltung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class ExternalDirectoryActions(
    val onNewCircuit: () -> Unit = {},
    val onNewCongregation: (circuitId: String) -> Unit = {},
    val onNewSpeaker: (congregationId: String) -> Unit = {},
    val onSelectCircuit: (ExternalCircuit) -> Unit = {},
    val onSelectCongregation: (ExternalCongregation) -> Unit = {},
    val onSelectSpeaker: (VisitingSpeaker) -> Unit = {},
    val onEditorChange: (ExternalDirectoryEditor) -> Unit = {},
    val onSave: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onDeleteCircuit: (ExternalCircuit) -> Unit = {},
    val onDeleteCongregation: (ExternalCongregation) -> Unit = {},
    val onDeleteSpeaker: (VisitingSpeaker) -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onToggleCircuit: (String) -> Unit = {},
    val onClearCircuits: () -> Unit = {},
    val onToggleCongregation: (String) -> Unit = {},
    val onClearCongregations: () -> Unit = {},
    val onSearchCongregations: (String) -> Unit = {},
)

@Composable
fun ExternalDirectoryScreen(viewModel: ExternalDirectoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ExternalDirectoryContent(
        state = state,
        actions = ExternalDirectoryActions(
            onNewCircuit = viewModel.editor::newCircuit,
            onNewCongregation = viewModel.editor::newCongregation,
            onNewSpeaker = viewModel.editor::newSpeaker,
            onSelectCircuit = viewModel.editor::select,
            onSelectCongregation = viewModel.editor::select,
            onSelectSpeaker = viewModel.editor::select,
            onEditorChange = viewModel.editor::update,
            onSave = viewModel.changes::save,
            onClose = viewModel.editor::close,
            onDiscard = viewModel.editor::discard,
            onDeleteCircuit = viewModel.changes::delete,
            onDeleteCongregation = viewModel.changes::delete,
            onDeleteSpeaker = viewModel.changes::delete,
            onDismissError = viewModel::dismissError,
            onToggleCircuit = viewModel.filter::toggleCircuit,
            onClearCircuits = viewModel.filter::clearCircuits,
            onToggleCongregation = viewModel.filter::toggleCongregation,
            onClearCongregations = viewModel.filter::clearCongregations,
            onSearchCongregations = viewModel.filter::searchCongregations,
        ),
        modifier = modifier,
    )
}

private fun ExternalDirectoryActions.add(action: AddAction) = when (action.target) {
    AddTarget.CIRCUIT -> onNewCircuit()
    AddTarget.CONGREGATION -> onNewCongregation(action.parentId)
    AddTarget.SPEAKER -> onNewSpeaker(action.parentId)
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun ExternalDirectoryContent(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    modifier: Modifier = Modifier,
) {
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    val isBusy = state.isSaving || state.isDeleting
    val navigator = rememberListDetailNavigation(state, onClose = actions.onClose)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val listState = rememberLazyListState()

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ListPaneHeader("Externe Redner")
            if (!state.canView) {
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
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                listPane = {
                    AnimatedPane {
                        CollapsingHeaderColumn(
                            listState = listState,
                            header = { DirectoryFilterBar(state, actions) },
                            spacing = 8.dp,
                        ) {
                            ExternalDirectoryTree(state, actions, isBusy, listState)
                        }
                    }
                },
                detailPane = {
                    AnimatedPane {
                        DetailPane(
                            showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] ==
                                PaneAdaptedValue.Hidden,
                            onBack = actions.onClose,
                            backEnabled = !isBusy,
                        ) {
                            ExternalDirectoryDetail(
                                state = state,
                                actions = actions.copy(
                                    onDiscard = if (singlePane) actions.onClose else actions.onDiscard,
                                ),
                                isBusy = isBusy,
                                onRequestDelete = { deleteRequest = it },
                            )
                        }
                    }
                },
            )
        }
        val addAction = state.addAction
        // On a phone the detail covers the list, so the FAB only stays there for the add action of a saved entry.
        val coveredByDetail = singlePane && state.hasDetail && addAction.target == AddTarget.CIRCUIT
        if (state.canPlan && !coveredByDetail) {
            FloatingActionButton(
                onClick = { if (!state.isLoading && !isBusy) actions.add(addAction) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .semantics { contentDescription = addAction.target.label },
            ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        }
    }

    deleteRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { if (!isBusy) deleteRequest = null },
            title = { Text("Löschen?") },
            text = { Text(request.text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        request.confirm()
                        deleteRequest = null
                    },
                    enabled = !isBusy,
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text("Abbrechen") } },
        )
    }
}

@ThemePreviews
@Composable
private fun ExternalDirectoryContentPreview() = PreviewThemeWrapper {
    ExternalDirectoryContent(state = previewDirectoryState, actions = ExternalDirectoryActions())
}

@ThemePreviews
@Composable
private fun ExternalDirectoryFilteredPreview() = PreviewThemeWrapper {
    val north = ExternalCircuit("c1", "Kreis Nord")
    val south = ExternalCircuit("c2", "Kreis Süd")
    val musterstadt = ExternalCongregation("g1", "c1", "Musterstadt")
    val zell = ExternalCongregation("g2", "c2", "Zell")
    val tim = VisitingSpeaker(id = "s1", congregationId = "g1", lastName = "Beispiel", firstName = "Tim")
    val eva = VisitingSpeaker(id = "s2", congregationId = "g2", lastName = "Muster", firstName = "Eva")
    val directory = ExternalDirectory(listOf(north, south), listOf(musterstadt, zell), listOf(tim, eva))
    Column {
        // Nur die Versammlung gewählt: der Kreis steht auf „Alle“, die Liste zeigt nur Zell.
        ExternalDirectoryContent(
            state = ExternalDirectoryUiState(
                directory = directory,
                filter = DirectoryFilter(congregationIds = setOf("g2")),
                congregationQuery = "ze",
                isLoading = false,
                canView = true,
                canPlan = true,
            ),
            actions = ExternalDirectoryActions(),
        )
    }
}
