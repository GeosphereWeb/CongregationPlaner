@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.org

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

/** Zustand einer Ebene in der Versammlungsauswahl. */
enum class OrgLevelStatus {
    /** Auswahl getroffen: Häkchen. */
    Done,

    /** Als Nächstes zu wählen: lavendel hervorgehoben. */
    Active,

    /** Optionen werden geladen. */
    Loading,
}

/** Eine Ebene (Region, Subregion, Zweig, Kreis, Versammlung) mit Wert, Häkchen oder Pfeil. */
@Composable
@Suppress("LongParameterList")
fun OrgLevelRow(
    label: String,
    value: String?,
    status: OrgLevelStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "bitte wählen",
    enabled: Boolean = true,
) {
    val active = status == OrgLevelStatus.Active
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier.fillMaxWidth().height(46.dp).alpha(if (enabled) 1f else 0.5f).clip(shape)
            .background(if (active) NightColors.SurfaceActive else NightColors.Surface)
            .border(1.dp, if (active) NightColors.Lavender else NightColors.Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                modifier = Modifier.width(100.dp),
                style = nightTextStyle(
                    11.sp,
                    color = if (active) NightColors.Lavender else NightColors.OnBackgroundMuted,
                    monospace = true,
                    letterSpacing = 0.88.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = value ?: placeholder,
                style = nightTextStyle(
                    15.sp,
                    FontWeight.Medium,
                    if (value == null) NightColors.OnBackgroundMuted else NightColors.OnBackground,
                ),
                maxLines = 1,
            )
        }
        when (status) {
            OrgLevelStatus.Done -> Glyph(NightColors.Mint, check = true)
            OrgLevelStatus.Active -> Glyph(NightColors.Lavender, check = false)
            OrgLevelStatus.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = NightColors.Lavender,
            )
        }
    }
}

/** Häkchen oder Pfeil nach unten, auf dem 24er-Raster des Designs gezeichnet. */
@Composable
private fun Glyph(color: Color, check: Boolean) {
    Canvas(Modifier.size(18.dp)) {
        val unit = size.width / 24f
        val path = Path().apply {
            if (check) {
                moveTo(5f * unit, 12.5f * unit)
                lineTo(9.5f * unit, 17f * unit)
                lineTo(19f * unit, 7.5f * unit)
            } else {
                moveTo(6f * unit, 9f * unit)
                lineTo(12f * unit, 15f * unit)
                lineTo(18f * unit, 9f * unit)
            }
        }
        drawPath(path, color, style = Stroke(2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@ThemePreviews
@Composable
private fun OrgLevelRowPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(4.dp)) {
        OrgLevelRow("Region", "EMEA", OrgLevelStatus.Done, {})
        OrgLevelRow("Subregion", "Europa", OrgLevelStatus.Done, {})
        OrgLevelRow("Versammlung", null, OrgLevelStatus.Active, {})
        OrgLevelRow("Kreis", null, OrgLevelStatus.Loading, {})
        OrgLevelRow("Zweig", null, OrgLevelStatus.Active, {}, enabled = false)
    }
}
