package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import kotlinx.datetime.LocalTime

@Composable
fun CongregationMetadataScreen(viewModel: CongregationMetadataViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var congregationToDelete by remember { mutableStateOf<CongregationMetadata?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Versammlung Metadaten", style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = viewModel::createCongregation,
            enabled = !state.isLoading && !state.isDeleting && !state.isSaving,
        ) {
            Text("Versammlung hinzufügen")
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideLayout = maxWidth >= 840.dp
            val editor = state.editor

            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CongregationListPane(
                        congregations = state.congregations,
                        selectedId = state.editor?.id,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        onSelect = viewModel::editCongregation,
                        onDelete = { congregationToDelete = it },
                        actionsEnabled = !state.isDeleting && !state.isSaving,
                    )

                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 4.dp),
                    ) {
                        CongregationDetail(
                            congregation = state.editor,
                            isSaving = state.isSaving,
                            isBusy = state.isSaving || state.isDeleting,
                            onChange = viewModel::updateEditor,
                            onSave = viewModel::saveCongregation,
                            onCancel = viewModel::cancelEditing,
                        )
                    }
                }
            } else {
                if (editor == null) {
                    CongregationListPane(
                        congregations = state.congregations,
                        selectedId = null,
                        modifier = Modifier.fillMaxSize(),
                        onSelect = viewModel::editCongregation,
                        onDelete = { congregationToDelete = it },
                        actionsEnabled = !state.isDeleting && !state.isSaving,
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = viewModel::cancelEditing,
                            enabled = !state.isSaving && !state.isDeleting,
                        ) {
                            Text("Zurück zur Liste")
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            CongregationEditor(
                                congregation = editor,
                                isSaving = state.isSaving,
                                isBusy = state.isSaving || state.isDeleting,
                                onChange = viewModel::updateEditor,
                                onSave = viewModel::saveCongregation,
                                onCancel = viewModel::cancelEditing,
                            )
                        }
                    }
                }
            }
        }
    }

    congregationToDelete?.let { congregation ->
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting && !state.isSaving) congregationToDelete = null
            },
            title = { Text("Versammlung löschen?") },
            text = { Text("„${congregation.name}“ wird dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCongregation(congregation)
                        congregationToDelete = null
                    },
                    enabled = !state.isDeleting && !state.isSaving,
                ) {
                    Text(if (state.isDeleting) "Löscht …" else "Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { congregationToDelete = null },
                    enabled = !state.isDeleting && !state.isSaving,
                ) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun CongregationListPane(
    congregations: List<CongregationMetadata>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (CongregationMetadata) -> Unit,
    onDelete: (CongregationMetadata) -> Unit,
    actionsEnabled: Boolean,
) {
    Column(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Versammlungen", style = MaterialTheme.typography.titleMedium)
            }
            if (congregations.isEmpty()) {
                item {
                    Text("Noch keine Versammlungen vorhanden.")
                }
            } else {
                items(congregations, key = { congregation -> congregation.id }) { congregation ->
                    CongregationListItem(
                        congregation = congregation,
                        selected = selectedId == congregation.id,
                        onSelect = { onSelect(congregation) },
                        onDelete = { onDelete(congregation) },
                        actionsEnabled = actionsEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun CongregationDetail(
    congregation: CongregationMetadata?,
    isSaving: Boolean,
    isBusy: Boolean,
    onChange: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    if (congregation == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Wähle links eine Versammlung aus oder lege eine neue an.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        CongregationEditor(
            congregation = congregation,
            isSaving = isSaving,
            isBusy = isBusy,
            onChange = onChange,
            onSave = onSave,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun CongregationEditor(
    congregation: CongregationMetadata,
    isSaving: Boolean,
    isBusy: Boolean,
    onChange: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (congregation.id.isBlank()) "Neue Versammlung" else "Versammlung bearbeiten",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = congregation.name,
                onValueChange = { onChange(congregation.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            WeekdaySelector(
                label = "Wochentag der Zusammenkunft unter der Woche",
                weekday = congregation.midweekMeetingWeekday,
                onWeekdayChange = { onChange(congregation.copy(midweekMeetingWeekday = it)) },
            )
            MeetingTimeInput(
                label = "Uhrzeit der Zusammenkunft unter der Woche",
                time = congregation.midweekMeetingTime,
                onTimeChange = { onChange(congregation.copy(midweekMeetingTime = it)) },
            )
            WeekdaySelector(
                label = "Wochentag der Wochenend-Zusammenkunft",
                weekday = congregation.weekendMeetingWeekday,
                onWeekdayChange = { onChange(congregation.copy(weekendMeetingWeekday = it)) },
            )
            MeetingTimeInput(
                label = "Uhrzeit der Wochenend-Zusammenkunft",
                time = congregation.weekendMeetingTime,
                onTimeChange = { onChange(congregation.copy(weekendMeetingTime = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isBusy) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isBusy) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingTimeInput(label: String, time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = time.toInputValue(),
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
        readOnly = true,
        supportingText = { Text("Tippen, um eine Uhrzeit auszuwählen.") },
        trailingIcon = {
            TextButton(onClick = { showTimePicker = true }) {
                Text("Wählen")
            }
        },
        singleLine = true,
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(label) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

private fun LocalTime.toInputValue(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
private fun WeekdaySelector(label: String, weekday: Int, onWeekdayChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(weekdayName(weekday))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WEEKDAYS.forEach { (number, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onWeekdayChange(number)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CongregationListItem(
    congregation: CongregationMetadata,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(congregation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${weekdayName(congregation.midweekMeetingWeekday)} ${congregation.midweekMeetingTime} · " +
                    "${weekdayName(congregation.weekendMeetingWeekday)} ${congregation.weekendMeetingTime}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete, enabled = actionsEnabled) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun weekdayName(weekday: Int): String = WEEKDAYS.firstOrNull { it.first == weekday }?.second ?: "Ungültig"

private val WEEKDAYS = listOf(
    1 to "Montag",
    2 to "Dienstag",
    3 to "Mittwoch",
    4 to "Donnerstag",
    5 to "Freitag",
    6 to "Samstag",
    7 to "Sonntag",
)
