package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
internal fun CongregationListPane(
    congregations: List<CongregationMetadata>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (CongregationMetadata) -> Unit,
    onDelete: (CongregationMetadata) -> Unit,
    actionsEnabled: Boolean,
) {
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Versammlungen", style = MaterialTheme.typography.titleMedium)
            }
            if (congregations.isEmpty()) {
                item {
                    Text("Noch keine Versammlungen vorhanden.")
                }
            } else {
                items(congregations, key = { congregation -> congregation.id }) { congregation ->
                    CongregationListItem(
                        congregation = congregation,
                        selected = selectedId == congregation.id,
                        onSelect = { onSelect(congregation) },
                        onDelete = { onDelete(congregation) },
                        actionsEnabled = actionsEnabled,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CongregationListPanePreview() = PreviewThemeWrapper {
    CongregationListPane(
        congregations = previewCongregations,
        selectedId = previewCongregations.first().id,
        onSelect = {},
        onDelete = {},
        actionsEnabled = true,
    )
}
