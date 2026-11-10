package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.DetailPlaceholder
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
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
            onNewCircuit = viewModel::newCircuit,
            onNewCongregation = viewModel::newCongregation,
            onNewSpeaker = viewModel::newSpeaker,
            onSelectCircuit = viewModel::select,
            onSelectCongregation = viewModel::select,
            onSelectSpeaker = viewModel::select,
            onEditorChange = viewModel::updateEditor,
            onSave = viewModel::save,
            onClose = viewModel::closeDetail,
            onDiscard = viewModel::discardChanges,
            onDeleteCircuit = viewModel::delete,
            onDeleteCongregation = viewModel::delete,
            onDeleteSpeaker = viewModel::delete,
            onDismissError = viewModel::dismissError,
            onToggleCircuit = viewModel::toggleCircuitFilter,
            onClearCircuits = viewModel::clearCircuitFilter,
            onToggleCongregation = viewModel::toggleCongregationFilter,
            onClearCongregations = viewModel::clearCongregationFilter,
            onSearchCongregations = viewModel::searchCongregations,
        ),
        modifier = modifier,
    )
}

private fun ExternalDirectoryActions.add(action: AddAction) = when (action.target) {
    AddTarget.CIRCUIT -> onNewCircuit()
    AddTarget.CONGREGATION -> onNewCongregation(action.parentId)
    AddTarget.SPEAKER -> onNewSpeaker(action.parentId)
}

/** Was nach der Rückfrage gelöscht wird. */
private class DeleteRequest(val text: String, val confirm: () -> Unit)

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

