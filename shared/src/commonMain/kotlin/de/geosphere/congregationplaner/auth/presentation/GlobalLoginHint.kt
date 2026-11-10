package de.geosphere.congregationplaner.auth.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.header.NightScreen

/** Schritt 2 für globale Admins: Anmeldung ohne Versammlung, mit dem Weg zurück zur Auswahl. */
@Composable
internal fun GlobalLoginHint(onChooseCongregation: () -> Unit) {
    Text(
        text = "Globale Admins melden sich ohne Versammlung an.",
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        style = nightTextStyle(13.sp, color = NightColors.OnBackgroundMuted),
    )
    SecondaryButton(
        text = "Doch eine Versammlung wählen",
        onClick = onChooseCongregation,
        modifier = Modifier.fillMaxWidth(),
        outlined = false,
    )
    Spacer(Modifier.height(14.dp))
}

@ThemePreviews
@Composable
private fun GlobalLoginHintPreview() = NightPhonePreview {
    NightScreen(stepLabel = "SCHRITT 2 / 2", current = "Globale Verwaltung") {
        GlobalLoginHint(onChooseCongregation = {})
    }
}
