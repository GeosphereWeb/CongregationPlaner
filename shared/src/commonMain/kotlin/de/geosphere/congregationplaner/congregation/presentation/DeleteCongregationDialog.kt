package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.dialog.ConfirmDialog

/** Rückfrage vor dem Löschen; während gespeichert oder gelöscht wird, ist sie gesperrt. */
@Composable
internal fun DeleteCongregationDialog(
    congregation: CongregationMetadata,
    isDeleting: Boolean,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = "Versammlung löschen?",
        message = "„${congregation.name}“ wird dauerhaft gelöscht.",
        confirmText = if (isDeleting) "Löscht …" else "Löschen",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDangerous = true,
        enabled = enabled,
    )
}

@ThemePreviews
@Composable
private fun DeleteCongregationDialogPreview() = PreviewThemeWrapper {
    DeleteCongregationDialog(previewCongregations.first(), isDeleting = false, enabled = true, onConfirm = {
    }, onDismiss = {})
}
