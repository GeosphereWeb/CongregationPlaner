package de.geosphere.congregationplaner.talk.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.presentation.common.RecencyLegend
import de.geosphere.congregationplaner.talk.presentation.common.RecencyTile
import de.geosphere.congregationplaner.talk.presentation.common.tileDescription
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/** Heatmap: alle Vorträge als Kacheln in der Farbe „wie lange her“; ein Tipp öffnet den Vortrag (CP-191). */
@Composable
internal fun TalkHistoryTiles(
    state: TalkHistoryUiState,
    actions: TalkHistoryActions,
    gridState: LazyGridState,
    today: LocalDate,
) {
    if (state.rows.isEmpty()) {
        Text("Noch keine Vorträge in der Liste.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 52.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "legend", span = { GridItemSpan(maxLineSpan) }) { RecencyLegend() }
        items(state.rows, key = { it.talk.number }) { row ->
            val badge = row.badge(today)
            RecencyTile(
                number = row.talk.number,
                level = badge.level,
                description = badge.tileDescription(row.talk.number),
                dimmed = isInactive(row.talk),
                selected = state.selectedNumber == row.talk.number,
                onClick = { actions.onSelect(row.talk) },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TalkHistoryTilesPreview() = PreviewThemeWrapper {
    val talks = (1..12).map { Talk(it, "Vortrag $it", active = it != 7) }
    val days = listOf(
        LocalDate(2026, 12, 6),
        LocalDate(2025, 3, 2),
        LocalDate(2023, 6, 4),
        LocalDate(2021, 11, 2),
        LocalDate(2018, 5, 6),
    )
    TalkHistoryContent(
        state = previewState(
            talks = talks,
            history = days.mapIndexed { index, day -> TalkHistoryEntry(index * 2 + 1, listOf(day)) },
            isLoading = false,
            canView = true,
            selectedNumber = 3,
            showTiles = true,
        ),
        actions = TalkHistoryActions(),
        today = previewToday,
    )
}
