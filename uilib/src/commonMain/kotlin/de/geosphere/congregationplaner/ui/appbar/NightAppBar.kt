package de.geosphere.congregationplaner.ui.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

private val TOUCH_TARGET = 44.dp

/**
 * Obere Leiste der Unterbereiche: Zurück-Pfeil, Titel und optional ein Untertitel (z. B. der Versammlungsname).
 * [backDescription] ist die Beschreibung für Screenreader, z. B. „Zurück zu Versammlung“.
 */
@Composable
fun NightTopBar(
    title: String,
    backIcon: ImageVector,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NightColors.HeaderTop, NightColors.HeaderMiddle)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(TOUCH_TARGET)) {
            Icon(backIcon, contentDescription = backDescription, tint = NightColors.OnBackground)
        }
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = NightColors.OnBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NightColors.OnBackgroundMuted)
            }
        }
    }
}

/** Tabs unter der [NightTopBar]: transparent, Indikator in Lavendel und eine feine Linie darunter. */
@Composable
fun NightTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = NightColors.OnBackground,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                height = 3.dp,
                color = NightColors.Lavender,
            )
        },
        divider = { HorizontalDivider(color = NightColors.Border) },
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                selectedContentColor = NightColors.OnBackground,
                unselectedContentColor = NightColors.OnBackgroundMuted,
                text = {
                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun NightTopBarWithTabsPreview() = NightPreviewWrapper {
    Column {
        NightTopBar(
            title = "Vortragsplanung",
            backIcon = ImageVector.Builder("back", 24.dp, 24.dp, 24f, 24f).build(),
            backDescription = "Zurück zu Versammlung",
            onBack = {},
            subtitle = "Berlin-Mitte",
        )
        NightTabRow(listOf("Wochenende", "Externe Redner", "Historie"), selectedIndex = 0, onSelect = {})
    }
}
