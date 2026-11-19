package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.EventHorizon
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

private const val HORIZON_HINT =
    "Startseite: Termine mit Wochenereignis von heute an so weit im Voraus zeigen " +
        "(bis ${EventHorizon.MAX_DAYS} Tage oder ${EventHorizon.MAX_MONTHS} Monate)."

/** Zeitraum der Startseite: eine Zahl und „Tage“ oder „Monate“ (ADR-0037). */
@Composable
internal fun EventHorizonInput(horizon: EventHorizon, onChange: (EventHorizon) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Besondere Ereignisse", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = horizon.amountText,
                onValueChange = { onChange(horizon.withAmountText(it)) },
                label = { Text("Zeitraum") },
                isError = !horizon.isValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
            )
            EventHorizon.Unit.entries.forEach { unit ->
                FilterChip(
                    selected = horizon.unit == unit,
                    onClick = { onChange(horizon.copy(unit = unit)) },
                    label = { Text(unit.label) },
                )
            }
        }
        Text(
            HORIZON_HINT,
            style = MaterialTheme.typography.bodySmall,
            color = if (horizon.isValid) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@ThemePreviews
@Composable
private fun EventHorizonInputPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EventHorizonInput(EventHorizon(), onChange = {})
        EventHorizonInput(EventHorizon(400, EventHorizon.Unit.DAYS), onChange = {})
    }
}
