package de.geosphere.congregationplaner.event.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.event.di.SpecialEventGraph
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.presentation.components.rememberScopedViewModel
import de.geosphere.congregationplaner.talk.presentation.reference.ErrorRow
import de.geosphere.congregationplaner.talk.presentation.reference.ReferenceListDetail
import de.geosphere.congregationplaner.talk.presentation.schedule.TalkDateInput
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.list.SelectableListCard
import de.geosphere.congregationplaner.ui.pane.DetailPlaceholder
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SpecialEventRoute(currentUser: AppUser) {
    val viewModel = rememberScopedViewModel { createGraph<SpecialEventGraph>().specialEventManagementViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    SpecialEventContent(
        state = state,
        actions = SpecialEventActions(
            onNew = { viewModel.newEvent() },
            onEdit = viewModel::editEvent,
            onEditorChange = viewModel::updateEditor,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onCancel = viewModel::cancelEditing,
            onDismissError = viewModel::dismissError,
        ),
    )
}

@Composable
internal fun SpecialEventContent(
    state: SpecialEventUiState,
    actions: SpecialEventActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.canManage) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        ErrorRow(state.errorMessage, actions.onDismissError)
        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        ReferenceListDetail(
            state = state,
            onClose = actions.onCancel,
            createLabel = "Ereignis anlegen",
            onCreate = actions.onNew,
            list = { SpecialEventList(state, actions) },
            detail = {
                state.editor?.let { SpecialEventEditorCard(it, state.eventTypes, state.isSaving, actions) }
                    ?: DetailPlaceholder("Wähle ein Ereignis aus oder lege ein neues an.")
            },
        )
    }
}

@Composable
private fun SpecialEventList(state: SpecialEventUiState, actions: SpecialEventActions) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.events.isEmpty()) item { Text("Noch keine besonderen Ereignisse.") }
        items(state.events, key = { it.id }) { event ->
            SelectableListCard(
                selected = state.editor?.original?.id == event.id,
                onSelect = { actions.onEdit(event) },
                onDelete = { actions.onDelete(event) },
                actionsEnabled = !state.isSaving,
                canDelete = true,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(eventTypeIcon(specialEventIconName(event, state.eventTypes))),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                }
                Text(event.dateRangeText())
                Text("Sichtbar für: ${event.audienceText()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SpecialEventEditorCard(
    editor: SpecialEventEditorState,
    eventTypes: List<EventType>,
    isSaving: Boolean,
    actions: SpecialEventActions,
) {
    val event = editor.event
    Card(colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (editor.isNew) "Neues Ereignis" else "Ereignis ändern",
                style = MaterialTheme.typography.titleMedium,
            )
            EventTypeChips(event.eventTypeId, eventTypes, enabled = !isSaving) {
                actions.onEditorChange(editor.withType(it))
            }
            OutlinedTextField(
                value = event.title,
                onValueChange = { actions.onEditorChange(editor.copy(event = event.copy(title = it))) },
                label = { Text("Titel") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            TalkDateInput(event.startDate, label = "Beginn") { actions.onEditorChange(editor.withStart(it)) }
            TalkDateInput(event.endDate, label = "Ende") { actions.onEditorChange(editor.withEnd(it)) }
            Text("Sichtbar für", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventAudience.entries.forEach { group ->
                    FilterChip(
                        selected = group in event.audience,
                        onClick = { actions.onEditorChange(editor.toggled(group)) },
                        label = { Text(group.label) },
                        enabled = !isSaving,
                    )
                }
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

/** Auswahl der Ereignisart mit Icon; „Ohne Art“ lässt nur den Titel. */
@Composable
private fun EventTypeChips(
    selectedId: String?,
    eventTypes: List<EventType>,
    enabled: Boolean,
    onSelect: (EventType?) -> Unit,
) {
    Text("Art", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text("Ohne Art") },
            enabled = enabled,
        )
        eventTypes.forEach { type ->
            FilterChip(
                selected = selectedId == type.id,
                onClick = { onSelect(type) },
                label = { Text(type.name) },
                leadingIcon = {
                    Icon(painterResource(eventTypeIcon(type.icon)), contentDescription = null, Modifier.size(18.dp))
                },
                enabled = enabled,
            )
        }
    }
}

private val previewTypes = listOf(
    EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo"),
    EventType("dw", "Dienstwoche", icon = "ic_cp_persons_doorstep_duo"),
)

private val previewEvents = listOf(
    SpecialEvent("1", "Kreiskongress", LocalDate(2027, 3, 6), LocalDate(2027, 3, 7), eventTypeId = "kk"),
    SpecialEvent(
        "2",
        "Ältestenschule",
        LocalDate(2027, 4, 12),
        audience = setOf(EventAudience.ELDERS, EventAudience.MINISTERIAL_SERVANTS),
    ),
)

@ThemePreviews
@Composable
private fun SpecialEventContentPreview() = PreviewThemeWrapper {
    SpecialEventContent(
        state = SpecialEventUiState(
            events = previewEvents,
            eventTypes = previewTypes,
            isLoading = false,
            canManage = true,
            editor = SpecialEventEditorState(previewEvents[1], previewEvents[1]),
        ),
        actions = SpecialEventActions(),
    )
}

@ThemePreviews
@Composable
private fun SpecialEventContentEmptyPreview() = PreviewThemeWrapper {
    SpecialEventContent(
        state = SpecialEventUiState(isLoading = false, canManage = true, errorMessage = "Keine Verbindung."),
        actions = SpecialEventActions(),
    )
}

@ThemePreviews
@Composable
private fun SpecialEventEditorSavingPreview() = PreviewThemeWrapper {
    SpecialEventEditorCard(
        SpecialEventEditorState(SpecialEvent(title = "Neu", startDate = LocalDate(2027, 5, 1))),
        eventTypes = previewTypes,
        isSaving = true,
        actions = SpecialEventActions(),
    )
}

@ThemePreviews
@Composable
private fun SpecialEventContentNotAllowedPreview() = PreviewThemeWrapper {
    SpecialEventContent(state = SpecialEventUiState(isLoading = false), actions = SpecialEventActions())
}
