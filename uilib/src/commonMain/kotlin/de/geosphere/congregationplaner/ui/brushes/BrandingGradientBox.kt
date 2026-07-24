package de.geosphere.congregationplaner.ui.brushes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.LocalCustomColors

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
    Box(modifier.size(300.dp).background(brush = Brush.backgroundBrush))
}

@Preview // Nutzt die plattformübergreifende Preview
@Composable
private fun BrandingBrushesLightPreview() {
    AppTheme(useDarkTheme = false) {
        // Erzwinge Light Mode in deinem Theme
        BrandingGradientBox()
    }
}

@Preview
@Composable
private fun BrandingBrushesDarkPreview() {
    AppTheme(useDarkTheme = true) {
        // Erzwinge Dark Mode in deinem Theme
        BrandingGradientBox()
    }
}
