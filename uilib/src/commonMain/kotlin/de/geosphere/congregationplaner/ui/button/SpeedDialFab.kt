package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Ein beschrifteter Knopf im Menü des [SpeedDialFab]. */
data class SpeedDialItem(val label: String, val onClick: () -> Unit)

/**
 * FAB in Lavendel, der darüber beschriftete Knöpfe ([items], von oben nach unten) aufklappt, damit klar ist, was er
 * tut. Das „+“ dreht sich dabei zum „×“; ein zweiter Tipp oder die Wahl eines Knopfs schließt wieder. Solange
 * [enabled] `false` ist (z. B. beim Speichern), bleibt das Menü zu.
 */
@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val open = expanded && enabled
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (open) {
            items.forEach { item ->
                // Im kräftigen Lila der übrigen Knöpfe, damit sie als Knöpfe erkennbar sind.
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                    containerColor = NightColors.Primary,
                    contentColor = NightColors.OnBackground,
                    icon = { Icon(addIcon, contentDescription = null) },
                    text = { Text(item.label) },
                )
            }
        }
        // Helles Lavendel statt des dunklen primaryContainer, damit der FAB vom Hintergrund absticht.
        FloatingActionButton(
            onClick = { if (enabled) expanded = !expanded },
            containerColor = NightColors.Lavender,
            contentColor = NightColors.HeaderTop,
        ) {
            Icon(
                addIcon,
                contentDescription = if (open) "Schließen" else contentDescription,
                modifier = Modifier.rotate(if (open) OPEN_ROTATION else 0f),
            )
        }
    }
}

private const val OPEN_ROTATION = 45f

// Das Plus aus den Material Symbols (Apache 2.0).
private val addIcon = ImageVector.Builder(
    name = "add",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
    fill = SolidColor(Color.Black),
).build()

@ThemePreviews
@Composable
private fun SpeedDialFabPreview() = NightPreviewWrapper {
    SpeedDialFab(
        items = listOf(SpeedDialItem("Alle Termine 2026 anlegen") {}, SpeedDialItem("Termin anlegen") {}),
        contentDescription = "Termine anlegen",
    )
}
