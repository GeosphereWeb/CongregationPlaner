package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.pane.DetailPane
import de.geosphere.congregationplaner.ui.pane.ListPaneHeader

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
internal fun CongregationMetadataContent(
    state: CongregationMetadataUiState,
    actions: CongregationMetadataActions,
    modifier: Modifier = Modifier,
) {
    var congregationToDelete by remember { mutableStateOf<CongregationMetadata?>(null) }

    val isBusy = state.isBusy
    val navigator = rememberListDetailNavigation(state, onClose = actions.onCancel)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) actions.onCancel else actions.onDiscard

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ListPaneHeader("Versammlung Metadaten")

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
                                onSelect = actions.onSelect,
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
                                onBack = actions.onCancel,
                                backEnabled = !isBusy,
                            ) {
                                CongregationDetail(
                                    congregation = state.editor,
                                    canEdit = state.permissions.canEditSelection,
                                    canEditMeetingTimes = state.permissions.canEditMeetingTimes,
                                    isSaving = state.isSaving,
                                    isBusy = isBusy,
                                    isDirty = state.isEditorDirty,
                                    onChange = actions.onUpdate,
                                    onSave = actions.onSave,
                                    onCancel = onCancelEditing,
                                )
                            }
                        }
                    },
                )
            }
        }
        // On a phone the editor covers the list; the FAB would overlay it there.
        if (state.permissions.canCreate && (!singlePane || state.editor == null)) {
            FloatingActionButton(
                onClick = { if (!state.isLoading && !isBusy) actions.onCreate() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .semantics { contentDescription = "Versammlung hinzufügen" },
            ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        }
    }

    congregationToDelete?.let { congregation ->
        DeleteCongregationDialog(
            congregation = congregation,
            isDeleting = state.isDeleting,
            enabled = !state.isBusy,
            onConfirm = {
                actions.onDelete(congregation)
                congregationToDelete = null
            },
            onDismiss = { congregationToDelete = null },
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
        actions = CongregationMetadataActions(),
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
        actions = CongregationMetadataActions(),
    )
}
