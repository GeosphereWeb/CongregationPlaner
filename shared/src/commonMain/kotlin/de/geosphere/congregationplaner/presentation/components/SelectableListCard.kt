package de.geosphere.congregationplaner.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Listeneintrag einer Listen-/Detailansicht: hervorgehoben, wenn [selected], und mit Löschen-Aktion,
 * sofern [canDelete]. Der fachliche Inhalt kommt aus [content].
 */
@Composable
@Suppress("LongParameterList")
internal fun SelectableListCard(
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
    content: @Composable ColumnScope.() -> Unit,
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
            content()
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
private fun SelectableListCardPreview() = PreviewThemeWrapper {
    SelectableListCard(selected = true, onSelect = {}, onDelete = {}, actionsEnabled = true, canDelete = true) {
        Text("Eintrag", style = MaterialTheme.typography.titleMedium)
    }
}
