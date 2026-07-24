package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.customColors

// Wichtig: Importieren Sie Ihre Erweiterung, falls Sie in einem anderen Package arbeiten
// import de.geosphere.congregationplaner.theming.customColors

@Composable
fun StatusCardExample() {
    // 1. Card nutzt das offizielle M3-Farbschema (primaryContainer)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Text nutzt M3 Standard-Schriftfarbe (onPrimaryContainer)
            Text(
                text = "Kongregations-Planer Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. TEXT NUTZT IHRE EIGENE ZUSATZFARBE (brandCustom)
            Text(
                text = "Wichtiger Hinweis mit eigener Markenfarbe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.customColors.brandCustom,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. BUTTON NUTZT IHRE EIGENEN STATUSFARBEN (success & successContainer)
            Button(
                onClick = { /* Aktion */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.customColors.successContainer,
                    contentColor = MaterialTheme.customColors.success,
                ),
            ) {
                Text(text = "Aktion erfolgreich bestätigen")
            }
        }
    }
}

@Preview
@Composable
private fun StatusCardExamplePreview() {
    AppTheme {
        StatusCardExample()
    }
}
