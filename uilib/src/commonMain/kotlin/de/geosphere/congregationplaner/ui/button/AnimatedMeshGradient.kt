package de.geosphere.congregationplaner.ui.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.tooling.preview.Preview
import de.geosphere.congregationplaner.theming.AppTheme

// Hinweis: MeshGradientPainter importieren (abhängig von deiner Compose/Material3-Version)

@Suppress("MagicNumber")
@Composable
fun AnimatedMeshGradient() {
    // 1. Unendliche Animation starten
    val infiniteTransition = rememberInfiniteTransition(label = "MeshGradientTransition")

    // 2. X-Koordinate des Mittelpunkts animieren (hin und her)
    val animatedX by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "CenterX",
    )

    // 3. Y-Koordinate des Mittelpunkts animieren (etwas andere Zeit für organischere Bewegung)
    val animatedY by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "CenterY",
    )

    // 4. MeshGradientPainter mit den animierten Werten erstellen
    val meshPainter = remember(animatedX, animatedY) {
        MeshGradientPainter(2, 2) {
            // Obere Reihe (Starr in den Ecken/Mitte)
            setVertex(0, 0, Offset(0f, 0f), Color(0xFF113A99))
            setVertex(0, 1, Offset(0.5f, 0f), Color(0xFF126599))
            setVertex(0, 2, Offset(1f, 0f), Color(0xFF131199))

            // Mittlere Reihe (Der Mittelpunkt bewegt sich!)
            setVertex(1, 0, Offset(0f, 0.5f), Color(0xFF1F44A0))
            setVertex(1, 1, Offset(animatedX, animatedY), Color(0xFFE040FB)) // Pinker Center-Punkt
            setVertex(1, 2, Offset(1f, 0.5f), Color(0xFF401199))

            // Untere Reihe (Starr in den Ecken/Mitte)
            setVertex(2, 0, Offset(0f, 1f), Color(0xFF151266))
            setVertex(2, 1, Offset(0.5f, 1f), Color(0xFF221166))
            setVertex(2, 2, Offset(1f, 1f), Color(0xFF0A1C50))
        }
    }

    // 5. Den Painter auf die Box anwenden
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(meshPainter),
    )
}

@Preview
@Composable
fun WernerPReview() = AppTheme {
    AnimatedMeshGradient()
}
