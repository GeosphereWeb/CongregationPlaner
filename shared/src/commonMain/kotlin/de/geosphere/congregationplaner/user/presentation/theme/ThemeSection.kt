package de.geosphere.congregationplaner.user.presentation.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.rememberScopedViewModel
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.user.di.ThemeGraph
import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import dev.zacsweers.metro.createGraph

/** "Darstellung" in the settings; the choice applies immediately and is stored on the device. */
@Composable
fun ThemeSection(modifier: Modifier = Modifier) {
    val viewModel = rememberScopedViewModel { createGraph<ThemeGraph>().themeViewModel }
    val preference by viewModel.preference.collectAsState()
    ThemeSettings(preference, viewModel::selectStyle, viewModel::selectMode, modifier)
}

@Composable
private fun ThemeSettings(
    preference: ThemePreference,
    onSelectStyle: (ThemeStyle) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Darstellung", style = MaterialTheme.typography.titleSmall)
        Choice(ThemeStyle.entries, preference.style, ThemeStyle::label, onSelectStyle)
        // The night-blue design is always dark.
        Choice(ThemeMode.entries, preference.mode, ThemeMode::label, onSelectMode, preference.style != ThemeStyle.NIGHT)
    }
}

@Composable
private fun <T> Choice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                enabled = enabled,
                label = { Text(label(option)) },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ThemeSettingsNightPreview() = PreviewThemeWrapper {
    ThemeSettings(ThemePreference(), {}, {})
}

@ThemePreviews
@Composable
private fun ThemeSettingsBlueDarkPreview() = PreviewThemeWrapper {
    ThemeSettings(ThemePreference(ThemeStyle.BLUE, ThemeMode.DARK), {}, {})
}
