package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("${circuit.id} · ${circuit.name}", style = MaterialTheme.typography.titleMedium)
            if (circuit.overseerName.isNotBlank()) {
                Text("Kreisaufseher: ${circuit.overseerName}", style = MaterialTheme.typography.bodyMedium)
            }
            if (canDelete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDelete, enabled = actionsEnabled) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
