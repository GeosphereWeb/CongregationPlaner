package de.geosphere.congregationplaner.ui.rating

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Füllung eines Sterns der Bewertung. */
enum class StarFill { Empty, Half, Full }

/**
 * Bewertung in gezeichneten Sternen im Goldton; [stars] sagt je Stern, wie weit er gefüllt ist (halb = linke Hälfte).
 * [onTap] bekommt die Nummer des Sterns (ab 1).
 */
@Composable
fun StarRating(stars: List<StarFill>, label: String = "Bewertung", onTap: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row {
            stars.forEachIndexed { index, fill ->
                IconButton(
                    onClick = { onTap(index + 1) },
                    modifier = Modifier.semantics { contentDescription = "Stern ${index + 1} von ${stars.size}" },
                ) {
                    Canvas(Modifier.size(28.dp)) { drawStar(fill, NightColors.Gold) }
                }
            }
        }
    }
}

private fun DrawScope.drawStar(fill: StarFill, color: Color) {
    val star = starPath(center, min(size.width, size.height) / 2)
    when (fill) {
        StarFill.Full -> drawPath(star, color)
        StarFill.Half -> clipRect(right = size.width / 2) { drawPath(star, color) }
        StarFill.Empty -> Unit
    }
    drawPath(star, color, style = Stroke(width = 1.5.dp.toPx()))
}

/** Fünfzackiger Stern um [center] mit Spitze oben; die inneren Ecken liegen auf 40 % des Radius. */
private fun starPath(center: Offset, radius: Float): Path = Path().apply {
    repeat(10) { i ->
        val r = if (i % 2 == 0) radius else radius * 0.4f
        val angle = -PI / 2 + i * PI / 5
        val point = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
        if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

@ThemePreviews
@Composable
private fun StarRatingPreview() = PreviewThemeWrapper {
    Column {
        StarRating(listOf(StarFill.Full, StarFill.Full, StarFill.Full, StarFill.Half, StarFill.Empty)) {}
        StarRating(List(5) { StarFill.Empty }, label = "Ohne Bewertung") {}
    }
}
