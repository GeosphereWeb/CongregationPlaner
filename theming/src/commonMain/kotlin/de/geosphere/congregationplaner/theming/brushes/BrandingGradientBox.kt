package de.geosphere.congregationplaner.theming.brushes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.LocalCustomColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

val Brush.Companion.backgroundBrush: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            LocalCustomColors.current.werner1,
            LocalCustomColors.current.werner2,
        ),
    )

@Composable
private fun BrandingGradientBox(modifier: Modifier = Modifier) {
    Box(modifier.size(600.dp, 250.dp).background(brush = Brush.backgroundBrush))
}

@ThemePreviews
@Composable
private fun BrandingGradientBoxPreview() {
    PreviewThemeWrapper {
        BrandingGradientBox()
    }
}
