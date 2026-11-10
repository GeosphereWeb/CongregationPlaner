package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
internal fun ExternalDirectoryTree(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    listState: LazyListState,
) {
    val directory = state.visibleDirectory
    val editor = state.editor
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (directory.circuits.isEmpty()) {
            item {
                Text(if (state.filter.isActive) "Keine Einträge für diesen Filter." else "Noch keine Kreise angelegt.")
            }
        }
        directory.circuits.forEach { circuit ->
            stickyHeader(key = "circuit-${circuit.id}") {
                SelectableListCard(
                    selected = editor is ExternalDirectoryEditor.Circuit && editor.id == circuit.id,
                    onSelect = { actions.onSelectCircuit(circuit) },
                    onDelete = {},
                    actionsEnabled = !isBusy,
                    canDelete = false,
                    accent = NightColors.Lavender,
                ) {
                    Text("Kreis ${circuit.name}", style = MaterialTheme.typography.titleMedium)
                    Text(congregationCountLabel(directory.congregationsIn(circuit.id).size))
                }
            }
            directory.congregationsIn(circuit.id).forEach { congregation ->
                stickyHeader(key = "congregation-${congregation.id}") {
                    SelectableListCard(
                        selected = editor is ExternalDirectoryEditor.Congregation && editor.id == congregation.id,
                        onSelect = { actions.onSelectCongregation(congregation) },
                        onDelete = {},
                        actionsEnabled = !isBusy,
                        canDelete = false,
                        accent = NightColors.Sky,
                    ) {
                        Text("Versammlung ${congregation.name}", style = MaterialTheme.typography.titleSmall)
                        Text(speakerCountLabel(directory.speakersIn(congregation.id).size))
                    }
                }
                directory.speakersIn(congregation.id).forEach { speaker ->
                    item(key = "speaker-${speaker.id}") {
                        SelectableListCard(
                            selected = editor is ExternalDirectoryEditor.Speaker && editor.id == speaker.id,
                            onSelect = { actions.onSelectSpeaker(speaker) },
                            onDelete = {},
                            actionsEnabled = !isBusy,
                            canDelete = false,
                            accent = NightColors.Mint,
                        ) {
                            Text(speaker.displayName())
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ExternalDirectoryTreePreview() = PreviewThemeWrapper {
    ExternalDirectoryTree(
        previewDirectoryState,
        ExternalDirectoryActions(),
        isBusy = false,
        listState = rememberLazyListState(),
    )
}
