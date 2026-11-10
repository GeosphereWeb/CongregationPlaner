package de.geosphere.congregationplaner.organization.presentation.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.header.NightScreen
import de.geosphere.congregationplaner.ui.header.PanelTitle

/** Für bereits angemeldete Nutzer, deren Versammlung noch nicht feststeht (z. B. nach einem Update). */
@Composable
fun CongregationRequiredScreen(onSignOut: () -> Unit, selector: @Composable () -> Unit) {
    NightScreen(stepLabel = "ZUGANG", current = "Versammlung wählen") {
        PanelTitle("Versammlung wählen", "Wähle deine Versammlung, um die App zu nutzen.")
        Spacer(Modifier.height(14.dp))
        selector()
        Spacer(Modifier.weight(1f))
        SecondaryButton(text = "Abmelden", onClick = onSignOut, modifier = Modifier.fillMaxWidth(), outlined = false)
    }
}

@ThemePreviews
@Composable
private fun CongregationRequiredScreenPreview() = NightPhonePreview {
    CongregationRequiredScreen(onSignOut = {}) { Text("Auswahl der Versammlung") }
}
