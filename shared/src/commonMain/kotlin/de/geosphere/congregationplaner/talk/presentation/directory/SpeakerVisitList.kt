package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.SpeakerVisits
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/** Alle gefilterten Redner in einer Liste, wer am längsten nicht da war zuerst; zum Einladen (CP-193). */
@Composable
internal fun SpeakerVisitList(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    listState: LazyListState,
    today: LocalDate,
) {
    val editor = state.editor
    val speakers = state.speakersByLastVisit
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (speakers.isEmpty()) {
            item { Text(if (state.isFiltered) "Keine Redner für diesen Filter." else "Noch keine Redner angelegt.") }
        }
        items(speakers, key = { "visit-${it.id}" }) { speaker ->
            SpeakerRow(
                speaker = speaker,
                badge = state.speakerBadge(speaker.id, today),
                selected = editor is ExternalDirectoryEditor.Speaker && editor.id == speaker.id,
                isBusy = isBusy,
                onSelect = { actions.onSelectSpeaker(speaker) },
                congregation = state.congregationName(speaker),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SpeakerVisitListPreview() = PreviewThemeWrapper {
    SpeakerVisitList(
        state = previewDirectoryState.copy(
            visits = mapOf("s1" to SpeakerVisits(LocalDate(2027, 1, 10), 3)),
            sortByLastVisit = true,
        ),
        actions = ExternalDirectoryActions(),
        isBusy = false,
        listState = rememberLazyListState(),
        today = previewToday,
    )
}
