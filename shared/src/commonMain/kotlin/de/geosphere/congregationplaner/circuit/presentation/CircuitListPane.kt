package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
internal fun CircuitListPane(
    circuits: List<Circuit>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (Circuit) -> Unit,
    onDelete: (Circuit) -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
) {
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Kreise", style = MaterialTheme.typography.titleMedium)
            }
            if (circuits.isEmpty()) {
                item {
                    Text("Noch keine Kreise vorhanden.")
                }
            } else {
                items(circuits, key = { circuit -> circuit.id }) { circuit ->
                    CircuitListItem(
                        circuit = circuit,
                        selected = selectedId == circuit.id,
                        onSelect = { onSelect(circuit) },
                        onDelete = { onDelete(circuit) },
                        actionsEnabled = actionsEnabled,
                        canDelete = canDelete,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CircuitListPanePreview() = PreviewThemeWrapper {
    CircuitListPane(
        circuits = previewCircuits,
        selectedId = previewCircuits.first().id,
        onSelect = {},
        onDelete = {},
        actionsEnabled = true,
        canDelete = true,
    )
}
