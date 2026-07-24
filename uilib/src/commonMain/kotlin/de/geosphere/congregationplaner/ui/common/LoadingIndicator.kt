package de.geosphere.congregationplaner.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import de.geosphere.congregationplaner.theming.AppDimensions

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
