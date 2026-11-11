package de.geosphere.congregationplaner.congregation.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata

/** Callbacks der Versammlungsdaten; gebündelt, damit die Composables überschaubare Signaturen behalten. */
data class CongregationMetadataActions(
    val onCreate: () -> Unit = {},
    val onSelect: (CongregationMetadata) -> Unit = {},
    val onUpdate: (CongregationMetadata) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onDelete: (CongregationMetadata) -> Unit = {},
)
