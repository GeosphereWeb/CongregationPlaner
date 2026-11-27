package de.geosphere.congregationplaner.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.DangerButton

/**
 * Rückfrage mit Bestätigen und Abbrechen. Mit [isDangerous] ist Bestätigen ein roter [DangerButton] (z. B. Löschen);
 * [enabled] = `false` sperrt beide Knöpfe und das Wegtippen, solange etwas läuft.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String = "Abbrechen",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDangerous: Boolean = false,
    enabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (isDangerous) {
                DangerButton(confirmText, onConfirm, enabled = enabled)
            } else {
                TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmText) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text(dismissText) } },
    )
}

/**
 * Simple Info Dialog
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    buttonText: String = "OK",
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.Unspecified,
            )
        },
        text = {
            Text(
                text = message,
                color = Color.Unspecified,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = buttonText,
                        color = Color.Unspecified,
                    )
                },
            )
        },
    )
}

@ThemePreviews
@Composable
private fun ConfirmDialogPreview() = PreviewThemeWrapper {
    ConfirmDialog(
        title = "Versammlung löschen?",
        message = "Diese Versammlung wird dauerhaft gelöscht.",
        confirmText = "Löschen",
        onConfirm = {},
        onDismiss = {},
        isDangerous = true,
    )
}

@ThemePreviews
@Composable
private fun ConfirmDialogDisabledPreview() = PreviewThemeWrapper {
    ConfirmDialog(
        title = "Versammlung löschen?",
        message = "Diese Versammlung wird dauerhaft gelöscht.",
        confirmText = "Löscht …",
        onConfirm = {},
        onDismiss = {},
        isDangerous = true,
        enabled = false,
    )
}

@ThemePreviews
@Composable
private fun InfoDialogPreview() = PreviewThemeWrapper {
    InfoDialog(
        title = "Änderungen gespeichert",
        message = "Die Versammlungsdaten wurden erfolgreich gespeichert.",
        onDismiss = {},
    )
}
