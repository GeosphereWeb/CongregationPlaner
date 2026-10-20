package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CircuitDetail(
    editor: CircuitEditorState?,
    canEdit: Boolean,
    isSaving: Boolean,
    isBusy: Boolean,
    onChange: (Circuit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    if (editor == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Wähle links einen Kreis aus oder lege einen neuen an.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else if (!canEdit) {
        CircuitSummary(editor.circuit)
    } else {
        CircuitEditor(
            editor = editor,
            isSaving = isSaving,
            isBusy = isBusy,
            onChange = onChange,
            onSave = onSave,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun CircuitSummary(circuit: Circuit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(circuit.name, style = MaterialTheme.typography.titleMedium)
            Text("Kreisnummer: ${circuit.id}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Kreisaufseher: ${circuit.overseerName.ifBlank { "–" }}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CircuitDetailReadOnlyPreview() = PreviewThemeWrapper {
    CircuitDetail(
        editor = CircuitEditorState(previewCircuits.first(), isNew = false),
        canEdit = false,
        isSaving = false,
        isBusy = false,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}

@ThemePreviews
@Composable
private fun CircuitDetailNewPreview() = PreviewThemeWrapper {
    CircuitDetail(
        editor = CircuitEditorState(Circuit(), isNew = true),
        canEdit = true,
        isSaving = false,
        isBusy = false,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
