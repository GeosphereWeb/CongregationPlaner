@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonElevation
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.customColors
import de.geosphere.congregationplaner.ui.shapes.ButtonShape

/**
 * Primary Button Komponente mit MeshGradient Hintergrund
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
@Suppress("LongParameterList")
fun PrimaryButtonComposable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (RowScope.() -> Unit),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonShape(),
        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.customColors.btnContainerBorderColor),
        colors = ButtonColors(
            containerColor = MaterialTheme.customColors.btnContainerColor,
            contentColor = MaterialTheme.customColors.btnContentColor,
            disabledContainerColor = MaterialTheme.customColors.btnContainerColorDisabled,
            disabledContentColor = MaterialTheme.customColors.btnContentColorDisabled,
        ),
        elevation = elevation,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@ThemePreviews
@Composable
private fun PrimaryButtonComposablePreview() = PreviewThemeWrapper {
    Column(
        modifier = Modifier.padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.PaddingSmall),
    ) {
        PrimaryButtonComposable(
            onClick = {},
            enabled = true,
        ) {
            Text("enabled Button")
        }
        PrimaryButtonComposable(
            onClick = {},
            enabled = false,
        ) {
            Text("Disabled Button")
        }
    }
}