@Composable
private fun ExternalDirectoryTree(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    listState: LazyListState,
) {
    val directory = state.visibleDirectory
    val editor = state.editor
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (directory.circuits.isEmpty()) {
            item {
                Text(if (state.filter.isActive) "Keine Einträge für diesen Filter." else "Noch keine Kreise angelegt.")
            }
        }
        directory.circuits.forEach { circuit ->
            stickyHeader(key = "circuit-${circuit.id}") {
                SelectableListCard(
                    selected = editor is ExternalDirectoryEditor.Circuit && editor.id == circuit.id,
                    onSelect = { actions.onSelectCircuit(circuit) },
                    onDelete = {},
                    actionsEnabled = !isBusy,
                    canDelete = false,
                    accent = NightColors.Lavender,
                ) {
                    Text("Kreis ${circuit.name}", style = MaterialTheme.typography.titleMedium)
                    Text(congregationCountLabel(directory.congregations.count { it.circuitId == circuit.id }))
                }
            }
            directory.congregations.filter { it.circuitId == circuit.id }.forEach { congregation ->
                stickyHeader(key = "congregation-${congregation.id}") {
                    SelectableListCard(
                        selected = editor is ExternalDirectoryEditor.Congregation && editor.id == congregation.id,
                        onSelect = { actions.onSelectCongregation(congregation) },
                        onDelete = {},
                        actionsEnabled = !isBusy,
                        canDelete = false,
                        accent = NightColors.Sky,
                    ) {
                        Text("Versammlung ${congregation.name}", style = MaterialTheme.typography.titleSmall)
                        Text("${directory.speakers.count { it.congregationId == congregation.id }} Redner")
                    }
                }
                directory.speakers.filter { it.congregationId == congregation.id }.forEach { speaker ->
                    item(key = "speaker-${speaker.id}") {
                        SelectableListCard(
                            selected = editor is ExternalDirectoryEditor.Speaker && editor.id == speaker.id,
                            onSelect = { actions.onSelectSpeaker(speaker) },
                            onDelete = {},
                            actionsEnabled = !isBusy,
                            canDelete = false,
                            accent = NightColors.Mint,
                        ) {
                            Text(speaker.displayName())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter nach Kreisen und Versammlungen (je mehrere wählbar); jede Auswahl gilt sofort. Ohne Kreis stehen alle
 * Versammlungen zur Wahl; die Suche grenzt sie ab dem zweiten Buchstaben ein.
 */
@Composable
private fun DirectoryFilterBar(state: ExternalDirectoryUiState, actions: ExternalDirectoryActions) {
    val directory = state.directory
    if (directory.circuits.isEmpty()) return
    val circuits = directory.circuitOptions(state.filter)
    val congregations = directory.congregationOptions(state.filter, state.congregationQuery)
    val chosenCongregations = directory.congregationOptions(state.filter, "").filter { it.selected }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterMenu(
            title = "Kreis",
            summary = filterSummary(circuits.filter { it.selected }.map { it.label }, "Alle Kreise", "Kreise"),
            options = circuits,
            actions = FilterMenuActions(actions.onToggleCircuit, actions.onClearCircuits, "Alle Kreise"),
            modifier = Modifier.weight(1f),
        )
        FilterMenu(
            title = "Versammlung",
            summary = filterSummary(chosenCongregations.map { it.label }, "Alle Versammlungen", "Versammlungen"),
            options = congregations,
            actions = FilterMenuActions(
                actions.onToggleCongregation,
                actions.onClearCongregations,
                "Alle Versammlungen",
            ),
            query = state.congregationQuery,
            onQueryChange = actions.onSearchCongregations,
            modifier = Modifier.weight(1f),
        )
    }
}

private class FilterMenuActions(val onToggle: (String) -> Unit, val onClear: () -> Unit, val clearLabel: String)

/** Auswahlfeld mit Häkchen je Eintrag; das Menü bleibt offen, damit man mehrere wählen kann. */
@Composable
private fun FilterMenu(
    title: String,
    summary: String,
    options: List<FilterOption>,
    actions: FilterMenuActions,
    modifier: Modifier = Modifier,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = {
            expanded = true
        }, modifier = Modifier.fillMaxWidth()) { Text(summary, maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (query != null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Name suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            DropdownMenuItem(text = { Text(actions.clearLabel) }, onClick = actions.onClear)
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Checkbox(checked = option.selected, onCheckedChange = null) },
                    onClick = { actions.onToggle(option.id) },
                )
            }
        }
    }
}

internal fun congregationCountLabel(count: Int): String = if (count == 1) "1 Versammlung" else "$count Versammlungen"

private fun VisitingSpeaker.displayName(): String =
    listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")

@Composable
private fun ExternalDirectoryDetail(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    onRequestDelete: (DeleteRequest) -> Unit,
) {
    val directory = state.directory
    val editor = state.editor
    if (editor != null && !state.canPlan) {
        ReadOnlyCard(readOnlyView(editor, directory))
        return
    }
    when (editor) {
        null -> DetailPlaceholder("Wähle einen Kreis, eine Versammlung oder einen Redner aus.")

        is ExternalDirectoryEditor.Circuit -> EditorCard(
            title = if (editor.original == null) "Neuer Kreis" else "Kreis bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { circuit ->
                {
                    onRequestDelete(
                        DeleteRequest("Kreis „${circuit.name}“ wird gelöscht.") { actions.onDeleteCircuit(circuit) },
                    )
                }
            },
        ) {
            NameField("Name des Kreises", editor.circuit.name) {
                actions.onEditorChange(editor.copy(circuit = editor.circuit.copy(name = it)))
            }
        }

        is ExternalDirectoryEditor.Congregation -> EditorCard(
            title = if (editor.original == null) "Neue Versammlung" else "Versammlung bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { congregation ->
                {
                    onRequestDelete(
                        DeleteRequest("Versammlung „${congregation.name}“ wird gelöscht.") {
                            actions.onDeleteCongregation(congregation)
                        },
                    )
                }
            },
        ) {
            Text("Kreis: ${directory.circuitName(editor.congregation.circuitId)}")
            NameField("Name der Versammlung", editor.congregation.name) {
                actions.onEditorChange(editor.copy(congregation = editor.congregation.copy(name = it)))
            }
        }

        is ExternalDirectoryEditor.Speaker -> EditorCard(
            title = if (editor.original == null) "Neuer Redner" else "Redner bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { speaker ->
                {
                    onRequestDelete(
                        DeleteRequest("Redner „${speaker.displayName()}“ wird gelöscht.") {
                            actions.onDeleteSpeaker(speaker)
                        },
                    )
                }
            },
        ) {
            SpeakerFields(editor, directory) { actions.onEditorChange(it) }
        }
    }
}

private fun ExternalDirectory.circuitName(circuitId: String): String =
    circuits.firstOrNull { it.id == circuitId }?.name.orEmpty()

