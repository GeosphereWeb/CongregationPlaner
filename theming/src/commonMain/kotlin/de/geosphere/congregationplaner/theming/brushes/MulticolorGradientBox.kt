package de.geosphere.congregationplaner.theming.brushes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.LocalCustomColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

val Brush.Companion.multicolorGradientBrush: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            LocalCustomColors.current.werner1,
            LocalCustomColors.current.werner2,
        ),
    )

// rows/columns are PATCH counts: a 4x4 point grid = 3x3 patches.

val meshPainter =
    MeshGradientPainter(
        rows = 3,
        columns = 3,
        hasBicubicColor = true, // Catmull-Rom: smooth, iOS-like blend
    ) {
        // setVertex(row, column, position, color) — position is normalized 0..1
        setVertex(0, 0, Offset(0.0000f, 0.0000f), Color(0xFF202D6F))
        setVertex(0, 1, Offset(0.4229f, 0.0000f), Color(0xFF202D6F))
        setVertex(0, 2, Offset(0.6257f, 0.0000f), Color(0xFF202D6F))
        setVertex(0, 3, Offset(1.0000f, 0.0000f), Color(0xFF202D6F))
        setVertex(1, 0, Offset(0.0000f, 0.3952f), Color(0xFF808FDB))
        setVertex(1, 1, Offset(0.3600f, 0.3355f), Color(0xFF808FDB))
        setVertex(1, 2, Offset(0.7029f, 0.2884f), Color(0xFF2B7397))
        setVertex(1, 3, Offset(1.0000f, 0.2236f), Color(0xFF2B7397))
        setVertex(2, 0, Offset(0.0000f, 0.8069f), Color(0xFF202D6F))
        setVertex(2, 1, Offset(0.3457f, 0.7548f), Color(0xFF4F2B97))
        setVertex(2, 2, Offset(0.7086f, 0.6976f), Color(0xFF4F2B97))
        setVertex(2, 3, Offset(1.0000f, 0.6963f), Color(0xFF202D6F))
        setVertex(3, 0, Offset(0.0000f, 1.0000f), Color(0xFF202D6F))
        setVertex(3, 1, Offset(0.2400f, 1.0000f), Color(0xFF202D6F))
        setVertex(3, 2, Offset(0.6543f, 1.0000f), Color(0xFF202D6F))
        setVertex(3, 3, Offset(1.0000f, 1.0000f), Color(0xFF202D6F))
    }

@Composable
private fun MulticolorGradientBox(modifier: Modifier = Modifier) {
    val meshPainter = remember {
        meshPainter
    }
    Box(modifier = modifier.size(600.dp, 250.dp).paint(meshPainter))
}

@ThemePreviews
@Composable
private fun MulticolorGradientBoxPreview() {
    PreviewThemeWrapper {
        MulticolorGradientBox()
    }
}
