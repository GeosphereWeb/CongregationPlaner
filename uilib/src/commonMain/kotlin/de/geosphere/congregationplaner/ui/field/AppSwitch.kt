package de.geosphere.congregationplaner.ui.field

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import de.geosphere.congregationplaner.theming.AppTheme

private const val DISABLED_CHECKED_TRACK_ALPHA = 0.5f
private const val DISABLED_CHECKED_THUMB_ALPHA = 0.7f

/**
 * Switch whose disabled-but-checked state stays recognisable as "on":
 * the default colours render it as a blank track without a visible thumb.
 * The enabled-but-off state gets a light outline and thumb so it reads as tappable.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            // Default unchecked colours are all the same dark tone and look inert in the night theme.
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedBorderColor = scheme.onSurfaceVariant,
            uncheckedTrackColor = scheme.surfaceContainer,
            disabledCheckedTrackColor = scheme.primary.copy(alpha = DISABLED_CHECKED_TRACK_ALPHA),
            disabledCheckedThumbColor = scheme.onPrimary.copy(alpha = DISABLED_CHECKED_THUMB_ALPHA),
            disabledCheckedBorderColor = Color.Transparent,
        ),
    )
}

@Preview(name = "AppSwitch states")
@Composable
private fun AppSwitchPreview() = AppTheme {
    Column {
        AppSwitch(checked = true, onCheckedChange = {})
        AppSwitch(checked = false, onCheckedChange = {})
        AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
        AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
    }
}
