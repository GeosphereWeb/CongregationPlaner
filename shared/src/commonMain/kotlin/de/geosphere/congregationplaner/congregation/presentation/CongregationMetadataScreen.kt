package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata

@Composable
fun CongregationMetadataScreen(viewModel: CongregationMetadataViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Versammlung Metadaten", style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = viewModel::createCongregation,
            enabled = state.editor == null && !state.isLoading,
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
                onChange = viewModel::updateEditor,
                onSave = viewModel::saveCongregation,
                onCancel = viewModel::cancelEditing,
            )
        }

        if (!state.isLoading && state.congregations.isEmpty()) {
            Text("Noch keine Versammlungen vorhanden.")
        }

        state.congregations.forEach { congregation ->
            CongregationCard(congregation, onEdit = { viewModel.editCongregation(congregation) })
        }
    }
}

@Composable
private fun CongregationEditor(
    congregation: CongregationMetadata,
    isSaving: Boolean,
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
            OutlinedTextField(
                value = congregation.midweekMeetingWeekday,
                onValueChange = { onChange(congregation.copy(midweekMeetingWeekday = it)) },
                label = { Text("Wochentag der Zusammenkunft unter der Woche") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = congregation.midweekMeetingTime,
                onValueChange = { onChange(congregation.copy(midweekMeetingTime = it)) },
                label = { Text("Uhrzeit der Zusammenkunft unter der Woche") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = congregation.weekendMeetingWeekday,
                onValueChange = { onChange(congregation.copy(weekendMeetingWeekday = it)) },
                label = { Text("Wochentag der Wochenend-Zusammenkunft") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = congregation.weekendMeetingTime,
                onValueChange = { onChange(congregation.copy(weekendMeetingTime = it)) },
                label = { Text("Uhrzeit der Wochenend-Zusammenkunft") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isSaving) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@Composable
private fun CongregationCard(congregation: CongregationMetadata, onEdit: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(congregation.name, style = MaterialTheme.typography.titleMedium)
            Text("Unter der Woche: ${congregation.midweekMeetingWeekday} ${congregation.midweekMeetingTime}")
            Text("Wochenende: ${congregation.weekendMeetingWeekday} ${congregation.weekendMeetingTime}")
            TextButton(onClick = onEdit) {
                Text("Bearbeiten")
            }
        }
    }
}
