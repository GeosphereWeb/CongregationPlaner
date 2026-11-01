package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.DetailPlaceholder
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Callbacks der Pflege-Oberfläche; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class TalkCatalogActions(
    val onNew: () -> Unit = {},
    val onEdit: (Talk) -> Unit = {},
    val onEditorChange: (TalkEditorState) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (Talk) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

internal data class WeekEventActions(
    val onNew: () -> Unit = {},
    val onEdit: (WeekEvent) -> Unit = {},
    val onEditorChange: (WeekEventEditorState) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (WeekEvent) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

@Composable
fun TalkReferenceScreen(
    catalogViewModel: TalkCatalogManagementViewModel,
    eventViewModel: WeekEventManagementViewModel,
    modifier: Modifier = Modifier,
) {
    val catalogState by catalogViewModel.uiState.collectAsState()
    val eventState by eventViewModel.uiState.collectAsState()
    TalkReferenceContent(
        catalogState = catalogState,
        catalogActions = TalkCatalogActions(
            onNew = catalogViewModel::newTalk,
            onEdit = catalogViewModel::editTalk,
            onEditorChange = catalogViewModel::updateEditor,
            onSave = catalogViewModel::save,
            onDelete = catalogViewModel::delete,
            onCancel = catalogViewModel::cancelEditing,
            onDismissError = catalogViewModel::dismissError,
        ),
        eventState = eventState,
        eventActions = WeekEventActions(
            onNew = eventViewModel::newEvent,
            onEdit = eventViewModel::editEvent,
            onEditorChange = eventViewModel::updateEditor,
            onSave = eventViewModel::save,
            onDelete = eventViewModel::delete,
            onCancel = eventViewModel::cancelEditing,
            onDismissError = eventViewModel::dismissError,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun TalkReferenceContent(
    catalogState: TalkCatalogUiState,
    catalogActions: TalkCatalogActions,
    eventState: WeekEventUiState,
    eventActions: WeekEventActions,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListPaneHeader("Vortragsliste & Ereignisse")
        if (!catalogState.canManage) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Vorträge") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Wochenereignisse") })
        }
        if (selectedTab == 0) {
            TalkCatalogTab(catalogState, catalogActions)
        } else {
            WeekEventTab(eventState, eventActions)
        }
    }
}

@Composable
private fun ErrorRow(message: String?, onDismiss: () -> Unit) {
    message?.let {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

/** Liste links, Detail rechts; auf kleinen Bildschirmen nacheinander (siehe [rememberListDetailNavigation]). */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ReferenceListDetail(
    state: ListDetailState,
    onClose: () -> Unit,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    val navigator = rememberListDetailNavigation(state, onClose = onClose)
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        modifier = Modifier.fillMaxSize(),
        listPane = { AnimatedPane { list() } },
        detailPane = {
            AnimatedPane {
                DetailPane(
                    showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                    onBack = onClose,
                    backEnabled = !state.isSaving,
                ) {
                    detail()
                }
            }
        },
    )
}

@Composable
private fun TalkCatalogTab(state: TalkCatalogUiState, actions: TalkCatalogActions) {
    ErrorRow(state.errorMessage, actions.onDismissError)
    if (state.isLoading) {
        CircularProgressIndicator()
        return
    }
    OutlinedButton(onClick = actions.onNew, enabled = !state.isSaving) { Text("Vortrag anlegen") }
    ReferenceListDetail(
        state = state,
        onClose = actions.onCancel,
        list = {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.talks.isEmpty()) item { Text("Noch keine Vorträge in der Liste.") }
                items(state.talks, key = { it.number }) { talk ->
                    SelectableListCard(
                        selected = state.editor?.original?.number == talk.number,
                        onSelect = { actions.onEdit(talk) },
                        onDelete = { actions.onDelete(talk) },
                        actionsEnabled = !state.isSaving,
                        canDelete = true,
                    ) {
                        Text("${talk.number} – ${talk.title}", style = MaterialTheme.typography.titleMedium)
                        if (!talk.active) Text("nicht aktiv", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        detail = {
            state.editor?.let { TalkEditorCard(it, state.isSaving, actions) }
                ?: DetailPlaceholder("Wähle einen Vortrag aus oder lege einen neuen an.")
        },
    )
}

@Composable
private fun TalkEditorCard(editor: TalkEditorState, isSaving: Boolean, actions: TalkCatalogActions) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (editor.isNew) "Neuer Vortrag" else "Vortrag ändern", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = editor.numberText,
                onValueChange = { actions.onEditorChange(editor.copy(numberText = it)) },
                label = { Text("Nummer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = editor.isNew,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (!editor.isNew) Text("Die Nummer ist die ID und bleibt unverändert.") },
            )
            OutlinedTextField(
                value = editor.title,
                onValueChange = { actions.onEditorChange(editor.copy(title = it)) },
                label = { Text("Titel") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aktiv", modifier = Modifier.weight(1f))
                Switch(checked = editor.active, onCheckedChange = { actions.onEditorChange(editor.copy(active = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onSave, enabled = !isSaving && editor.isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = actions.onCancel, enabled = !isSaving) { Text("Schließen") }
            }
        }
    }
}

@Composable
private fun WeekEventTab(state: WeekEventUiState, actions: WeekEventActions) {
    ErrorRow(state.errorMessage, actions.onDismissError)
    if (state.isLoading) {
        CircularProgressIndicator()
        return
    }
    OutlinedButton(onClick = actions.onNew, enabled = !state.isSaving) { Text("Ereignis anlegen") }
    ReferenceListDetail(
        state = state,
        onClose = actions.onCancel,
        list = {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.events.isEmpty()) item { Text("Noch keine Wochenereignisse.") }
                items(state.events, key = { it.id }) { event ->
                    SelectableListCard(
                        selected = state.editor?.original?.id == event.id,
                        onSelect = { actions.onEdit(event) },
                        onDelete = { actions.onDelete(event) },
                        actionsEnabled = !state.isSaving,
                        canDelete = true,
                    ) {
                        Text(event.name, style = MaterialTheme.typography.titleMedium)
                        if (event.cancelsTalk) Text("Vortrag entfällt")
                    }
                }
            }
        },
        detail = {
            state.editor?.let { WeekEventEditorCard(it, state.isSaving, actions) }
                ?: DetailPlaceholder("Wähle ein Ereignis aus oder lege ein neues an.")
        },
    )
}

@Composable
private fun WeekEventEditorCard(editor: WeekEventEditorState, isSaving: Boolean, actions: WeekEventActions) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (editor.isNew) "Neues Wochenereignis" else "Wochenereignis ändern",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = { actions.onEditorChange(editor.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vortrag entfällt", modifier = Modifier.weight(1f))
                Switch(
                    checked = editor.cancelsTalk,
                    onCheckedChange = { actions.onEditorChange(editor.copy(cancelsTalk = it)) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onSave, enabled = !isSaving && editor.isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = actions.onCancel, enabled = !isSaving) { Text("Schließen") }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun TalkReferenceContentPreview() = PreviewThemeWrapper {
    TalkReferenceContent(
        catalogState = TalkCatalogUiState(
            talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Pausierter Vortrag", active = false)),
            isLoading = false,
            canManage = true,
            editor = TalkEditorState("3", "Neuer Vortrag", active = true),
        ),
        catalogActions = TalkCatalogActions(),
        eventState = WeekEventUiState(
            events = listOf(WeekEvent("kongress", "Kongress", cancelsTalk = true)),
            isLoading = false,
            canManage = true,
        ),
        eventActions = WeekEventActions(),
    )
}
