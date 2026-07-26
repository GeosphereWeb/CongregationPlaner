package de.geosphere.congregationplaner.ui.stuff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.brushes.meshPainter
import de.geosphere.congregationplaner.theming.customColors

@Composable
fun StatusCardExample(modifier: Modifier = Modifier) {
    val meshPainter1 = remember { meshPainter }
    val btnShape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = btnShape,
        modifier = modifier
            .padding(16.dp)
            .clip(btnShape) // Verhindert, dass der Hintergrund über die runden Ecken der Card steht
            .drawBehind {
                // drawBehind entkoppelt das Zeichnen komplett vom Compose-Layout-Sizing.
                // Der Gradient nimmt exakt die finale Pixelgröße der Card an.
                with(meshPainter1) {
                    draw(size)
                }
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kongregations-Planer Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wichtiger Hinweis mit eigener Markenfarbe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.customColors.brandCustom,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Aktion */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.customColors.successContainer,
                    contentColor = MaterialTheme.customColors.success,
                ),
            ) {
                Text(text = "Aktion erfolgreich bestätigen")
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StatusCardExamplePreview() = PreviewThemeWrapper {
    StatusCardExample(modifier = Modifier.fillMaxWidth())
}
