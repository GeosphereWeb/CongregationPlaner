package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Versammlung Metadaten", style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = viewModel::createCongregation,
            enabled = state.editor == null && !state.isLoading && !state.isDeleting,
        ) {
            Text("Versammlung hinzufügen")
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.editor?.let { editor ->
            CongregationEditor(
                congregation = editor,
                isSaving = state.isSaving,
                isBusy = state.isSaving || state.isDeleting,
                onChange = viewModel::updateEditor,
                onSave = viewModel::saveCongregation,
                onCancel = viewModel::cancelEditing,
            )
        }

        if (!state.isLoading && state.congregations.isEmpty()) {
            Text("Noch keine Versammlungen vorhanden.")
        }

        state.congregations.forEach { congregation ->
            CongregationCard(
                congregation = congregation,
                onEdit = { viewModel.editCongregation(congregation) },
                onDelete = { congregationToDelete = congregation },
                actionsEnabled = !state.isDeleting && !state.isSaving,
            )
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
private fun CongregationCard(
    congregation: CongregationMetadata,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(congregation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Unter der Woche: ${weekdayName(congregation.midweekMeetingWeekday)} " +
                    congregation.midweekMeetingTime,
            )
            Text(
                "Wochenende: ${weekdayName(congregation.weekendMeetingWeekday)} " +
                    congregation.weekendMeetingTime,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit, enabled = actionsEnabled) {
                    Text("Bearbeiten")
                }
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
