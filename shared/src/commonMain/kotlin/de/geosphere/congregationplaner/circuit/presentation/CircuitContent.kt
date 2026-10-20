package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun CircuitContent(
    state: CircuitUiState,
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
    onSelect: (Circuit) -> Unit,
    onUpdate: (Circuit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (Circuit) -> Unit,
) {
    var circuitToDelete by remember { mutableStateOf<Circuit?>(null) }
    val isBusy = state.isSaving || state.isDeleting
    val selectedId = state.editor?.takeUnless { it.isNew }?.circuit?.id

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Kreise", style = MaterialTheme.typography.headlineSmall)
        if (state.permissions.canCreate) {
            Button(onClick = onCreate, enabled = !state.isLoading && !isBusy) {
                Text("Kreis hinzufügen")
            }
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
                    CircuitListPane(
                        circuits = state.circuits,
                        selectedId = selectedId,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        onSelect = onSelect,
                        onDelete = { circuitToDelete = it },
                        actionsEnabled = !isBusy,
                        canDelete = state.permissions.canManage,
                    )

                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 4.dp),
                    ) {
                        CircuitDetail(
                            editor = editor,
                            canEdit = state.permissions.canEditSelection,
                            isSaving = state.isSaving,
                            isBusy = isBusy,
                            onChange = onUpdate,
                            onSave = onSave,
                            onCancel = onCancel,
                        )
                    }
                }
            } else if (editor == null) {
                CircuitListPane(
                    circuits = state.circuits,
                    selectedId = null,
                    modifier = Modifier.fillMaxSize(),
                    onSelect = onSelect,
                    onDelete = { circuitToDelete = it },
                    actionsEnabled = !isBusy,
                    canDelete = state.permissions.canManage,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onCancel, enabled = !isBusy) {
                        Text("Zurück zur Liste")
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        CircuitDetail(
                            editor = editor,
                            canEdit = state.permissions.canEditSelection,
                            isSaving = state.isSaving,
                            isBusy = isBusy,
                            onChange = onUpdate,
                            onSave = onSave,
                            onCancel = onCancel,
                        )
                    }
                }
            }
        }
    }

    circuitToDelete?.let { circuit ->
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) circuitToDelete = null
            },
            title = { Text("Kreis löschen?") },
            text = {
                Text(
                    "„${circuit.name}“ (${circuit.id}) wird dauerhaft gelöscht. " +
                        "Kreise mit zugeordneten Versammlungen können nicht gelöscht werden.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(circuit)
                        circuitToDelete = null
                    },
                    enabled = !isBusy,
                ) {
                    Text(if (state.isDeleting) "Löscht …" else "Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { circuitToDelete = null }, enabled = !isBusy) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

private val previewState = CircuitUiState(
    circuits = previewCircuits,
    editor = CircuitEditorState(previewCircuits.first(), isNew = false),
    isLoading = false,
    permissions = CircuitPermissions(canCreate = true, canManage = true, canEditSelection = true),
)

@ThemePreviews
@Composable
private fun CircuitScreenPreview() = PreviewThemeWrapper {
    CircuitContent(
        state = previewState,
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDelete = {},
    )
}

@Preview(name = "Kreisverwaltung (Desktop)", widthDp = 1280, heightDp = 900)
@Composable
private fun CircuitDesktopPreview() = AppTheme {
    CircuitContent(
        state = previewState,
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDelete = {},
    )
}
