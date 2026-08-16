@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.shapes.ButtonShape

/**
 * Primary Button Komponente mit MeshGradient Hintergrund
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun PrimaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = onClick,
        ).clip(shape = ButtonShape()).background(Color(0xFF2b3d97)),
    ) {
        Box(modifier = Modifier.padding(AppDimensions.PaddingSmall)) {
            Text(text = text, color = Color(0xFF97852b))
        }
    }
}

@ThemePreviews
@Composable
private fun PrimaryButtonComposablePreview() = PreviewThemeWrapper {
    Column(
        modifier = Modifier.padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.PaddingSmall),
    ) {
        PrimaryButtonComposable(
            text = "Primary Button",
            onClick = {},
            enabled = true,
        )
        PrimaryButtonComposable(
            text = "Disabled Button",
            onClick = {},
            enabled = false,
        )
    }
}
