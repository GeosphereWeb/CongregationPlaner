package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CongregationDetail(
    congregation: CongregationMetadata?,
    isSaving: Boolean,
    isBusy: Boolean,
    onChange: (CongregationMetadata) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    if (congregation == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Wähle links eine Versammlung aus oder lege eine neue an.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        CongregationEditor(
            congregation = congregation,
            isSaving = isSaving,
            isBusy = isBusy,
            onChange = onChange,
            onSave = onSave,
            onCancel = onCancel,
        )
    }
}

@ThemePreviews
@Composable
private fun CongregationDetailPreview() = PreviewThemeWrapper {
    CongregationDetail(
        congregation = previewCongregations.first(),
        isSaving = false,
        isBusy = false,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
