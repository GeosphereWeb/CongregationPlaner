package de.geosphere.congregationplaner.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * App Card Komponente
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
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
    backgroundColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusLarge),
        colors = CardDefaults.elevatedCardColors()
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

@ThemePreviews
@Composable
private fun AppCardPreview() = PreviewThemeWrapper {
    Column(
        modifier = Modifier.padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.PaddingSmall),
    ) {
       AppCard {
            Text("This is a simple card")
        }
        ElevatedAppCard {
            Text("This is an elevated card with shadow")
        }
    }
}
