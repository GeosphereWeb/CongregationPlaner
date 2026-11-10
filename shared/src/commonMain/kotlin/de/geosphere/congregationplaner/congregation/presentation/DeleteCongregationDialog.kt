package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Rückfrage vor dem Löschen; während gespeichert oder gelöscht wird, ist sie gesperrt. */
@Composable
internal fun DeleteCongregationDialog(
    congregation: CongregationMetadata,
    isDeleting: Boolean,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text("Versammlung löschen?") },
        text = { Text("„${congregation.name}“ wird dauerhaft gelöscht.") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(if (isDeleting) "Löscht …" else "Löschen") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text("Abbrechen") } },
    )
}

@ThemePreviews
@Composable
private fun DeleteCongregationDialogPreview() = PreviewThemeWrapper {
    DeleteCongregationDialog(previewCongregations.first(), isDeleting = false, enabled = true, onConfirm = {
    }, onDismiss = {})
}
