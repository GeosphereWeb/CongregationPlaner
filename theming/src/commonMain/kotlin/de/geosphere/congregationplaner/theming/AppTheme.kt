package de.geosphere.congregationplaner.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Die Haupt-Theme-Composable der Anwendung (nur dunkel, siehe ADR-0014). */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = nightColorScheme(),
        typography = nightTypography(),
        shapes = nightShapes(),
        content = content,
    )
}
