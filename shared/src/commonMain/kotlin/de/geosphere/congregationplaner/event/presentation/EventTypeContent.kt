package de.geosphere.congregationplaner.event.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.event.di.EventTypeGraph
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.talk.presentation.reference.ErrorRow
import de.geosphere.congregationplaner.talk.presentation.reference.ReferenceListDetail
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.dialog.ConfirmDialog
import de.geosphere.congregationplaner.ui.field.AppSwitch
import de.geosphere.congregationplaner.ui.list.SelectableListCard
import de.geosphere.congregationplaner.ui.pane.DetailPlaceholder
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun EventTypeRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<EventTypeGraph>().eventTypeManagementViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    EventTypeContent(
        state = state,
        actions = EventTypeActions(
            onNew = { viewModel.editType(null) },
            onEdit = viewModel::editType,
            onEditorChange = viewModel::updateEditor,
            onSave = viewModel::save,
            onRequestDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onCancelDelete = { viewModel.requestDelete(null) },
            onAdoptBlueprint = viewModel::adoptBlueprint,
            onCancel = { viewModel.updateEditor(null) },
            onDismissInfo = viewModel::dismissMessages,
            onDismissError = viewModel::dismissMessages,
        ),
    )
}

@Composable
internal fun EventTypeContent(state: EventTypeUiState, actions: EventTypeActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.canManage) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        ErrorRow(state.errorMessage, actions.onDismissError)
        state.infoMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = NightColors.Sky, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onDismissInfo) { Text("OK") }
            }
        }
        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        OutlinedButton(onClick = actions.onAdoptBlueprint, enabled = !state.isSaving) {
            Text("Vorlage des Landes übernehmen")
        }
        ReferenceListDetail(
            state = state,
            onClose = actions.onCancel,
            createLabel = "Ereignisart anlegen",
            onCreate = actions.onNew,
            list = { EventTypeList(state, actions) },
            detail = {
                state.editor?.let { EventTypeEditorCard(it, state.isSaving, actions) }
                    ?: DetailPlaceholder("Wähle eine Ereignisart aus oder lege eine neue an.")
            },
        )
    }
    state.pendingDelete?.let { type ->
        ConfirmDialog(
            title = "Ereignisart löschen?",
            message = deleteEventTypeHint(type),
            confirmText = "Löschen",
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onCancelDelete,
            isDangerous = true,
        )
    }
}

@Composable
private fun EventTypeList(state: EventTypeUiState, actions: EventTypeActions) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.types.isEmpty()) {
            item { Text("Noch keine Ereignisarten. Übernimm die Vorlage des Landes oder lege eine an.") }
        }
        items(state.types, key = { it.id }) { type ->
            SelectableListCard(
                selected = state.editor?.original?.id == type.id,
                onSelect = { actions.onEdit(type) },
                onDelete = { actions.onRequestDelete(type) },
                actionsEnabled = !state.isSaving,
                canDelete = true,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(eventTypeIcon(type.icon)), contentDescription = null, Modifier.size(20.dp))
                    Text(type.name, style = MaterialTheme.typography.titleMedium)
                }
                if (type.cancelsTalk) Text("Vortrag entfällt", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EventTypeEditorCard(editor: EventTypeEditorState, isSaving: Boolean, actions: EventTypeActions) {
    val type = editor.type
    var picking by remember { mutableStateOf(false) }
    Card(colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (editor.isNew) "Neue Ereignisart" else "Ereignisart ändern",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = type.name,
                onValueChange = { actions.onEditorChange(editor.copy(type = type.copy(name = it))) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vortrag entfällt", modifier = Modifier.weight(1f))
                AppSwitch(
                    checked = type.cancelsTalk,
                    onCheckedChange = { actions.onEditorChange(editor.copy(type = type.copy(cancelsTalk = it))) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(eventTypeIcon(type.icon)), contentDescription = null, Modifier.size(32.dp))
                Text(iconLabel(type.icon), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { picking = true }, enabled = !isSaving) { Text("Icon wählen") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onSave, enabled = !isSaving && editor.isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = actions.onCancel, enabled = !isSaving) { Text("Schließen") }
            }
        }
    }
    if (picking) {
        EventIconPickerDialog(
            selected = type.icon,
            onSelect = { icon ->
                picking = false
                actions.onEditorChange(editor.copy(type = type.copy(icon = icon)))
            },
            onDismiss = { picking = false },
        )
    }
}

private val previewTypes = listOf(
    EventType("dw", "Dienstwoche", icon = "ic_cp_persons_doorstep_duo"),
    EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo"),
)

@ThemePreviews
@Composable
private fun EventTypeContentPreview() = PreviewThemeWrapper {
    EventTypeContent(
        state = EventTypeUiState(
            types = previewTypes,
            isLoading = false,
            canManage = true,
            editor = EventTypeEditorState(previewTypes[1], previewTypes[1]),
            infoMessage = blueprintMessage(2),
        ),
        actions = EventTypeActions(),
    )
}

@ThemePreviews
@Composable
private fun EventTypeContentEmptyPreview() = PreviewThemeWrapper {
    EventTypeContent(
        state = EventTypeUiState(isLoading = false, canManage = true, errorMessage = "Keine Verbindung."),
        actions = EventTypeActions(),
    )
}

@ThemePreviews
@Composable
private fun EventTypeDeletePreview() = PreviewThemeWrapper {
    EventTypeContent(
        state = EventTypeUiState(
            types = previewTypes,
            isLoading = false,
            canManage = true,
            pendingDelete = previewTypes[1],
        ),
        actions = EventTypeActions(),
    )
}

@ThemePreviews
@Composable
private fun EventTypeEditorSavingPreview() = PreviewThemeWrapper {
    EventTypeEditorCard(EventTypeEditorState(EventType(name = "Neu")), isSaving = true, actions = EventTypeActions())
}

@ThemePreviews
@Composable
private fun EventTypeContentNotAllowedPreview() = PreviewThemeWrapper {
    EventTypeContent(state = EventTypeUiState(isLoading = false), actions = EventTypeActions())
}
