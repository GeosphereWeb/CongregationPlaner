package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onDelete: (CongregationMetadata) -> Unit,
) {
    var congregationToDelete by remember { mutableStateOf<CongregationMetadata?>(null) }

    val isBusy = state.isSaving || state.isDeleting
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        initialDestinationHistory = listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List)),
    )
    val hasEditor = state.editor != null
    LaunchedEffect(hasEditor) {
        if (hasEditor) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        } else if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
            navigator.navigateBack()
        }
    }
    // System-Zurück im Scaffold bedeutet: Bearbeitung abbrechen
    LaunchedEffect(navigator.currentDestination?.pane) {
        if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.List && hasEditor) onCancel()
    }

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
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden) {
                                TextButton(onClick = onCancel, enabled = !isBusy) {
                                    Text("Zurück zur Liste")
                                }
                            }
                            CongregationDetail(
                                congregation = state.editor,
                                canEdit = state.permissions.canEditSelection,
                                isSaving = state.isSaving,
                                isBusy = isBusy,
                                onChange = onUpdate,
                                onSave = onSave,
                                onCancel = onCancel,
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
