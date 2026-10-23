package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun CongregationMetadataScreen(viewModel: CongregationMetadataViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    CongregationMetadataContent(
        state = state,
        modifier = modifier,
        onCreate = viewModel::createCongregation,
        onSelect = viewModel::editCongregation,
        onUpdate = viewModel::updateEditor,
        onSave = viewModel::saveCongregation,
        onCancel = viewModel::cancelEditing,
        onDiscard = viewModel::discardChanges,
        onDelete = viewModel::deleteCongregation,
    )
}
