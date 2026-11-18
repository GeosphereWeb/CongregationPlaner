package de.geosphere.congregationplaner.event.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import org.jetbrains.compose.resources.painterResource

/** Auswahl eines Icons aus dem App-Satz mit Suche; [selected] ist markiert. */
@Composable
internal fun EventIconPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    icons: List<String> = eventIconNames,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(icons, query) { icons.matchingIcons(query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Icon wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Suchen, z. B. „arena“") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (matches.isEmpty()) Text("Kein Icon gefunden.")
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(matches, key = { it }) { name ->
                        IconCell(name, name == selected) { onSelect(name) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun IconCell(name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier.size(56.dp)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(role = Role.Button, onClickLabel = iconLabel(name), onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(eventTypeIcon(name)), contentDescription = iconLabel(name))
    }
}

@ThemePreviews
@Composable
private fun EventIconPickerDialogPreview() = PreviewThemeWrapper {
    EventIconPickerDialog(
        selected = "ic_cp_arena_duo",
        onSelect = {},
        onDismiss = {},
        icons = listOf("ic_cp_arena_duo", "ic_cp_wine_bread_duo", "ic_cp_persons_doorstep_duo", "ic_cp_square"),
    )
}
