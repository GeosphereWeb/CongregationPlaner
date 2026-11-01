package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush

@Composable
fun PreviewThemeWrapper(content: @Composable () -> Unit) {
    AppTheme {
        Box(modifier = Modifier.background(brush = Brush.backgroundBrush).padding(36.dp)) {
            content()
        }
    }
}
