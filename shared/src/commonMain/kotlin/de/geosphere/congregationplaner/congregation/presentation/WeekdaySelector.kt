package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
internal fun WeekdaySelector(label: String, weekday: Int, onWeekdayChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(weekdayName(weekday))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WEEKDAYS.forEach { (number, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onWeekdayChange(number)
                        expanded = false
                    },
                )
            }
        }
    }
}

internal fun weekdayName(weekday: Int): String = WEEKDAYS.firstOrNull { it.first == weekday }?.second ?: "Ungültig"

private val WEEKDAYS = listOf(
    1 to "Montag",
    2 to "Dienstag",
    3 to "Mittwoch",
    4 to "Donnerstag",
    5 to "Freitag",
    6 to "Samstag",
    7 to "Sonntag",
)

@ThemePreviews
@Composable
private fun WeekdaySelectorPreview() = PreviewThemeWrapper {
    WeekdaySelector(
        label = "Wochentag der Zusammenkunft",
        weekday = 3,
        onWeekdayChange = {},
    )
}
