package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.presentation.common.dateFromPickerMillis
import de.geosphere.congregationplaner.talk.presentation.common.toPickerMillis
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate

/**
 * Die Stichtage des Planungsassistenten über der Terminliste: Redner bzw. Vorträge, die ab diesem Tag eingeplant sind,
 * werden nicht vorgeschlagen. Ein Tipp öffnet den Datumswähler; dort lässt sich der Stichtag auch entfernen.
 */
@Composable
internal fun PlanningCutoffRow(
    settings: TalkPlanningSettings,
    onSpeakerFreeSinceChange: (LocalDate?) -> Unit,
    onTalkFreeSinceChange: (LocalDate?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CutoffChip(
            cutoffLabel("Redner", settings.speakerFreeSince),
            settings.speakerFreeSince,
            onSpeakerFreeSinceChange,
        )
        CutoffChip(cutoffLabel("Vorträge", settings.talkFreeSince), settings.talkFreeSince, onTalkFreeSinceChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CutoffChip(label: String, date: LocalDate?, onChange: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    AssistChip(onClick = { showPicker = true }, label = { Text(label, fontSize = 11.sp) })
    if (showPicker) {
        // Das Datum wird als UTC-Mitternacht gelesen und geschrieben, damit die Zeitzone das Datum nicht verschiebt.
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(dateFromPickerMillis(it)) }
                        showPicker = false
                    },
                ) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onChange(null)
                        showPicker = false
                    },
                ) { Text("Kein Stichtag") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@ThemePreviews
@Composable
private fun PlanningCutoffRowPreview() = PreviewThemeWrapper {
    PlanningCutoffRow(
        settings = TalkPlanningSettings(speakerFreeSince = LocalDate(2025, 11, 1)),
        onSpeakerFreeSinceChange = {},
        onTalkFreeSinceChange = {},
    )
}
