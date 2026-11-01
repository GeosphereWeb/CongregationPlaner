package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongParameterList", "LongMethod")
internal fun CircuitContent(
    state: CircuitUiState,
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
    onSelect: (Circuit) -> Unit,
    onUpdate: (Circuit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: (Circuit) -> Unit,
) {
    var circuitToDelete by remember { mutableStateOf<Circuit?>(null) }
    val isBusy = state.isSaving || state.isDeleting
    val selectedId = state.editor?.takeUnless { it.isNew }?.circuit?.id

    val navigator = rememberListDetailNavigation(state, onClose = onCancel)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) onCancel else onDiscard

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListPaneHeader("Kreise")
        if (state.permissions.canCreate) {
            Button(onClick = onCreate, enabled = !state.isLoading && !isBusy) {
                Text("Kreis hinzufügen")
            }
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                listPane = {
                    AnimatedPane {
                        CircuitListPane(
                            circuits = state.circuits,
                            selectedId = selectedId,
                            modifier = Modifier.fillMaxSize(),
                            onSelect = onSelect,
                            onDelete = { circuitToDelete = it },
                            actionsEnabled = !isBusy,
                            canDelete = state.permissions.canManage,
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        DetailPane(
                            showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] ==
                                PaneAdaptedValue.Hidden,
                            onBack = onCancel,
                            backEnabled = !isBusy,
                        ) {
                            CircuitDetail(
                                editor = state.editor,
                                canEdit = state.permissions.canEditSelection,
                                isSaving = state.isSaving,
                                isBusy = isBusy,
                                onChange = onUpdate,
                                onSave = onSave,
                                onCancel = onCancelEditing,
                            )
                        }
                    }
                },
            )
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
        onDiscard = {},
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
        onDiscard = {},
        onDelete = {},
    )
}
