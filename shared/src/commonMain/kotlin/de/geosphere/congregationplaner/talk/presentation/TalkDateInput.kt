package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TalkDateInput(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.toDisplayText(),
        onValueChange = {},
        label = { Text("Datum") },
        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
        readOnly = true,
        supportingText = { Text("Tippen, um ein Datum auszuwählen.") },
        trailingIcon = { TextButton(onClick = { showDatePicker = true }) { Text("Wählen") } },
        singleLine = true,
    )

    if (showDatePicker) {
        // Das Datum wird als UTC-Mitternacht gelesen und geschrieben, damit die Zeitzone das Datum nicht verschiebt.
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private val WEEKDAY_SHORT_NAMES = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

/** Deutsche Anzeige mit Wochentag, z. B. `So, 25.10.2026`. */
internal fun LocalDate.toDisplayText(): String {
    val dayText = day.toString().padStart(2, '0')
    val monthText = month.number.toString().padStart(2, '0')
    return "${WEEKDAY_SHORT_NAMES[dayOfWeek.isoDayNumber - 1]}, $dayText.$monthText.$year"
}
