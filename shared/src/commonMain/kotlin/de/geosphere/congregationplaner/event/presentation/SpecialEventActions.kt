package de.geosphere.congregationplaner.event.presentation

import de.geosphere.congregationplaner.event.domain.model.SpecialEvent

/** Callbacks der Pflege-Oberfläche; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class SpecialEventActions(
    val onNew: () -> Unit = {},
    val onEdit: (SpecialEvent) -> Unit = {},
    val onEditorChange: (SpecialEventEditorState) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (SpecialEvent) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)
