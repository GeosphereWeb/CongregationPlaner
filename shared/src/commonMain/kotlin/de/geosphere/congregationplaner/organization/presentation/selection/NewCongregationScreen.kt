package de.geosphere.congregationplaner.organization.presentation.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.header.NightScreen
import de.geosphere.congregationplaner.ui.header.PanelTitle

/**
 * Nach der Anmeldung: legt die vorgemerkte Versammlung an, sobald die E-Mail-Adresse bestätigt ist. Davor erklärt der
 * Bildschirm, wie sich die Adresse bestätigen lässt.
 */
@Composable
@Suppress("LongParameterList")
internal fun NewCongregationScreen(
    draft: NewCongregationDraft,
    emailVerified: Boolean,
    isCreating: Boolean,
    errorMessage: String?,
    onCreate: () -> Unit,
    onRefresh: () -> Unit,
    onResend: () -> Unit,
    onChangeName: () -> Unit,
    onSignOut: () -> Unit,
) {
    // Läuft einmal je Entwurf und je Bestätigung; ein Fehlschlag startet nicht von selbst neu.
    LaunchedEffect(emailVerified, draft) { if (emailVerified) onCreate() }
    NightScreen(
        stepLabel = "NEUE VERSAMMLUNG",
        current = draft.name,
        previous = listOf("Kreis ${draft.circuitName}"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (emailVerified) {
                PanelTitle("Versammlung anlegen", "„${draft.name}“ wird für dich angelegt.")
                if (isCreating) Text("Wird angelegt …", style = nightTextStyle(14.sp, color = NightColors.Sky))
                if (errorMessage != null) {
                    Text(errorMessage, style = nightTextStyle(14.sp, color = NightColors.Coral))
                    PrimaryButton("Erneut versuchen", onCreate, Modifier.fillMaxWidth(), enabled = !isCreating)
                    SecondaryButton("Anderen Namen wählen", onChangeName, Modifier.fillMaxWidth(), outlined = false)
                }
            } else {
                PanelTitle(
                    "E-Mail-Adresse bestätigen",
                    "Wir haben dir eine E-Mail geschickt. Öffne den Link darin, dann legen wir „${draft.name}“ an.",
                )
                PrimaryButton("Ich habe bestätigt", onRefresh, Modifier.fillMaxWidth())
                SecondaryButton("E-Mail erneut senden", onResend, Modifier.fillMaxWidth(), outlined = false)
                Text(
                    "Klappt es nicht, melde dich ab und mit derselben E-Mail-Adresse wieder an.",
                    style = nightTextStyle(13.sp, color = NightColors.OnBackgroundMuted),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("Abmelden", onSignOut, Modifier.fillMaxWidth(), outlined = false)
    }
}

@ThemePreviews
@Composable
private fun NewCongregationScreenPreview() = PreviewThemeWrapper {
    NewCongregationScreen(
        draft = NewCongregationDraft(circuitIds = listOf("c1"), circuitName = "Süd", name = "Musterstadt"),
        emailVerified = false,
        isCreating = false,
        errorMessage = null,
        onCreate = {},
        onRefresh = {},
        onResend = {},
        onChangeName = {},
        onSignOut = {},
    )
}
