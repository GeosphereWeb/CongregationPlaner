package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
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
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

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
        ),
        modifier = modifier,
    )
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListPaneHeader("Externe Redner")
        if (!state.canPlan) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        Button(onClick = actions.onNewCircuit, enabled = !state.isLoading && !isBusy) { Text("Kreis hinzufügen") }
        state.errorMessage?.let { message ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                    ExternalDirectoryTree(state, actions, isBusy) { deleteRequest = it }
                }
            },
            detailPane = {
                AnimatedPane {
                    DetailPane(
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = actions.onClose,
                        backEnabled = !isBusy,
                    ) {
                        ExternalDirectoryDetail(
                            state = state,
                            actions = actions.copy(onDiscard = if (singlePane) actions.onClose else actions.onDiscard),
                            isBusy = isBusy,
                        )
                    }
                }
            },
        )
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
    onRequestDelete: (DeleteRequest) -> Unit,
) {
    val directory = state.directory
    val editor = state.editor
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (directory.circuits.isEmpty()) item { Text("Noch keine Kreise angelegt.") }
        directory.circuits.forEach { circuit ->
            item(key = "circuit-${circuit.id}") {
                SelectableListCard(
                    selected = editor is ExternalDirectoryEditor.Circuit && editor.id == circuit.id,
                    onSelect = { actions.onSelectCircuit(circuit) },
                    onDelete = {
                        onRequestDelete(
                            DeleteRequest("Kreis „${circuit.name}“ wird gelöscht.") {
                                actions.onDeleteCircuit(circuit)
                            },
                        )
                    },
                    actionsEnabled = !isBusy,
                    canDelete = true,
                ) {
                    Text(circuit.name, style = MaterialTheme.typography.titleMedium)
                    Text("${directory.congregations.count { it.circuitId == circuit.id }} Versammlungen")
                }
            }
            directory.congregations.filter { it.circuitId == circuit.id }.forEach { congregation ->
                item(key = "congregation-${congregation.id}") {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        SelectableListCard(
                            selected = editor is ExternalDirectoryEditor.Congregation && editor.id == congregation.id,
                            onSelect = { actions.onSelectCongregation(congregation) },
                            onDelete = {
                                onRequestDelete(
                                    DeleteRequest("Versammlung „${congregation.name}“ wird gelöscht.") {
                                        actions.onDeleteCongregation(congregation)
                                    },
                                )
                            },
                            actionsEnabled = !isBusy,
                            canDelete = true,
                        ) {
                            Text(congregation.name, style = MaterialTheme.typography.titleSmall)
                            Text("${directory.speakers.count { it.congregationId == congregation.id }} Redner")
                        }
                    }
                }
                directory.speakers.filter { it.congregationId == congregation.id }.forEach { speaker ->
                    item(key = "speaker-${speaker.id}") {
                        Column(modifier = Modifier.padding(start = 32.dp)) {
                            SelectableListCard(
                                selected = editor is ExternalDirectoryEditor.Speaker && editor.id == speaker.id,
                                onSelect = { actions.onSelectSpeaker(speaker) },
                                onDelete = {
                                    onRequestDelete(
                                        DeleteRequest("Redner „${speaker.displayName()}“ wird gelöscht.") {
                                            actions.onDeleteSpeaker(speaker)
                                        },
                                    )
                                },
                                actionsEnabled = !isBusy,
                                canDelete = true,
                            ) {
                                Text(speaker.displayName())
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun VisitingSpeaker.displayName(): String =
    listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")

@Composable
private fun ExternalDirectoryDetail(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
) {
    val directory = state.directory
    when (val editor = state.editor) {
        null -> DetailPlaceholder("Wähle einen Kreis, eine Versammlung oder einen Redner aus.")

        is ExternalDirectoryEditor.Circuit -> EditorCard(
            title = if (editor.original == null) "Neuer Kreis" else "Kreis bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            extra = editor.original?.let { saved ->
                { OutlinedButton(onClick = { actions.onNewCongregation(saved.id) }) { Text("Versammlung hinzufügen") } }
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
            extra = editor.original?.let { saved ->
                { OutlinedButton(onClick = { actions.onNewSpeaker(saved.id) }) { Text("Redner hinzufügen") } }
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
    NameField("Telefon", speaker.phone) { onChange(editor.copy(speaker = speaker.copy(phone = it))) }
    NameField("E-Mail", speaker.email) { onChange(editor.copy(speaker = speaker.copy(email = it))) }
    OutlinedTextField(
        value = editor.talksText,
        onValueChange = { onChange(editor.copy(talksText = it)) },
        label = { Text("Vorträge (Nummern)") },
        supportingText = { Text("Nummern aus der Vortragsliste, z. B. 12, 45, 101") },
        modifier = Modifier.fillMaxWidth(),
    )
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

@Composable
@Suppress("LongParameterList")
private fun EditorCard(
    title: String,
    editor: ExternalDirectoryEditor,
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    extra: (@Composable () -> Unit)? = null,
    fields: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            fields()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onSave, enabled = !isBusy && editor.isDirty) {
                    Text(if (state.isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = actions.onDiscard, enabled = !isBusy) { Text("Abbrechen") }
            }
            extra?.invoke()
        }
    }
}

@ThemePreviews
@Composable
private fun ExternalDirectoryContentPreview() = PreviewThemeWrapper {
    val circuit = ExternalCircuit("c1", "Kreis Nord")
    val congregation = ExternalCongregation("g1", "c1", "Musterstadt")
    val speaker = VisitingSpeaker("s1", "g1", "Beispiel", "Tim", "0170 1234", "tim@example.org", listOf(12, 45))
    ExternalDirectoryContent(
        state = ExternalDirectoryUiState(
            directory = ExternalDirectory(listOf(circuit), listOf(congregation), listOf(speaker)),
            editor = ExternalDirectoryEditor.Speaker(speaker, "12, 45", speaker),
            isLoading = false,
            canPlan = true,
        ),
        actions = ExternalDirectoryActions(),
    )
}
