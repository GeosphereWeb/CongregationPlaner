package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun CircuitScreen(viewModel: CircuitViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    CircuitContent(
        state = state,
        modifier = modifier,
        onCreate = viewModel::createCircuit,
        onSelect = viewModel::editCircuit,
        onUpdate = viewModel::updateEditor,
        onSave = viewModel::saveCircuit,
        onCancel = viewModel::cancelEditing,
        onDelete = viewModel::deleteCircuit,
    )
}
