package de.geosphere.congregationplaner.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Confirm Dialog Komponente
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
                onClick = onConfirm,
                content = {
                    Text(
                        text = confirmText,
                        color = if (isDangerous) Color.Unspecified else Color.Unspecified,
                    )
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = dismissText,
                        color = Color.Unspecified,
                    )
                },
            )
        },
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
