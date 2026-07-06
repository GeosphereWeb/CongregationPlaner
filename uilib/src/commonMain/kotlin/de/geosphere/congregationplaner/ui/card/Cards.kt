package de.geosphere.congregationplaner.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.geosphere.congregationplaner.theming.AppColors
import de.geosphere.congregationplaner.theming.AppDimensions

/**
 * App Card Komponente
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
            )
            .padding(AppDimensions.PaddingMedium)
    ) {
        content()
    }
}

/**
 * Elevated Card Komponente mit Schatten-Effekt
 */
@Composable
fun ElevatedAppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .padding(AppDimensions.PaddingMedium)
        ) {
            content()
        }
    }
}
