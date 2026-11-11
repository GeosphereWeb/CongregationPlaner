package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.card.FieldCard
import de.geosphere.congregationplaner.ui.field.AppSwitch

@Composable
internal fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** „Ändern“/„Wählen“ und, falls etwas gewählt ist, „Entfernen“ rechts in einer [FieldCard]. */
@Composable
internal fun FieldActions(changeLabel: String, enabled: Boolean, onChange: () -> Unit, onRemove: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.End) {
        SecondaryButton(changeLabel, onChange, enabled = enabled, outlined = false)
        if (onRemove != null) TextButton(onClick = onRemove) { Text("Entfernen") }
    }
}

/**
 * Speichern, Verwerfen und (mit [onDelete]) Löschen eines Eintrags in der Detailansicht: auf dem Handy volle Breite
 * untereinander, sonst nebeneinander rechts; „Löschen“ steht als roter Textbutton darunter.
 */
@Composable
@Suppress("LongParameterList")
internal fun EditorActions(
    saveText: String,
    saveEnabled: Boolean,
    discardEnabled: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteText: String = "Löschen",
    deleteEnabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < COMPACT_WIDTH) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(saveText, onSave, Modifier.fillMaxWidth(), saveEnabled, BUTTON_HEIGHT)
                    SecondaryButton(
                        "Verwerfen",
                        onDiscard,
                        Modifier.fillMaxWidth(),
                        discardEnabled,
                        height = BUTTON_HEIGHT,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    SecondaryButton("Verwerfen", onDiscard, enabled = discardEnabled, height = BUTTON_HEIGHT)
                    PrimaryButton(saveText, onSave, enabled = saveEnabled, height = BUTTON_HEIGHT)
                }
            }
        }
        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                enabled = deleteEnabled,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(contentColor = NightColors.Coral),
            ) { Text(deleteText) }
        }
    }
}

@ThemePreviews
@Composable
private fun EditorControlsPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchRow(label = "Informiert", checked = true) {}
        SwitchRow(label = "Vortrag entfällt", checked = false, enabled = false) {}
        FieldActions(changeLabel = "Ändern", enabled = true, onChange = {}, onRemove = {})
        EditorActions(saveText = "Speichern", saveEnabled = true, discardEnabled = true, onSave = {
        }, onDiscard = {}, onDelete = {})
    }
}
