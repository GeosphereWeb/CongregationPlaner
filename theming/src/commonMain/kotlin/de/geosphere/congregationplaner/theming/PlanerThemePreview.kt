package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Sample screen for reviewing the sober color schemes; shows typical building blocks with static example data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanerThemeSample() {
    Surface(modifier = Modifier.width(390.dp), color = MaterialTheme.colorScheme.background) {
        Column {
            TopAppBar(title = { Text("Wochenplan") })
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Nächste Zusammenkunft",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Donnerstag, 27. November · 19:00",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Text("Aufgaben", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    SampleAssignment("Vorsitz", "Max Mustermann", confirmed = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SampleAssignment("Gebet", "Peter Beispiel", confirmed = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SampleAssignment("Lesung", "Jonas Muster", confirmed = true)
                }

                OutlinedTextField(
                    value = "Öffentlicher Vortrag",
                    onValueChange = {},
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Redner") },
                    isError = true,
                    supportingText = { Text("Bitte einen Redner auswählen") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Erinnerungen", modifier = Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = {})
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nur eigene Aufgaben (deaktiviert)",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                    Switch(checked = false, onCheckedChange = {}, enabled = false)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Speichern") }
                    OutlinedButton(onClick = {}) { Text("Abbrechen") }
                    TextButton(onClick = {}) { Text("Löschen") }
                }
                Spacer(Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SampleAssignment(role: String, name: String, confirmed: Boolean) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(name) },
        overlineContent = { Text(role) },
        trailingContent = {
            AssistChip(
                onClick = {},
                label = { Text(if (confirmed) "Bestätigt" else "Offen") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (confirmed) colors.tertiaryContainer else Color.Transparent,
                    labelColor = if (confirmed) colors.onTertiaryContainer else colors.onSurfaceVariant,
                ),
                border = if (confirmed) null else AssistChipDefaults.assistChipBorder(enabled = true),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Preview(name = "Planer – hell")
@Composable
private fun PlanerThemeLightPreview() {
    MaterialTheme(colorScheme = planerLightColors) { PlanerThemeSample() }
}

@Preview(name = "Planer – dunkel")
@Composable
private fun PlanerThemeDarkPreview() {
    MaterialTheme(colorScheme = planerDarkColors) { PlanerThemeSample() }
}

@Preview(name = "Planer Lila – hell")
@Composable
private fun PlanerThemePurpleLightPreview() {
    MaterialTheme(colorScheme = planerPurpleLightColors) { PlanerThemeSample() }
}

@Preview(name = "Planer Lila – dunkel")
@Composable
private fun PlanerThemePurpleDarkPreview() {
    MaterialTheme(colorScheme = planerPurpleDarkColors) { PlanerThemeSample() }
}
