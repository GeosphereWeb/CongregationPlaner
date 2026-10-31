@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.header

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

private val FADE_ALPHAS = listOf(0.3f, 0.55f)

/** Abstufung der bereits abgeschlossenen Zeilen: die jüngste ist am deutlichsten. */
internal fun fadeAlphas(count: Int): List<Float> = FADE_ALPHAS.takeLast(count)

/**
 * Kopfbereich im Teleprompter-Stil: App-Name und Schrittanzeige oben, darunter die abgeschlossenen Schritte
 * ausgeblendet und der aktuelle Schritt zwischen zwei Linien hervorgehoben.
 */
@Composable
fun TeleprompterHeader(
    stepLabel: String,
    current: String,
    modifier: Modifier = Modifier,
    previous: List<String> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth().background(NightColors.headerBrush)
            .drawBehind { drawRect(NightColors.headerGlowBrush(size)) }
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(NightColors.Lavender, CircleShape))
                Text("Congregation Planer", style = nightTextStyle(17.sp, FontWeight.Bold, letterSpacing = (-0.17).sp))
            }
            Text(
                text = stepLabel,
                style = nightTextStyle(
                    12.sp,
                    color = NightColors.OnBackgroundMuted,
                    monospace = true,
                    letterSpacing = 0.48.sp,
                ),
            )
        }
        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val lines = previous.takeLast(FADE_ALPHAS.size)
            val alphas = fadeAlphas(lines.size)
            lines.forEachIndexed { index, text ->
                Text(text, style = nightTextStyle(20.sp), modifier = Modifier.alpha(alphas[index]))
            }
            Column {
                HorizontalDivider(color = NightColors.Lavender.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayMarker()
                    Text(current, style = nightTextStyle(26.sp, FontWeight.Medium, letterSpacing = (-0.52).sp))
                }
                HorizontalDivider(color = NightColors.Lavender.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun PlayMarker() {
    Canvas(Modifier.size(14.dp)) {
        val unit = size.width / 24f
        val path = Path().apply {
            moveTo(7f * unit, 4f * unit)
            lineTo(20f * unit, 12f * unit)
            lineTo(7f * unit, 20f * unit)
            close()
        }
        drawPath(path, NightColors.Lavender)
    }
}

/**
 * Inhaltsfläche unter dem [TeleprompterHeader] mit abgerundeter Ecke oben links. Scrollt bei wenig Platz;
 * Kinder mit `weight(1f)` füllen die Resthöhe, sodass sich Aktionen am unteren Rand verankern lassen.
 */
@Composable
fun NightPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(NightColors.HeaderBottom)) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 40.dp))
                .background(NightColors.Background).verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = minHeight)
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
                content = content,
            )
        }
    }
}

/** Überschrift mit Untertitel am Anfang einer [NightPanel]. */
@Composable
fun PanelTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = nightTextStyle(28.sp, FontWeight.Bold, letterSpacing = (-0.56).sp))
        Text(subtitle, style = nightTextStyle(15.sp, color = NightColors.OnBackgroundMuted))
    }
}

/**
 * Gerüst der Anmelde-Bildschirme: [TeleprompterHeader] über einer [NightPanel], auf großen Fenstern
 * auf 480 dp Breite zentriert und über der Tastatur.
 */
@Composable
fun NightScreen(
    stepLabel: String,
    current: String,
    modifier: Modifier = Modifier,
    previous: List<String> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(NightColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxHeight().imePadding()) {
            TeleprompterHeader(stepLabel = stepLabel, current = current, previous = previous)
            NightPanel(modifier = Modifier.weight(1f), content = content)
        }
    }
}

@ThemePreviews
@Composable
private fun TeleprompterHeaderPreview() = NightPreviewWrapper {
    Column(Modifier.size(width = 390.dp, height = 420.dp)) {
        TeleprompterHeader(
            stepLabel = "SCHRITT 2 / 2",
            current = "Berlin-Mitte",
            previous = listOf("EMEA · Europa · Mitteleuropa", "Kreis 609a"),
        )
        NightPanel(Modifier.weight(1f)) {
            Text("Anmelden", style = nightTextStyle(28.sp, FontWeight.Bold))
        }
    }
}
