@file: Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.geosphere.congregationplaner.theming.AppColors
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.AppTheme

/**
 * Primary Button Komponente mit MeshGradient Hintergrund
 */
@Composable
fun PrimaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gradientPainter = remember {
        MeshGradientPainter(1, 1, hasBicubicColor = true) {
            setVertex(
                0,
                0,
                Offset(0f, 0f),
                Color.Red,
                rightControlPoint = Offset(0.5f, 0.5f),
            )
            setVertex(0, 1, Offset(1f, 0f), Color.Blue)
            setVertex(1, 0, Offset(0f, 1f), Color.Green)
            setVertex(1, 1, Offset(1f, 1f), Color.Yellow)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .clip(RoundedCornerShape(AppDimensions.CornerRadiusLarge))
            .then(
                if (enabled) {
                    Modifier.paint(gradientPainter)
                } else {
                    Modifier.background(AppColors.Primary.copy(alpha = 0.5f))
                },
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(AppDimensions.PaddingSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = AppColors.OnPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Secondary Button Komponente mit MeshGradient Hintergrund
 */
@Composable
fun SecondaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gradientPainter = remember {
        MeshGradientPainter(1, 1) {
            setVertex(0, 0, Offset(0f, 0f), Color(0xFF113A99))
            setVertex(0, 1, Offset(1f, 0f), Color(0xFF126599))
            setVertex(1, 0, Offset(0f, 1f), Color(0xFF131199))
            setVertex(1, 1, Offset(1f, 1f), Color(0xFF401199))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .clip(RoundedCornerShape(AppDimensions.CornerRadiusLarge))
            .then(
                if (enabled) {
                    Modifier.paint(gradientPainter)
                } else {
                    Modifier.background(AppColors.Secondary.copy(alpha = 0.5f))
                },
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(AppDimensions.PaddingSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = AppColors.OnSecondary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Outlined Button Komponente
 */
@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(AppDimensions.PaddingSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview
@Composable
private fun PrimaryButtonComposablePreview() {
    AppTheme {
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
}

@Preview
@Composable
private fun AllButtonsPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.PaddingSmall),
        ) {
            PrimaryButtonComposable(text = "Primary Button", onClick = {})
            SecondaryButtonComposable(text = "Secondary Button", onClick = {})
            OutlinedButton(text = "Outlined Button", onClick = {})
        }
    }
}
