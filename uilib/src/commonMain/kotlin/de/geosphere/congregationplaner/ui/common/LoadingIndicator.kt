package de.geosphere.congregationplaner.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Loading Indicator Komponente
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(AppDimensions.IconSizeLarge),
            color = Color.Unspecified,
            strokeWidth = AppDimensions.PaddingXSmall
        )
    }
}

/**
 * Fullscreen Loading Overlay
 */
@Composable
fun FullscreenLoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    }
}

@ThemePreviews
@Composable
private fun LoadingIndicatorPreview() = PreviewThemeWrapper {
    LoadingIndicator()
}

@ThemePreviews
@Composable
private fun FullscreenLoadingOverlayPreview() = PreviewThemeWrapper {
    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Text("Daten werden geladen")
        FullscreenLoadingOverlay(isLoading = true, modifier = Modifier.size(240.dp))
    }
}
