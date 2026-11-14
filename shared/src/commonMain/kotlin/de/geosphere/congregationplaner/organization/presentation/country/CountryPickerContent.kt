package de.geosphere.congregationplaner.organization.presentation.country

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.organization.domain.model.CountryOption
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Auswahl eines bestehenden Landes; zeigt Laden und Ladefehler mit „Erneut versuchen“. */
@Composable
internal fun CountryPicker(
    state: CountryChoiceState,
    onSelect: (CountryRef) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Land", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled && state.countries.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text(state.selectedOption?.displayName ?: "Land wählen") }
            if (state.isLoading) CircularProgressIndicator(Modifier.padding(start = 8.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.countries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country.displayName) },
                    onClick = {
                        onSelect(country.ref)
                        expanded = false
                    },
                )
            }
        }
        state.errorMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("Erneut versuchen") }
            }
        }
    }
}

private val previewCountries = listOf(
    CountryOption(CountryRef("EMEA", "europe", "central-europe", "de"), "Deutschland"),
    CountryOption(CountryRef("EMEA", "europe", "central-europe", "at"), "Österreich"),
)

@ThemePreviews
@Composable
private fun CountryPickerPreview() = PreviewThemeWrapper {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CountryPicker(CountryChoiceState(previewCountries, previewCountries[0].ref), {}, {})
        CountryPicker(CountryChoiceState(previewCountries), {}, {})
        CountryPicker(CountryChoiceState(isLoading = true), {}, {})
        CountryPicker(CountryChoiceState(errorMessage = "Die Länder konnten nicht geladen werden."), {}, {})
    }
}
