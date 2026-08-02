package de.geosphere.congregationplaner.ui.stuff

// ... (Imports bleiben gleich, Platz gespart)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.*
import de.geosphere.congregationplaner.theming.brushes.meshPainter

@Composable
fun StatusCardExample(modifier: Modifier = Modifier) {
    val meshPainter1 = remember { meshPainter }
    val btnShape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
    val density = LocalDensity.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = btnShape,
        modifier = modifier
            .padding(16.dp)
            .clip(btnShape)
            .drawBehind {
                // 1. Hintergrund zeichnen
                with(meshPainter1) { draw(size = size, alpha = 0.1f) }

                // 2. Kräftige Umrandung (Border)
                val strokeWidthPx = 3f
                val outline = btnShape.createOutline(size, layoutDirection, density)

                if (outline is Outline.Rounded) {
                    val halfStroke = strokeWidthPx / 2f

                    // KORREKTUR: Nutze drawRoundRect aus DrawScope (nicht canvas.drawRoundRect)
                    drawRoundRect(
                        brush = meshPainter1, // Dein Verlauf
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                        cornerRadius = CornerRadius(outline.roundRect.topLeftCornerRadius.x),
                        style = Stroke(width = strokeWidthPx) // 3px Rand
                    )
                }
            },
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
