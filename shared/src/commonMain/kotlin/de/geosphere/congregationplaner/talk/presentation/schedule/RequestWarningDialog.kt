package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Rückfrage vor dem Speichern, wenn dabei eine offene Redneranfrage gelöscht würde ([requestReplacementWarning]). */
@Composable
internal fun RequestWarningDialog(warning: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anfrage löschen?") },
        text = { Text(warning) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = NightColors.Coral),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@ThemePreviews
@Composable
private fun RequestWarningDialogPreview() = PreviewThemeWrapper {
    RequestWarningDialog(
        warning = "Die offene Anfrage bei Muster, Max wird gelöscht. Trotzdem speichern?",
        onConfirm = {},
        onDismiss = {},
    )
}
