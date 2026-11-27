package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.presentation.common.RecencyBadge
import de.geosphere.congregationplaner.talk.presentation.common.RecencyChip
import de.geosphere.congregationplaner.talk.presentation.common.RecencyStripe
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.list.SelectableListCard
import kotlinx.datetime.LocalDate

@Composable
internal fun ExternalDirectoryTree(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    listState: LazyListState,
    today: LocalDate,
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
                Text(if (state.isFiltered) "Keine Einträge für diesen Filter." else "Noch keine Kreise angelegt.")
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
                        SpeakerRow(
                            speaker = speaker,
                            badge = state.speakerBadge(speaker.id, today),
                            selected = editor is ExternalDirectoryEditor.Speaker && editor.id == speaker.id,
                            isBusy = isBusy,
                            onSelect = { actions.onSelectSpeaker(speaker) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ein Redner der Liste; ein von der Planung ausgeschlossener trägt einen Hinweis und steht blass. Mit [badge] zeigt
 * die Zeile Streifen und Chip „wie lange nicht da“ (CP-193), mit [congregation] die Versammlung darunter.
 */
@Composable
internal fun SpeakerRow(
    speaker: VisitingSpeaker,
    badge: RecencyBadge?,
    selected: Boolean,
    isBusy: Boolean,
    onSelect: () -> Unit,
    congregation: String? = null,
) {
    SelectableListCard(
        selected = selected,
        onSelect = onSelect,
        onDelete = {},
        actionsEnabled = !isBusy,
        canDelete = false,
        accent = NightColors.Mint,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).alpha(speakerRowAlpha(speaker)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            badge?.let { RecencyStripe(it.level) }
            Column(Modifier.weight(1f)) {
                Text(speaker.displayName())
                congregation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (speaker.excludedFromPlanning) {
                    Text(
                        EXCLUDED_FROM_PLANNING_LABEL,
                        style = MaterialTheme.typography.bodySmall,
                        color = NightColors.OnBackgroundMuted,
                    )
                }
            }
            badge?.let { RecencyChip(it) }
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
        today = previewToday,
    )
}
