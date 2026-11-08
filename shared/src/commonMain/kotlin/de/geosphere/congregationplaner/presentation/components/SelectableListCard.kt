package de.geosphere.congregationplaner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Listeneintrag einer Listen-/Detailansicht: hervorgehoben, wenn [selected], und mit Löschen-Icon rechts,
 * sofern [canDelete]; mit [onEdit] steht davor ein Bearbeiten-Icon (wenn ein Tipp auf den Eintrag etwas anderes
 * tut). [accent] färbt einen Streifen links, z. B. je Ebene einer Hierarchie. Die Aktionen stehen neben dem Inhalt,
 * damit die Einträge niedrig bleiben. Der fachliche Inhalt kommt aus
 * [content].
 */
@Composable
@Suppress("LongParameterList")
internal fun SelectableListCard(
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    canDelete: Boolean,
    onEdit: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) NightColors.SurfaceActive else NightColors.Surface)
            .border(1.dp, if (selected) NightColors.Lavender else NightColors.Border, shape)
            .clickable(onClick = onSelect)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        accent?.let { Box(Modifier.width(4.dp).fillMaxHeight().background(it)) }
        Column(
            Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            content()
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit, enabled = actionsEnabled, modifier = Modifier.size(40.dp)) {
                Icon(editIcon, contentDescription = "Bearbeiten", tint = NightColors.Lavender)
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete, enabled = actionsEnabled, modifier = Modifier.size(40.dp)) {
                Icon(deleteIcon, contentDescription = "Löschen", tint = NightColors.Coral)
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

@ThemePreviews
@Composable
private fun SelectableListCardPreview() = PreviewThemeWrapper {
    SelectableListCard(selected = true, onSelect = {}, onDelete = {}, actionsEnabled = true, canDelete = true) {
        Text("Eintrag", style = MaterialTheme.typography.titleMedium)
    }
}
