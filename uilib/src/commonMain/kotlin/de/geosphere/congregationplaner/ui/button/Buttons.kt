@file: Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.theming.customColors

/**
 * Primary Button Komponente mit MeshGradient Hintergrund
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val btnShape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
    Button(
        onClick = onClick,
        shapes = ButtonShapes(
            shape = btnShape,
            pressedShape = btnShape,
        ),
        colors = ButtonColors(
            containerColor = Color.Unspecified,
            contentColor = MaterialTheme.customColors.btnContainerColor,
            disabledContainerColor = Color.Unspecified,
            disabledContentColor = MaterialTheme.customColors.btnContainerColor
        ),
        modifier = modifier.background(brush = Brush.backgroundBrush, shape = btnShape),
        enabled = enabled
    ) {
        Text(
            text = text
        )
    }
}

// /**
// * Secondary Button Komponente mit MeshGradient Hintergrund
// */
// @Composable
// fun SecondaryButtonComposable(
//    text: String,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
// ) {
//    val gradientPainter = remember {
//        MeshGradientPainter(1, 1) {
//            setVertex(0, 0, Offset(0f, 0f), Color(0xFF113A99))
//            setVertex(0, 1, Offset(1f, 0f), Color(0xFF126599))
//            setVertex(1, 0, Offset(0f, 1f), Color(0xFF131199))
//            setVertex(1, 1, Offset(1f, 1f), Color(0xFF401199))
//        }
//    }
//
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(AppDimensions.ButtonHeightMedium)
//            .clip(RoundedCornerShape(AppDimensions.CornerRadiusLarge))
//            .then(
//                if (enabled) {
//                    Modifier.paint(gradientPainter)
//                } else {
//                    Modifier.background(AppColors.Secondary.copy(alpha = 0.5f))
//                },
//            )
//            .clickable(enabled = enabled) { onClick() }
//            .padding(AppDimensions.PaddingSmall),
//        contentAlignment = Alignment.Center,
//    ) {
//        Text(
//            text = text,
//            color = AppColors.OnSecondary,
//            fontWeight = FontWeight.Bold,
//        )
//    }
// }
//
// /**
// * Outlined Button Komponente
// */
// @Composable
// fun OutlinedButton(
//    text: String,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
// ) {
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(AppDimensions.ButtonHeightMedium)
//            .background(
//                color = Color.Transparent,
//                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
//            )
//            .clickable(enabled = enabled) { onClick() }
//            .padding(AppDimensions.PaddingSmall),
//        contentAlignment = Alignment.Center,
//    ) {
//        Text(
//            text = text,
//            color = if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.5f),
//            fontWeight = FontWeight.Bold,
//        )
//    }
// }

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
        )
        PrimaryButtonComposable(
            text = "Disabled Button",
            onClick = {},
            enabled = false,
        )
    }
}
//
// @Preview
// @Composable
// private fun AllButtonsPreview() {
//    AppTheme {
//        Column(
//            modifier = Modifier.padding(AppDimensions.PaddingMedium),
//            verticalArrangement = Arrangement.spacedBy(AppDimensions.PaddingSmall),
//        ) {
//            PrimaryButtonComposable(text = "Primary Button", onClick = {})
//            SecondaryButtonComposable(text = "Secondary Button", onClick = {})
//            OutlinedButton(text = "Outlined Button", onClick = {})
//        }
//    }
// }
