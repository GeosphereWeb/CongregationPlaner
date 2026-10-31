@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.field

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

// Material Symbols „visibility“ (outlined, 24 dp)
private const val VISIBILITY_PATH =
    "M15.19,14.69q1.31-1.31 1.31-3.19T15.19,8.31T12,7T8.81,8.31T7.5,11.5t1.31,3.19T12,16t3.19-1.31Z" +
        "m-5.1-1.28Q9.3,12.63 9.3,11.5t0.79-1.91T12,8.8t1.91,0.79q0.79,0.79 0.79,1.91t-0.79,1.91T12,14.2" +
        "t-1.91-0.79ZM5.35,16.96Q2.35,14.93 1,11.5Q2.35,8.07 5.35,6.04T12,4t6.65,2.04T23,11.5" +
        "q-1.35,3.42-4.35,5.46T12,19T5.35,16.96ZM12,11.5Zm5.19,4.01q2.36-1.49 3.61-4.01" +
        "q-1.25-2.53-3.61-4.01T12,6Q9.18,6 6.81,7.49T3.2,11.5q1.25,2.52 3.61,4.01T12,17t5.19-1.49Z"

private val VisibilityIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Visibility",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(VISIBILITY_PATH).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** Auge am Ende eines Passwortfelds ([FieldBox.trailing]); lavendel, solange das Passwort sichtbar ist. */
@Composable
fun PasswordVisibilityButton(visible: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier.size(44.dp)) {
        Icon(
            imageVector = VisibilityIcon,
            contentDescription = if (visible) "Passwort verbergen" else "Passwort anzeigen",
            tint = if (visible) NightColors.Lavender else NightColors.OnBackgroundMuted,
        )
    }
}

@ThemePreviews
@Composable
private fun PasswordVisibilityButtonPreview() = NightPreviewWrapper {
    Row {
        PasswordVisibilityButton(visible = false, onToggle = {})
        PasswordVisibilityButton(visible = true, onToggle = {})
    }
}
