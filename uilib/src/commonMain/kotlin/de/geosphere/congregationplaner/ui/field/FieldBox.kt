@file:Suppress("MagicNumber")

package de.geosphere.congregationplaner.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle

/**
 * Beschriftetes Eingabefeld. Der Rahmen wird bei Fokus lavendel und bei [isError] koralle.
 * [trailing] ist für kleine Aktionen am Feldende gedacht (z. B. Passwort anzeigen).
 */
@Composable
@Suppress("LongParameterList")
fun FieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    monospace: Boolean = false,
    isError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        isError -> NightColors.Coral
        focused -> NightColors.Lavender
        else -> NightColors.Border
    }
    val textStyle = nightTextStyle(if (monospace) 14.sp else 16.sp, FontWeight.Normal, monospace = monospace)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = nightTextStyle(13.sp, FontWeight.Medium, NightColors.OnBackgroundMuted))
        Row(
            modifier = Modifier.fillMaxWidth().background(NightColors.Surface, shape).border(1.dp, borderColor, shape)
                .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).height(50.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(NightColors.Lavender),
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                )
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = NightColors.Placeholder))
                }
            }
            trailing?.invoke()
        }
    }
}

@ThemePreviews
@Composable
private fun FieldBoxPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FieldBox("E-Mail", "", {}, placeholder = "name@beispiel.de", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        FieldBox("Passwort", "geheimeswort", {}, visualTransformation = PasswordVisualTransformation())
        FieldBox("Einladungslink", "congregationplaner://join/abc", {}, monospace = true, isError = true)
    }
}
