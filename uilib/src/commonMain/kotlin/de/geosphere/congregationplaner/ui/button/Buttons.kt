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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.geosphere.congregationplaner.theming.AppColors
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.AppTheme

/**
 * Primary Button Komponente
 */
@Composable
fun PrimaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .background(
                color = if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
            ).clickable(enabled = enabled) { onClick() }
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
 * Secondary Button Komponente
 */
@Composable
fun SecondaryButtonComposable(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .background(
                color = if (enabled) AppColors.Secondary else AppColors.Secondary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
            ).clickable(enabled = enabled) { onClick() }
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
fun OutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeightMedium)
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
            ).clickable(enabled = enabled) { onClick() }
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
