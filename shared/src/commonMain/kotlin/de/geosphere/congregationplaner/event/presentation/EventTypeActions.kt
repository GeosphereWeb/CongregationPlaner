package de.geosphere.congregationplaner.event.presentation

import de.geosphere.congregationplaner.event.domain.model.EventType

/** Callbacks der Pflege-Oberfläche; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class EventTypeActions(
    val onNew: () -> Unit = {},
    val onEdit: (EventType) -> Unit = {},
    val onEditorChange: (EventTypeEditorState) -> Unit = {},
    val onSave: () -> Unit = {},
    val onRequestDelete: (EventType) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
    val onAdoptBlueprint: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismissInfo: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)
