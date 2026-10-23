package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CircuitEditor(
    editor: CircuitEditorState,
    isSaving: Boolean,
    isBusy: Boolean,
    isDirty: Boolean,
    onChange: (Circuit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val circuit = editor.circuit
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (editor.isNew) "Neuer Kreis" else "Kreis bearbeiten",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = circuit.id,
                onValueChange = { onChange(circuit.copy(id = it)) },
                label = { Text("Kreisnummer") },
                supportingText = {
                    Text(
                        if (editor.isNew) {
                            "z. B. 609a – kann nach dem Anlegen nicht mehr geändert werden."
                        } else {
                            "Die Kreisnummer ist nicht änderbar."
                        },
                    )
                },
                enabled = editor.isNew,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = circuit.name,
                onValueChange = { onChange(circuit.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = circuit.overseerName,
                onValueChange = { onChange(circuit.copy(overseerName = it)) },
                label = { Text("Kreisaufseher (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isBusy && isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isBusy) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CircuitEditorPreview() = PreviewThemeWrapper {
    CircuitEditor(
        editor = CircuitEditorState(previewCircuits.first(), isNew = false),
        isSaving = false,
        isBusy = false,
        isDirty = true,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
