package de.geosphere.congregationplaner.congregation.presentation

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
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongParameterList", "LongMethod")
internal fun CongregationMetadataContent(
    state: CongregationMetadataUiState,
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
    onSelect: (CongregationMetadata) -> Unit,
    onUpdate: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: (CongregationMetadata) -> Unit,
) {
    var congregationToDelete by remember { mutableStateOf<CongregationMetadata?>(null) }

    val isBusy = state.isSaving || state.isDeleting
    val navigator = rememberListDetailNavigation(state, onClose = onCancel)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) onCancel else onDiscard

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListPaneHeader("Versammlung Metadaten")
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

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                listPane = {
                    AnimatedPane {
                        CongregationListPane(
                            congregations = state.congregations,
                            selectedId = state.editor?.id,
                            modifier = Modifier.fillMaxSize(),
                            onSelect = onSelect,
                            onDelete = { congregationToDelete = it },
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
                            CongregationDetail(
                                congregation = state.editor,
                                canEdit = state.permissions.canEditSelection,
                                canEditMeetingTimes = state.permissions.canEditMeetingTimes,
                                isSaving = state.isSaving,
                                isBusy = isBusy,
                                isDirty = state.isEditorDirty,
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
            permissions = CongregationPermissions(
                canCreate = true,
                canManage = true,
                canEditSelection = true,
                canEditMeetingTimes = true,
            ),
        ),
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDiscard = {},
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
            permissions = CongregationPermissions(
                canCreate = true,
                canManage = true,
                canEditSelection = true,
                canEditMeetingTimes = true,
            ),
        ),
        onCreate = {},
        onSelect = {},
        onUpdate = {},
        onSave = {},
        onCancel = {},
        onDiscard = {},
        onDelete = {},
    )
}
