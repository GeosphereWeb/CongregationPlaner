package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.presentation.picker.label
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Filter nach Kreisen und Versammlungen (je mehrere wählbar); jede Auswahl gilt sofort. Ohne Kreis stehen alle
 * Versammlungen zur Wahl; die Suche grenzt sie ab dem zweiten Buchstaben ein.
 */
@Composable
internal fun DirectoryFilterBar(state: ExternalDirectoryUiState, actions: ExternalDirectoryActions) {
    val directory = state.directory
    if (directory.circuits.isEmpty()) return
    val circuits = directory.circuitOptions(state.filter)
    val congregations = directory.congregationOptions(state.filter, state.congregationQuery)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterMenu(
            title = "Kreis",
            summary = directory.circuitFilterSummary(state.filter),
            options = circuits,
            actions = FilterMenuActions(actions.onToggleCircuit, actions.onClearCircuits, "Alle Kreise"),
            modifier = Modifier.weight(1f),
        )
        FilterMenu(
            title = "Versammlung",
            summary = directory.congregationFilterSummary(state.filter),
            options = congregations,
            actions = FilterMenuActions(
                actions.onToggleCongregation,
                actions.onClearCongregations,
                "Alle Versammlungen",
            ),
            query = state.congregationQuery,
            onQueryChange = actions.onSearchCongregations,
            modifier = Modifier.weight(1f),
        )
    }
}

internal class FilterMenuActions(val onToggle: (String) -> Unit, val onClear: () -> Unit, val clearLabel: String)

/** Auswahlfeld mit Häkchen je Eintrag; das Menü bleibt offen, damit man mehrere wählen kann. */
@Composable
internal fun FilterMenu(
    title: String,
    summary: String,
    options: List<FilterOption>,
    actions: FilterMenuActions,
    modifier: Modifier = Modifier,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = {
            expanded = true
        }, modifier = Modifier.fillMaxWidth()) { Text(summary, maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (query != null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Name suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            DropdownMenuItem(text = { Text(actions.clearLabel) }, onClick = actions.onClear)
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Checkbox(checked = option.selected, onCheckedChange = null) },
                    onClick = { actions.onToggle(option.id) },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DirectoryFilterBarPreview() = PreviewThemeWrapper {
    Column {
        DirectoryFilterBar(previewDirectoryState, ExternalDirectoryActions())
        DirectoryFilterBar(
            previewDirectoryState.copy(
                filter = DirectoryFilter(circuitIds = setOf("c1"), congregationIds = setOf("g1")),
            ),
            ExternalDirectoryActions(),
        )
    }
}