@Composable
private fun SpeakerFields(
    editor: ExternalDirectoryEditor.Speaker,
    directory: ExternalDirectory,
    onChange: (ExternalDirectoryEditor.Speaker) -> Unit,
) {
    val speaker = editor.speaker
    CongregationSelector(directory, speaker.congregationId) {
        onChange(editor.copy(speaker = speaker.copy(congregationId = it)))
    }
    NameField("Familienname", speaker.lastName) { onChange(editor.copy(speaker = speaker.copy(lastName = it))) }
    NameField("Vorname", speaker.firstName) { onChange(editor.copy(speaker = speaker.copy(firstName = it))) }
    NameField("Handy", speaker.mobilePhone) { onChange(editor.copy(speaker = speaker.copy(mobilePhone = it))) }
    NameField("Festnetz", speaker.landlinePhone) { onChange(editor.copy(speaker = speaker.copy(landlinePhone = it))) }
    NameField("E-Mail", speaker.email) { onChange(editor.copy(speaker = speaker.copy(email = it))) }
    OutlinedTextField(
        value = editor.talksText,
        onValueChange = { onChange(editor.copy(talksText = it)) },
        label = { Text("Vorträge (Nummern)") },
        supportingText = { Text("Nummern aus der Vortragsliste, z. B. 12, 45, 101") },
        modifier = Modifier.fillMaxWidth(),
    )
    RankingBar(speaker.ranking) {
        onChange(editor.copy(speaker = speaker.copy(ranking = nextRanking(speaker.ranking, it))))
    }
}

/** Bewertung des Redners in Sternen; ein Tipp auf den aktuellen Stern löscht sie. */
@Composable
private fun RankingBar(ranking: Int, onTap: (Int) -> Unit) {
    Column {
        Text("Bewertung", style = MaterialTheme.typography.labelMedium)
        Row {
            rankingStars(ranking).forEachIndexed { index, filled ->
                TextButton(
                    onClick = { onTap(index + 1) },
                    modifier = Modifier.semantics { contentDescription = "${index + 1} Sterne" },
                ) { Text(if (filled) "★" else "☆", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

/** Auswahl der Versammlung eines Redners; die Einträge nennen den Kreis, weil Namen doppelt vorkommen können. */
@Composable
private fun CongregationSelector(directory: ExternalDirectory, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    fun label(congregation: ExternalCongregation) =
        "${congregation.name} (${directory.circuitName(congregation.circuitId)})"
    Column {
        Text("Versammlung", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(directory.congregations.firstOrNull { it.id == selectedId }?.let(::label).orEmpty())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            directory.congregations.forEach { congregation ->
                DropdownMenuItem(
                    text = { Text(label(congregation)) },
                    onClick = {
                        onSelect(congregation.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NameField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

/** Nur-Lesen-Ansicht für den Helfer: dieselben Angaben wie im Editor, aber ohne Felder und Aktionen. */
@Composable
private fun ReadOnlyCard(view: ReadOnlyView) {
    Card(modifier = Modifier.fillMaxWidth(), colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(view.title, style = MaterialTheme.typography.titleMedium)
            view.lines.forEach { Text(it) }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun EditorCard(
    title: String,
    editor: ExternalDirectoryEditor,
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    onDelete: (() -> Unit)?,
    fields: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            fields()
            EditorActions(
                saveText = if (state.isSaving) "Speichert …" else "Speichern",
                saveEnabled = !isBusy && editor.isDirty,
                discardEnabled = !isBusy,
                onSave = actions.onSave,
                onDiscard = actions.onDiscard,
                // Nur ein gespeicherter Eintrag lässt sich löschen; ein neuer ist noch nicht angelegt.
                onDelete = onDelete,
                deleteEnabled = !isBusy,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ExternalDirectoryContentPreview() = PreviewThemeWrapper {
    val circuit = ExternalCircuit("c1", "Kreis Nord")
    val congregation = ExternalCongregation("g1", "c1", "Musterstadt")
    val speaker = VisitingSpeaker(
        id = "s1",
        congregationId = "g1",
        lastName = "Beispiel",
        firstName = "Tim",
        mobilePhone = "0170 1234",
        landlinePhone = "08092 1234",
        email = "tim@example.org",
        talkNumbers = listOf(12, 45),
        ranking = 3,
    )
    ExternalDirectoryContent(
        state = ExternalDirectoryUiState(
            directory = ExternalDirectory(listOf(circuit), listOf(congregation), listOf(speaker)),
            editor = ExternalDirectoryEditor.Speaker(speaker, "12, 45", speaker),
            isLoading = false,
            canView = true,
            canPlan = true,
        ),
        actions = ExternalDirectoryActions(),
    )
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
