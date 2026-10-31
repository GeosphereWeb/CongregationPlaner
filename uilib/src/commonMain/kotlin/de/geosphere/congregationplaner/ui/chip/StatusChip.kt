@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

/** Bedeutung eines Chips: Gold = Highlight/Wartet, Mint = Erfolg, Himmelblau = Info, Koralle = Fehler. */
enum class StatusTone(val content: Color, val container: Color) {
    Gold(NightColors.Gold, NightColors.GoldContainer),
    Mint(NightColors.Mint, NightColors.MintContainer),
    Sky(NightColors.Sky, NightColors.SkyContainer),
    Coral(NightColors.Coral, NightColors.CoralContainer),
}

/** Kleine Statusmarke mit Punkt. */
@Composable
fun StatusChip(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(tone.container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(tone.content, CircleShape))
        Text(text, style = nightTextStyle(13.sp, FontWeight.Medium, tone.content))
    }
}

@ThemePreviews
@Composable
private fun StatusChipPreview() = NightPreviewWrapper {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip("Freigabe ausstehend", StatusTone.Gold)
        StatusChip("Geprobt", StatusTone.Mint)
        StatusChip("Entwurf", StatusTone.Sky)
        StatusChip("Fehler", StatusTone.Coral)
    }
}
