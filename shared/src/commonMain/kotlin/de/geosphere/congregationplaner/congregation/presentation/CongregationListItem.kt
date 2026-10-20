package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Composable
@Suppress("LongParameterList")
internal fun CongregationListItem(
    congregation: CongregationMetadata,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
) {
    SelectableListCard(selected, onSelect, onDelete, actionsEnabled, canDelete) {
        Text(congregation.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "${weekdayName(congregation.midweekMeetingWeekday)} ${congregation.midweekMeetingTime} · " +
                "${weekdayName(congregation.weekendMeetingWeekday)} ${congregation.weekendMeetingTime}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@ThemePreviews
@Composable
private fun CongregationListItemPreview() = PreviewThemeWrapper {
    CongregationListItem(
        congregation = previewCongregations.first(),
        selected = true,
        onSelect = {},
        onDelete = {},
        actionsEnabled = true,
        canDelete = true,
    )
}
