package de.geosphere.congregationplaner.congregation.presentation

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
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CongregationMetadataContent(
    state: CongregationMetadataUiState,
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
    onSelect: (CongregationMetadata) -> Unit,
    onUpdate: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (CongregationMetadata) -> Unit,
) {
    var congregationToDelete by remember { mutableStateOf<CongregationMetadata?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Versammlung Metadaten", style = MaterialTheme.typography.headlineSmall)
        if (state.permissions.canCreate) {
            Button(
                onClick = onCreate,
                enabled = !state.isLoading && !state.isDeleting && !state.isSaving,
            ) {
                Text("Versammlung hinzufügen")
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
                    CongregationListPane(
                        congregations = state.congregations,
                        selectedId = state.editor?.id,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        onSelect = onSelect,
                        onDelete = { congregationToDelete = it },
                        actionsEnabled = !state.isDeleting && !state.isSaving,
                        canDelete = state.permissions.canManage,
                    )

                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 4.dp),
                    ) {
                        CongregationDetail(
                            congregation = state.editor,
                            canEdit = state.permissions.canEditSelection,
                            isSaving = state.isSaving,
                            isBusy = state.isSaving || state.isDeleting,
                            onChange = onUpdate,
                            onSave = onSave,
                            onCancel = onCancel,
                        )
                    }
                }
            } else if (editor == null) {
                CongregationListPane(
                    congregations = state.congregations,
                    selectedId = null,
                    modifier = Modifier.fillMaxSize(),
                    onSelect = onSelect,
                    onDelete = { congregationToDelete = it },
                    actionsEnabled = !state.isDeleting && !state.isSaving,
                    canDelete = state.permissions.canManage,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !state.isSaving && !state.isDeleting,
                    ) {
                        Text("Zurück zur Liste")
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        CongregationDetail(
                            congregation = editor,
                            canEdit = state.permissions.canEditSelection,
                            isSaving = state.isSaving,
                            isBusy = state.isSaving || state.isDeleting,
                            onChange = onUpdate,
                            onSave = onSave,
                            onCancel = onCancel,
                        )
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
                        onDelete(congregation)
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

@ThemePreviews
@Composable
private fun CongregationMetadataScreenPreview() = PreviewThemeWrapper {
    CongregationMetadataContent(
        state = CongregationMetadataUiState(
            congregations = previewCongregations,
            editor = previewCongregations.first(),
            isLoading = false,
            permissions = CongregationPermissions(canCreate = true, canManage = true, canEditSelection = true),
        ),
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDelete = {},
    )
}

@Preview(name = "Versammlungsverwaltung (Desktop)", widthDp = 1280, heightDp = 900)
@Composable
private fun CongregationMetadataDesktopPreview() = AppTheme {
    CongregationMetadataContent(
        state = CongregationMetadataUiState(
            congregations = previewCongregations,
            editor = previewCongregations.first(),
            isLoading = false,
            permissions = CongregationPermissions(canCreate = true, canManage = true, canEditSelection = true),
        ),
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDelete = {},
    )
}
