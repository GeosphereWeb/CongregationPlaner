package de.geosphere.congregationplaner.talk.presentation.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.PlanningUsage
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/**
 * Auswahl aus der Vortragsliste mit Suche nach Nummer oder Titel; nicht aktive Vorträge sind nicht wählbar.
 * Vorgeschlagen werden zuerst die Vorträge, die am längsten nicht eingeplant sind; Vorträge, die der Stichtag aus
 * [usage] sperrt, lassen sich einblenden. Steht der Vortrag nicht in der Liste, trägt [onFreeEntry] den Suchtext als
 * Titel ohne Nummer ein.
 */
@Composable
internal fun TalkPickerDialog(
    talks: List<Talk>,
    onSelect: (Talk) -> Unit,
    onDismiss: () -> Unit,
    onFreeEntry: (String) -> Unit = {},
    usage: PlanningUsage = PlanningUsage(),
) {
    var query by remember { mutableStateOf("") }
    var showBlocked by remember { mutableStateOf(false) }
    val matches = remember(talks, query) { talks.matching(query) }
    val suggestions = remember(matches, usage, showBlocked) { matches.suggested(usage, showBlocked) }
    val blockedCount = remember(matches, usage) { matches.count { usage.isTalkBlocked(it.number) } }
    val freeTitle = freeEntryOf(query, TalkSlot.MAX_TALK_TITLE_LENGTH)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vortrag wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Nummer oder Titel suchen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (blockedCount > 0) ShowBlockedToggle(blockedCount, showBlocked) { showBlocked = it }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (suggestions.isEmpty()) item { Text("Kein passender freier Vortrag gefunden.") }
                    items(suggestions, key = { it.item.number }) { suggestion ->
                        SuggestionRow(suggestion.item.label(), suggestion.note, suggestion.blocked) {
                            onSelect(suggestion.item)
                        }
                    }
                }
            }
        },
        confirmButton = {
            freeTitle?.let { title -> TextButton(onClick = { onFreeEntry(title) }) { Text("„$title“ frei eintragen") } }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@ThemePreviews
@Composable
private fun TalkPickerDialogPreview() = PreviewThemeWrapper {
    TalkPickerDialog(
        talks = listOf(Talk(1, "Beispielvortrag"), Talk(2, "Weiterer Vortrag"), Talk(3, "Pausiert", active = false)),
        onSelect = {},
        onDismiss = {},
        usage = PlanningUsage(
            settings = TalkPlanningSettings(talkFreeSince = LocalDate(2024, 1, 1)),
            talkLastPlanned = mapOf(1 to LocalDate(2026, 3, 1), 2 to LocalDate(2023, 5, 7)),
        ),
    )
}
