package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetingTimeInput(label: String, time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = time.toInputValue(),
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
        readOnly = true,
        supportingText = { Text("Tippen, um eine Uhrzeit auszuwählen.") },
        trailingIcon = {
            TextButton(onClick = { showTimePicker = true }) {
                Text("Wählen")
            }
        },
        singleLine = true,
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(label) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

private fun LocalTime.toInputValue(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@ThemePreviews
@Composable
private fun MeetingTimeInputPreview() = PreviewThemeWrapper {
    MeetingTimeInput(
        label = "Uhrzeit der Zusammenkunft",
        time = LocalTime(19, 0),
        onTimeChange = {},
    )
}
