package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    canEdit: Boolean,
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
    } else if (!canEdit) {
        CongregationSummary(congregation)
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

@Composable
private fun CongregationSummary(congregation: CongregationMetadata) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(congregation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Unter der Woche: ${weekdayName(congregation.midweekMeetingWeekday)} " +
                    "${congregation.midweekMeetingTime}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Wochenende: ${weekdayName(congregation.weekendMeetingWeekday)} ${congregation.weekendMeetingTime}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CongregationDetailReadOnlyPreview() = PreviewThemeWrapper {
    CongregationDetail(
        congregation = previewCongregations.first(),
        canEdit = false,
        isSaving = false,
        isBusy = false,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}

@ThemePreviews
@Composable
private fun CongregationDetailPreview() = PreviewThemeWrapper {
    CongregationDetail(
        congregation = previewCongregations.first(),
        canEdit = true,
        isSaving = false,
        isBusy = false,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
