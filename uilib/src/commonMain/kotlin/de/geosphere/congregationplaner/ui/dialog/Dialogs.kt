package de.geosphere.congregationplaner.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.theming.AppColors
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton

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
    isDangerous: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = AppColors.OnBackground
            )
        },
        text = {
            Text(
                text = message,
                color = AppColors.OnBackground
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                content = {
                    Text(
                        text = confirmText,
                        color = if (isDangerous) AppColors.Error else AppColors.Primary
                    )
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = dismissText,
                        color = AppColors.Primary
                    )
                }
            )
        }
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = AppColors.OnBackground
            )
        },
        text = {
            Text(
                text = message,
                color = AppColors.OnBackground
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = buttonText,
                        color = AppColors.Primary
                    )
                }
            )
        }
    )
}
