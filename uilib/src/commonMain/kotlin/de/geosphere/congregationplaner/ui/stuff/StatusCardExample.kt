package de.geosphere.congregationplaner.ui.stuff

// ... (Imports bleiben gleich, Platz gespart)
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.scale
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

    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // GLOW-EFFEKT HINTER DER CARD
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (12 - index * 4).dp)
                    .clip(btnShape)
                    .drawBehind {
                        with(meshPainter1) { 
                            draw(size = size, alpha = 0.2f - (index * 0.05f)) 
                        }
                    }
                    .align(Alignment.Center)
            )
        }

        // DIE EIGENTLICHE CARD (Vordergrund)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = btnShape,
            modifier = Modifier
                .fillMaxWidth()
                .clip(btnShape)
                .drawBehind {
                    // Scharfer Mesh-Gradient
                    with(meshPainter1) { draw(size = size, alpha = 0.9f) }
                }
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.3f), shape = btnShape),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kongregations-Planer Info", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Wichtiger Hinweis.", color = MaterialTheme.customColors.brandCustom)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* Aktion */ }) { Text("Aktion") }
            }
        }
    }
}

@ThemePreviews
@Composable
fun StatusCardExamplePreview() = PreviewThemeWrapper {
    StatusCardExample()
}
