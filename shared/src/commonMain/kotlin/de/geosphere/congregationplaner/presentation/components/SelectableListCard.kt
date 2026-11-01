package de.geosphere.congregationplaner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Listeneintrag einer Listen-/Detailansicht: hervorgehoben, wenn [selected], und mit Löschen-Aktion,
 * sofern [canDelete]. Der fachliche Inhalt kommt aus [content].
 */
@Composable
@Suppress("LongParameterList")
internal fun SelectableListCard(
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) NightColors.SurfaceActive else NightColors.Surface)
            .border(1.dp, if (selected) NightColors.Lavender else NightColors.Border, shape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
        if (canDelete) {
            TextButton(onClick = onDelete, enabled = actionsEnabled) {
                Text("Löschen", color = NightColors.Coral)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SelectableListCardPreview() = PreviewThemeWrapper {
    SelectableListCard(selected = true, onSelect = {}, onDelete = {}, actionsEnabled = true, canDelete = true) {
        Text("Eintrag", style = MaterialTheme.typography.titleMedium)
    }
}
