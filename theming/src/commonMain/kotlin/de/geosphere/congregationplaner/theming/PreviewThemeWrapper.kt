package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush

@Composable
fun PreviewThemeWrapper(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Light Mode Variante
        AppTheme(useDarkTheme = false) {
            Box(modifier = Modifier.background(brush = Brush.backgroundBrush).padding(36.dp)) {
                content()
            }
        }

        // 2. Dark Mode Variante
        AppTheme(useDarkTheme = true) {
            Box(modifier = Modifier.background(brush = Brush.backgroundBrush).padding(36.dp)) {
                content()
            }
        }
    }
}
