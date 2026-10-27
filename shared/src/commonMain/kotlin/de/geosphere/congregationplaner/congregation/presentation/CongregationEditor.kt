package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CongregationEditor(
    congregation: CongregationMetadata,
    isSaving: Boolean,
    isBusy: Boolean,
    isDirty: Boolean,
    onChange: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (congregation.id.isBlank()) "Neue Versammlung" else "Versammlung bearbeiten",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = congregation.name,
                onValueChange = { onChange(congregation.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "Zeiten ${rememberCurrentYear()} (gelten auch für Jahre ohne Abweichung)",
                style = MaterialTheme.typography.titleSmall,
            )
            WeekdaySelector(
                label = "Wochentag der Zusammenkunft unter der Woche",
                weekday = congregation.midweekMeetingWeekday,
                onWeekdayChange = { onChange(congregation.copy(midweekMeetingWeekday = it)) },
            )
            MeetingTimeInput(
                label = "Uhrzeit der Zusammenkunft unter der Woche",
                time = congregation.midweekMeetingTime,
                onTimeChange = { onChange(congregation.copy(midweekMeetingTime = it)) },
            )
            WeekdaySelector(
                label = "Wochentag der Wochenend-Zusammenkunft",
                weekday = congregation.weekendMeetingWeekday,
                onWeekdayChange = { onChange(congregation.copy(weekendMeetingWeekday = it)) },
            )
            MeetingTimeInput(
                label = "Uhrzeit der Wochenend-Zusammenkunft",
                time = congregation.weekendMeetingTime,
                onTimeChange = { onChange(congregation.copy(weekendMeetingTime = it)) },
            )
            YearlyMeetingTimesEditor(congregation, onChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isBusy && isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isBusy) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CongregationEditorPreview() = PreviewThemeWrapper {
    CongregationEditor(
        congregation = previewCongregations.first(),
        isSaving = false,
        isBusy = false,
        isDirty = true,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
