package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CircuitListItem(
    circuit: Circuit,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
) {
    SelectableListCard(selected, onSelect, onDelete, actionsEnabled, canDelete) {
        Text("${circuit.id} · ${circuit.name}", style = MaterialTheme.typography.titleMedium)
        if (circuit.overseerName.isNotBlank()) {
            Text("Kreisaufseher: ${circuit.overseerName}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@ThemePreviews
@Composable
private fun CircuitListItemPreview() = PreviewThemeWrapper {
    CircuitListItem(
        circuit = previewCircuits.first(),
        selected = true,
        onSelect = {},
        onDelete = {},
        actionsEnabled = true,
        canDelete = true,
    )
}
