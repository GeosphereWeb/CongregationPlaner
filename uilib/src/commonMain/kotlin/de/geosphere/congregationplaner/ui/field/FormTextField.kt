package de.geosphere.congregationplaner.ui.field

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Einzeiliges Eingabefeld eines Formulars über die volle Breite. */
@Composable
fun FormTextField(label: String, value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
}

@ThemePreviews
@Composable
private fun FormTextFieldPreview() = PreviewThemeWrapper {
    Column {
        FormTextField("Familienname", "Muster") {}
        FormTextField("Vorname", "", enabled = false) {}
    }
}
